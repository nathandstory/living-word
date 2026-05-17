package com.livingword.network.payload;

import com.livingword.LivingWord;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record JoinListeningSessionPayload(UUID sessionId) implements CustomPacketPayload {
    public static final Type<JoinListeningSessionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, "join_listening_session"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JoinListeningSessionPayload> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        JoinListeningSessionPayload::sessionId,
        JoinListeningSessionPayload::new
    );

    @Override
    public Type<JoinListeningSessionPayload> type() {
        return TYPE;
    }
}
