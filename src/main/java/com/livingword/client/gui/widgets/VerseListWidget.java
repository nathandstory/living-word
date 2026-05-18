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
        render(graphics, font, chapter, state, x, y, width, contentHeight(chapter), 0);
    }

    public void render(GuiGraphics graphics, Font font, ChapterData chapter, BibleGuiState state, int x, int y, int width, int height, int scrollOffset) {
        graphics.enableScissor(x, y, x + width, y + height);
        int lineY = y;
        lineY -= scrollOffset;
        for (Map.Entry<Integer, String> verse : sortedVerses(chapter)) {
            int color = verse.getKey() == state.selectedVerse() ? SELECTED : TEXT;
            String line = verse.getKey() + ". " + verse.getValue();
            if (lineY + LINE_HEIGHT >= y && lineY <= y + height) {
                graphics.drawString(font, font.plainSubstrByWidth(line, width), x, lineY, color, false);
            }
            lineY += LINE_HEIGHT;
        }
        graphics.disableScissor();
    }

    public OptionalInt verseAt(ChapterData chapter, int y, double mouseY) {
        return verseAt(chapter, y, mouseY, 0);
    }

    public OptionalInt verseAt(ChapterData chapter, int y, double mouseY, int scrollOffset) {
        int index = (((int) mouseY - y) + scrollOffset) / LINE_HEIGHT;
        List<Map.Entry<Integer, String>> verses = sortedVerses(chapter);
        if (index < 0 || index >= verses.size()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(verses.get(index).getKey());
    }

    public int maxScroll(ChapterData chapter, int height) {
        return Math.max(0, contentHeight(chapter) - height);
    }

    private static int contentHeight(ChapterData chapter) {
        return sortedVerses(chapter).size() * LINE_HEIGHT;
    }

    private static List<Map.Entry<Integer, String>> sortedVerses(ChapterData chapter) {
        return chapter.verses().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
    }
}
