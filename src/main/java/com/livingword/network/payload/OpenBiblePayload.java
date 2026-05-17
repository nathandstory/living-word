package com.livingword.network.payload;

import com.livingword.LivingWord;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenBiblePayload(String translationId, String bookId, int chapter) implements CustomPacketPayload {
    public static final Type<OpenBiblePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LivingWord.MOD_ID, "open_bible"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBiblePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        OpenBiblePayload::translationId,
        ByteBufCodecs.STRING_UTF8,
        OpenBiblePayload::bookId,
        ByteBufCodecs.VAR_INT,
        OpenBiblePayload::chapter,
        OpenBiblePayload::new
    );

    @Override
    public Type<OpenBiblePayload> type() {
        return TYPE;
    }
}
