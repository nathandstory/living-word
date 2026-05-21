package com.livingword.network.payload;

import com.livingword.LivingWord;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ChapterFinishedPayload(UUID sessionId) implements CustomPacketPayload {
    public static final Type<ChapterFinishedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, "chapter_finished"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChapterFinishedPayload> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        ChapterFinishedPayload::sessionId,
        ChapterFinishedPayload::new
    );

    @Override
    public Type<ChapterFinishedPayload> type() {
        return TYPE;
    }
}
