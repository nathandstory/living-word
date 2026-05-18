package com.livingword.network;

import com.livingword.LivingWord;
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
    }

    public static ListeningSession startNearbyListeningSession(ServerPlayer source, String translationId, String bookId, int chapter, double radius) {
        long now = Util.getMillis();
        ListeningSession created = LISTENING_SESSIONS.create(translationId, bookId, chapter, now);
        double radiusSquared = radius * radius;
        for (ServerPlayer candidate : source.serverLevel().players()) {
            if (candidate.distanceToSqr(source) <= radiusSquared) {
                LISTENING_SESSIONS.join(created.id(), candidate.getUUID());
            }
        }
        syncSessionToParticipants(created.id(), now);
        return LISTENING_SESSIONS.get(created.id()).orElse(created);
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
