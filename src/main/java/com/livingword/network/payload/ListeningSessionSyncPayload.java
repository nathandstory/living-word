package com.livingword.network.payload;

import com.livingword.LivingWord;
import com.livingword.sync.ListeningSession;
import com.livingword.sync.PlaybackState;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.UUID;

public record ListeningSessionSyncPayload(
    UUID sessionId,
    String translationId,
    String bookId,
    int chapter,
    String audioManifestId,
    PlaybackState state,
    long positionMillis,
    long serverTimeMillis,
    int participantCount
) implements CustomPacketPayload {
    public static final Type<ListeningSessionSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, "listening_session_sync"));
    private static final StreamCodec<RegistryFriendlyByteBuf, PlaybackState> PLAYBACK_STATE_CODEC = NeoForgeStreamCodecs.enumCodec(PlaybackState.class);
    public static final StreamCodec<RegistryFriendlyByteBuf, ListeningSessionSyncPayload> STREAM_CODEC = StreamCodec.of(
        ListeningSessionSyncPayload::encode,
        ListeningSessionSyncPayload::decode
    );

    public ListeningSessionSyncPayload {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId is required");
        }
        if (translationId == null || translationId.isBlank()) {
            throw new IllegalArgumentException("translationId is required");
        }
        if (bookId == null || bookId.isBlank()) {
            throw new IllegalArgumentException("bookId is required");
        }
        if (chapter < 1) {
            throw new IllegalArgumentException("chapter must be positive");
        }
        audioManifestId = audioManifestId == null || audioManifestId.isBlank() ? "default" : audioManifestId;
        if (state == null) {
            throw new IllegalArgumentException("state is required");
        }
        positionMillis = Math.max(0L, positionMillis);
        participantCount = Math.max(0, participantCount);
    }

    public ListeningSessionSyncPayload(
        UUID sessionId,
        String translationId,
        String bookId,
        int chapter,
        PlaybackState state,
        long positionMillis,
        long serverTimeMillis,
        int participantCount
    ) {
        this(sessionId, translationId, bookId, chapter, "default", state, positionMillis, serverTimeMillis, participantCount);
    }

    public static ListeningSessionSyncPayload fromSession(ListeningSession session, long serverMillis) {
        return new ListeningSessionSyncPayload(
            session.id(),
            session.translationId(),
            session.bookId(),
            session.chapter(),
            session.audioManifestId(),
            session.state(),
            session.positionMillisAt(serverMillis),
            serverMillis,
            session.participants().size()
        );
    }

    @Override
    public Type<ListeningSessionSyncPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ListeningSessionSyncPayload payload) {
        UUIDUtil.STREAM_CODEC.encode(buffer, payload.sessionId);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.translationId);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.bookId);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.chapter);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.audioManifestId);
        PLAYBACK_STATE_CODEC.encode(buffer, payload.state);
        ByteBufCodecs.VAR_LONG.encode(buffer, payload.positionMillis);
        ByteBufCodecs.VAR_LONG.encode(buffer, payload.serverTimeMillis);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.participantCount);
    }

    private static ListeningSessionSyncPayload decode(RegistryFriendlyByteBuf buffer) {
        return new ListeningSessionSyncPayload(
            UUIDUtil.STREAM_CODEC.decode(buffer),
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            ByteBufCodecs.VAR_INT.decode(buffer),
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            PLAYBACK_STATE_CODEC.decode(buffer),
            ByteBufCodecs.VAR_LONG.decode(buffer),
            ByteBufCodecs.VAR_LONG.decode(buffer),
            ByteBufCodecs.VAR_INT.decode(buffer)
        );
    }
}
