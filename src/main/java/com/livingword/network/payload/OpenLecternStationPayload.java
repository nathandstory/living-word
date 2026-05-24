package com.livingword.network.payload;

import com.livingword.LivingWord;
import com.livingword.discs.ScriptureDiscPlaybackMode;
import com.livingword.discs.ScriptureDiscSelection;
import com.livingword.lectern.LecternListeningStation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenLecternStationPayload(
    BlockPos sourcePos,
    String translationId,
    String bookId,
    int chapter,
    String audioManifestId,
    ScriptureDiscPlaybackMode playbackMode,
    long resumePositionMillis,
    boolean playing,
    boolean displayEnabled
) implements CustomPacketPayload {
    public static final Type<OpenLecternStationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, "open_lectern_station"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLecternStationPayload> STREAM_CODEC = StreamCodec.of(
        OpenLecternStationPayload::encode,
        OpenLecternStationPayload::decode
    );

    public OpenLecternStationPayload {
        if (sourcePos == null) {
            throw new IllegalArgumentException("sourcePos is required");
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
        playbackMode = playbackMode == null ? ScriptureDiscPlaybackMode.SINGLE_CHAPTER : playbackMode;
        resumePositionMillis = Math.max(0L, resumePositionMillis);
    }

    public static OpenLecternStationPayload fromStation(LecternListeningStation station, boolean playing) {
        return new OpenLecternStationPayload(
            station.sourcePos(),
            station.selection().translationId(),
            station.selection().bookId(),
            station.selection().chapter(),
            station.selection().audioManifestId(),
            station.selection().playbackMode(),
            station.resumePositionMillis(),
            playing,
            station.displayEnabled()
        );
    }

    public ScriptureDiscSelection selection() {
        return new ScriptureDiscSelection(translationId, bookId, chapter, audioManifestId, playbackMode);
    }

    @Override
    public Type<OpenLecternStationPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, OpenLecternStationPayload payload) {
        buffer.writeBlockPos(payload.sourcePos);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.translationId);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.bookId);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.chapter);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.audioManifestId);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.playbackMode.name());
        ByteBufCodecs.VAR_LONG.encode(buffer, payload.resumePositionMillis);
        buffer.writeBoolean(payload.playing);
        buffer.writeBoolean(payload.displayEnabled);
    }

    private static OpenLecternStationPayload decode(RegistryFriendlyByteBuf buffer) {
        return new OpenLecternStationPayload(
            buffer.readBlockPos(),
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            ByteBufCodecs.VAR_INT.decode(buffer),
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            ScriptureDiscPlaybackMode.fromId(ByteBufCodecs.STRING_UTF8.decode(buffer)),
            ByteBufCodecs.VAR_LONG.decode(buffer),
            buffer.readBoolean(),
            buffer.readBoolean()
        );
    }
}
