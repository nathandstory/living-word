package com.livingword.discs;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class ScriptureDisc extends Item {
    private final String displayKey;
    private final String translationId;
    private final String bookId;
    private final int startChapter;
    private final int endChapter;
    private final String audioManifestId;

    public ScriptureDisc(
        Properties properties,
        String displayKey,
        String translationId,
        String bookId,
        int startChapter,
        int endChapter,
        String audioManifestId
    ) {
        super(properties);
        this.displayKey = displayKey;
        this.translationId = translationId;
        this.bookId = bookId;
        this.startChapter = startChapter;
        this.endChapter = endChapter;
        this.audioManifestId = audioManifestId;
    }

    public String displayKey() {
        return displayKey;
    }

    public String translationId() {
        return translationId;
    }

    public String bookId() {
        return bookId;
    }

    public int startChapter() {
        return startChapter;
    }

    public int endChapter() {
        return endChapter;
    }

    public String audioManifestId() {
        return audioManifestId;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(displayKey).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.livingword.scripture_disc.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
