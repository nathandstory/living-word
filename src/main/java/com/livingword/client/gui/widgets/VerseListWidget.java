package com.livingword.client.gui.widgets;

import com.livingword.bible.ChapterData;
import com.livingword.client.gui.BibleGuiState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Map;

public final class VerseListWidget {
    private static final int TEXT = 0xFFE8D7B5;
    private static final int SELECTED = 0xFFFFD37A;

    public void render(GuiGraphics graphics, Font font, ChapterData chapter, BibleGuiState state, int x, int y, int width) {
        int lineY = y;
        for (Map.Entry<Integer, String> verse : chapter.verses().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            int color = verse.getKey() == state.selectedVerse() ? SELECTED : TEXT;
            String line = verse.getKey() + ". " + verse.getValue();
            graphics.drawString(font, font.plainSubstrByWidth(line, width), x, lineY, color, false);
            lineY += 14;
        }
    }
}
