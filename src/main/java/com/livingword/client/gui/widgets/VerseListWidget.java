package com.livingword.client.gui.widgets;

import com.livingword.bible.BibleReference;
import com.livingword.bible.ChapterData;
import com.livingword.client.gui.BibleGuiState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.OptionalInt;

public final class VerseListWidget {
    private static final int TEXT = 0xFF3B2A18;
    private static final int SELECTED = 0xFF7C3F08;
    private static final int BOOKMARK = 0xFFC08A2E;
    private static final int LINE_HEIGHT = 14;

    public void render(GuiGraphics graphics, Font font, ChapterData chapter, BibleGuiState state, int x, int y, int width) {
        render(graphics, font, chapter, state, x, y, width, contentHeight(chapter), 0);
    }

    public void render(GuiGraphics graphics, Font font, ChapterData chapter, BibleGuiState state, int x, int y, int width, int height, int scrollOffset) {
        graphics.enableScissor(x, y, x + width, y + height);
        int lineY = y;
        lineY -= scrollOffset;
        int lastVerse = -1;
        for (WrappedVerseLayout.VisualLine line : wrappedLines(chapter, font, width)) {
            int color = line.verseNumber() == state.selectedVerse() ? SELECTED : TEXT;
            if (lineY + LINE_HEIGHT >= y && lineY <= y + height) {
                boolean firstLineOfVerse = line.verseNumber() != lastVerse;
                if (firstLineOfVerse && state.isBookmarked(new BibleReference(chapter.translationId(), chapter.bookId(), chapter.chapter(), line.verseNumber()))) {
                    graphics.drawString(font, "*", x, lineY, BOOKMARK, false);
                }
                graphics.drawString(font, line.text(), x + 10, lineY, color, false);
            }
            lastVerse = line.verseNumber();
            lineY += LINE_HEIGHT;
        }
        graphics.disableScissor();
    }

    public OptionalInt verseAt(ChapterData chapter, int y, double mouseY) {
        return verseAt(chapter, y, mouseY, 0);
    }

    public OptionalInt verseAt(ChapterData chapter, int y, double mouseY, int scrollOffset) {
        return verseAt(WrappedVerseLayout.wrap(chapter, 80, String::length), y, mouseY, scrollOffset);
    }

    public OptionalInt verseAt(ChapterData chapter, Font font, int width, int y, double mouseY, int scrollOffset) {
        return verseAt(wrappedLines(chapter, font, width), y, mouseY, scrollOffset);
    }

    private OptionalInt verseAt(List<WrappedVerseLayout.VisualLine> lines, int y, double mouseY, int scrollOffset) {
        int index = (((int) mouseY - y) + scrollOffset) / LINE_HEIGHT;
        if (index < 0 || index >= lines.size()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(lines.get(index).verseNumber());
    }

    public int maxScroll(ChapterData chapter, int height) {
        return Math.max(0, contentHeight(chapter) - height);
    }

    public int maxScroll(ChapterData chapter, Font font, int width, int height) {
        return Math.max(0, contentHeight(chapter, font, width) - height);
    }

    private static int contentHeight(ChapterData chapter) {
        return WrappedVerseLayout.wrap(chapter, 80, String::length).size() * LINE_HEIGHT;
    }

    private static int contentHeight(ChapterData chapter, Font font, int width) {
        return wrappedLines(chapter, font, width).size() * LINE_HEIGHT;
    }

    private static List<WrappedVerseLayout.VisualLine> wrappedLines(ChapterData chapter, Font font, int width) {
        return WrappedVerseLayout.wrap(chapter, Math.max(24, width - 18), font::width);
    }
}
