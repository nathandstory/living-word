package com.livingword.network.payload;

import com.livingword.LivingWord;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record TimestampCorrectionPayload(UUID sessionId, long positionMillis, long serverTimeMillis) implements CustomPacketPayload {
    public static final Type<TimestampCorrectionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, "timestamp_correction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TimestampCorrectionPayload> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        TimestampCorrectionPayload::sessionId,
        ByteBufCodecs.VAR_LONG,
        TimestampCorrectionPayload::positionMillis,
        ByteBufCodecs.VAR_LONG,
        TimestampCorrectionPayload::serverTimeMillis,
        TimestampCorrectionPayload::new
    );

    @Override
    public Type<TimestampCorrectionPayload> type() {
        return TYPE;
    }
}
