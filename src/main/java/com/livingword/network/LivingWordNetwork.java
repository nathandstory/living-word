package com.livingword.network;

import com.livingword.LivingWord;
import com.livingword.discs.ScriptureDisc;
import com.livingword.discs.ScriptureDiscSelection;
import com.livingword.network.payload.ConfigureScriptureDiscPayload;
import com.livingword.network.payload.JoinListeningSessionPayload;
import com.livingword.network.payload.LeaveListeningSessionPayload;
import com.livingword.network.payload.ListeningSessionSyncPayload;
import com.livingword.network.payload.OpenBiblePayload;
import com.livingword.network.payload.PlaybackControlPayload;
import com.livingword.network.payload.TimestampCorrectionPayload;
import com.livingword.sync.ListeningSession;
import com.livingword.sync.ListeningSessionManager;
import net.minecraft.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.UUID;

public final class LivingWordNetwork {
    private static final ListeningSessionManager LISTENING_SESSIONS = new ListeningSessionManager();

    private LivingWordNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(LivingWordNetwork::registerPayloadHandlers);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        registrar.playToClient(OpenBiblePayload.TYPE, OpenBiblePayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> invokeClient("openBibleScreen"));
        });
        registrar.playToClient(ListeningSessionSyncPayload.TYPE, ListeningSessionSyncPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> invokeClient("handleSessionSync", ListeningSessionSyncPayload.class, payload));
        });
        registrar.playToServer(JoinListeningSessionPayload.TYPE, JoinListeningSessionPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    LISTENING_SESSIONS.join(payload.sessionId(), player.getUUID());
                    syncSessionToParticipants(payload.sessionId(), Util.getMillis());
                }
            });
        });
        registrar.playToServer(LeaveListeningSessionPayload.TYPE, LeaveListeningSessionPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    LISTENING_SESSIONS.leave(payload.sessionId(), player.getUUID());
                    syncSessionToParticipants(payload.sessionId(), Util.getMillis());
                }
            });
        });
        registrar.playBidirectional(PlaybackControlPayload.TYPE, PlaybackControlPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                Player player = context.player();
                if (player instanceof ServerPlayer) {
                    LISTENING_SESSIONS.control(payload.sessionId(), payload.state(), payload.positionMillis(), payload.serverTimeMillis());
                    syncSessionToParticipants(payload.sessionId(), Util.getMillis());
                } else {
                    invokeClient("handlePlaybackControl", PlaybackControlPayload.class, payload);
                }
            });
        });
        registrar.playToClient(TimestampCorrectionPayload.TYPE, TimestampCorrectionPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> invokeClient("handleTimestampCorrection", TimestampCorrectionPayload.class, payload));
        });
        registrar.playToServer(ConfigureScriptureDiscPayload.TYPE, ConfigureScriptureDiscPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    ItemStack stack = player.getItemInHand(payload.hand());
                    if (stack.getItem() instanceof ScriptureDisc) {
                        ScriptureDiscSelection.write(stack, new ScriptureDiscSelection(
                            payload.translationId(),
                            payload.bookId(),
                            payload.chapter(),
                            payload.audioManifestId(),
                            payload.playbackMode()
                        ));
                        player.displayClientMessage(Component.translatable("message.livingword.disc.configured"), true);
                    }
                }
            });
        });
    }

    public static ListeningSession startNearbyListeningSession(ServerPlayer source, String translationId, String bookId, int chapter, double radius) {
        return startNearbyListeningSession(source, translationId, bookId, chapter, radius, 0L);
    }

    public static ListeningSession startNearbyListeningSession(ServerPlayer source, String translationId, String bookId, int chapter, double radius, long startPositionMillis) {
        return startNearbyListeningSession(source, translationId, bookId, chapter, "default", radius, startPositionMillis);
    }

    public static ListeningSession startNearbyListeningSession(ServerPlayer source, String translationId, String bookId, int chapter, String audioManifestId, double radius) {
        return startNearbyListeningSession(source, translationId, bookId, chapter, audioManifestId, radius, 0L);
    }

    public static ListeningSession startNearbyListeningSession(ServerPlayer source, String translationId, String bookId, int chapter, String audioManifestId, double radius, long startPositionMillis) {
        long now = Util.getMillis();
        ListeningSession created = LISTENING_SESSIONS.create(translationId, bookId, chapter, audioManifestId, now);
        if (startPositionMillis > 0L) {
            LISTENING_SESSIONS.seek(created.id(), startPositionMillis, now);
            LISTENING_SESSIONS.play(created.id(), now);
        }
        double radiusSquared = radius * radius;
        for (ServerPlayer candidate : source.serverLevel().players()) {
            if (candidate.distanceToSqr(source) <= radiusSquared) {
                LISTENING_SESSIONS.join(created.id(), candidate.getUUID());
            }
        }
        syncSessionToParticipants(created.id(), now);
        return LISTENING_SESSIONS.get(created.id()).orElse(created);
    }

    public static java.util.Optional<ListeningSession> stopListeningSession(UUID sessionId) {
        long now = Util.getMillis();
        java.util.Optional<ListeningSession> existing = LISTENING_SESSIONS.get(sessionId);
        if (existing.isEmpty()) {
            return java.util.Optional.empty();
        }
        LISTENING_SESSIONS.control(sessionId, com.livingword.sync.PlaybackState.STOPPED, existing.orElseThrow().positionMillisAt(now), now);
        syncSessionToParticipants(sessionId, now);
        return LISTENING_SESSIONS.remove(sessionId);
    }

    private static void syncSessionToParticipants(UUID sessionId, long serverMillis) {
        LISTENING_SESSIONS.get(sessionId).ifPresent(session -> {
            ListeningSessionSyncPayload payload = ListeningSessionSyncPayload.fromSession(session, serverMillis);
            for (UUID participantId : session.participants()) {
                ServerPlayer participant = participantById(participantId);
                if (participant != null) {
                    PacketDistributor.sendToPlayer(participant, payload);
                }
            }
        });
    }

    private static ServerPlayer participantById(UUID participantId) {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getPlayerList().getPlayer(participantId);
    }

    private static void invokeClient(String methodName) {
        try {
            Class<?> client = Class.forName("com.livingword.client.LivingWordClient");
            client.getMethod(methodName).invoke(null);
        } catch (ReflectiveOperationException exception) {
            LivingWord.LOGGER.warn("Unable to invoke client Living Word handler {}", methodName, exception);
        }
    }

    private static <T> void invokeClient(String methodName, Class<T> payloadType, T payload) {
        try {
            Class<?> client = Class.forName("com.livingword.client.LivingWordClient");
            client.getMethod(methodName, payloadType).invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            LivingWord.LOGGER.warn("Unable to invoke client Living Word handler {}", methodName, exception);
        }
    }
}
