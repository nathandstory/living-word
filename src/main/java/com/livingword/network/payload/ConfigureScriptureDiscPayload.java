package com.livingword.network.payload;

import com.livingword.LivingWord;
import com.livingword.discs.ScriptureDiscPlaybackMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

public record ConfigureScriptureDiscPayload(
    InteractionHand hand,
    String translationId,
    String bookId,
    int chapter,
    String audioManifestId,
    ScriptureDiscPlaybackMode playbackMode
) implements CustomPacketPayload {
    public static final Type<ConfigureScriptureDiscPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, "configure_scripture_disc"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureScriptureDiscPayload> STREAM_CODEC = StreamCodec.of(
        ConfigureScriptureDiscPayload::encode,
        ConfigureScriptureDiscPayload::decode
    );

    public ConfigureScriptureDiscPayload {
        if (hand == null) {
            throw new IllegalArgumentException("hand is required");
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
        if (playbackMode == null) {
            playbackMode = ScriptureDiscPlaybackMode.SINGLE_CHAPTER;
        }
    }

    public ConfigureScriptureDiscPayload(InteractionHand hand, String translationId, String bookId, int chapter) {
        this(hand, translationId, bookId, chapter, "default", ScriptureDiscPlaybackMode.SINGLE_CHAPTER);
    }

    @Override
    public Type<ConfigureScriptureDiscPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ConfigureScriptureDiscPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buffer, payload.hand.ordinal());
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.translationId);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.bookId);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.chapter);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.audioManifestId);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.playbackMode.name());
    }

    private static ConfigureScriptureDiscPayload decode(RegistryFriendlyByteBuf buffer) {
        int handOrdinal = ByteBufCodecs.VAR_INT.decode(buffer);
        InteractionHand[] hands = InteractionHand.values();
        InteractionHand hand = hands[Math.clamp(handOrdinal, 0, hands.length - 1)];
        return new ConfigureScriptureDiscPayload(
            hand,
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            ByteBufCodecs.VAR_INT.decode(buffer),
            ByteBufCodecs.STRING_UTF8.decode(buffer),
            ScriptureDiscPlaybackMode.fromId(ByteBufCodecs.STRING_UTF8.decode(buffer))
        );
    }
}
