package com.livingword.network.payload;

import com.livingword.LivingWord;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record LeaveListeningSessionPayload(UUID sessionId) implements CustomPacketPayload {
    public static final Type<LeaveListeningSessionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, "leave_listening_session"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LeaveListeningSessionPayload> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        LeaveListeningSessionPayload::sessionId,
        LeaveListeningSessionPayload::new
    );

    @Override
    public Type<LeaveListeningSessionPayload> type() {
        return TYPE;
    }
}
