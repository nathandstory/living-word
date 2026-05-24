package com.livingword.network.payload;

import com.livingword.LivingWord;
import com.livingword.discs.ScriptureDiscPlaybackMode;
import com.livingword.discs.ScriptureDiscSelection;
import com.livingword.lectern.LecternStationAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ConfigureLecternStationPayload(
    BlockPos sourcePos,
    String translationId,
    String bookId,
    int chapter,
    String audioManifestId,
    ScriptureDiscPlaybackMode playbackMode,
    LecternStationAction action
) implements CustomPacketPayload {
    public static final Type<ConfigureLecternStationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, "configure_lectern_station"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureLecternStationPayload> STREAM_CODEC = StreamCodec.of(
        ConfigureLecternStationPayload::encode,
        ConfigureLecternStationPayload::decode
    );

    public ConfigureLecternStationPayload {
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
        action = action == null ? LecternStationAction.SAVE : action;
    }

    public ConfigureLecternStationPayload(BlockPos sourcePos, ScriptureDiscSelection selection, LecternStationAction action) {
        this(
            sourcePos,
            selection.translationId(),
            selection.bookId(),
            selection.chapter(),
            selection.audioManifestId(),
            selection.playbackMode(),
            action
        );
    }

    public ScriptureDiscSelection selection() {
        return new ScriptureDiscSelection(translationId, bookId, chapter, audioManifestId, playbackMode);
    }

    @Override
    public Type<ConfigureLecternStationPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ConfigureLecternStationPayload payload) {
        buffer.writeBlockPos(payload.sourcePos);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.translationId);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.bookId);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.chapter);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.audioManifestId);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.playbackMode.name());
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.action.name());
    }

    private static ConfigureLecternStationPayload decode(RegistryFriendlyByteBuf buffer) {
        return new ConfigureLecternStationPayload(
            buffer.readBlockPos(),
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            ByteBufCodecs.VAR_INT.decode(buffer),
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            ScriptureDiscPlaybackMode.fromId(ByteBufCodecs.STRING_UTF8.decode(buffer)),
            LecternStationAction.fromId(ByteBufCodecs.STRING_UTF8.decode(buffer))
        );
    }
}
