package com.livingword.client.gui.widgets;

import com.livingword.bible.ChapterData;
import com.livingword.client.gui.BibleGuiState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public final class VerseListWidget {
    private static final int TEXT = 0xFFE8D7B5;
    private static final int SELECTED = 0xFFFFD37A;
    private static final int LINE_HEIGHT = 14;

    public void render(GuiGraphics graphics, Font font, ChapterData chapter, BibleGuiState state, int x, int y, int width) {
        int lineY = y;
        for (Map.Entry<Integer, String> verse : sortedVerses(chapter)) {
            int color = verse.getKey() == state.selectedVerse() ? SELECTED : TEXT;
            String line = verse.getKey() + ". " + verse.getValue();
            graphics.drawString(font, font.plainSubstrByWidth(line, width), x, lineY, color, false);
            lineY += LINE_HEIGHT;
        }
    }

    public OptionalInt verseAt(ChapterData chapter, int y, double mouseY) {
        int index = ((int) mouseY - y) / LINE_HEIGHT;
        List<Map.Entry<Integer, String>> verses = sortedVerses(chapter);
        if (index < 0 || index >= verses.size()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(verses.get(index).getKey());
    }

    private static List<Map.Entry<Integer, String>> sortedVerses(ChapterData chapter) {
        return chapter.verses().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
    }
}
