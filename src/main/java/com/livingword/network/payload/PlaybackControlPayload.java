package com.livingword.network.payload;

import com.livingword.LivingWord;
import com.livingword.sync.PlaybackState;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.UUID;

public record PlaybackControlPayload(UUID sessionId, PlaybackState state, long positionMillis, long serverTimeMillis) implements CustomPacketPayload {
    public static final Type<PlaybackControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, "playback_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlaybackControlPayload> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        PlaybackControlPayload::sessionId,
        NeoForgeStreamCodecs.enumCodec(PlaybackState.class),
        PlaybackControlPayload::state,
        ByteBufCodecs.VAR_LONG,
        PlaybackControlPayload::positionMillis,
        ByteBufCodecs.VAR_LONG,
        PlaybackControlPayload::serverTimeMillis,
        PlaybackControlPayload::new
    );

    @Override
    public Type<PlaybackControlPayload> type() {
        return TYPE;
    }
}
