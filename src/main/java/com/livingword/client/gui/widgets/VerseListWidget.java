package com.livingword.client.gui.widgets;

import com.livingword.bible.BibleReference;
import com.livingword.bible.ChapterData;
import com.livingword.client.gui.BibleGuiState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

public final class VerseListWidget {
    private static final int TEXT = 0xFF3B2A18;
    private static final int SELECTED = 0xFF7C3F08;
    private static final int BOOKMARK = 0xFFC08A2E;
    private static final int HIGHLIGHT_FILL = 0x88F0C24A;
    private static final int HIGHLIGHT_SELECTED_FILL = 0xAAE8AD2D;
    private static final int HIGHLIGHT_BORDER = 0xFFE0A31A;
    private static final int HIGHLIGHT_ACCENT = 0xFFFFD45A;
    private static final int HIGHLIGHT_MARK = 0xFFE0A31A;
    private static final int SEARCH_RESULT_FILL = 0x66FFF0A0;
    private static final int SEARCH_RESULT_BORDER = 0xFFFFE17A;
    private static final int SEARCH_RESULT_UNDERLINE = 0xFFFFC400;
    private static final int SEARCH_MATCH_TEXT = 0xFFB64000;
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
            boolean selected = line.verseNumber() == state.selectedVerse();
            int color = selected ? SELECTED : TEXT;
            if (lineY + LINE_HEIGHT >= y && lineY <= y + height) {
                boolean firstLineOfVerse = line.verseNumber() != lastVerse;
                BibleReference reference = new BibleReference(chapter.translationId(), chapter.bookId(), chapter.chapter(), line.verseNumber());
                boolean highlighted = state.isHighlighted(reference);
                boolean activeSearchResult = state.currentSearchResult().filter(reference::equals).isPresent();
                if (highlighted) {
                    renderHighlightFrame(graphics, x + 6, lineY - 2, width - 12, LINE_HEIGHT + 1, selected);
                }
                if (activeSearchResult) {
                    renderActiveSearchFrame(graphics, x + 6, lineY - 2, width - 12, LINE_HEIGHT + 1);
                }
                if (firstLineOfVerse && highlighted) {
                    graphics.drawString(font, ">", x + 1, lineY, HIGHLIGHT_MARK, false);
                } else if (firstLineOfVerse && state.isBookmarked(reference)) {
                    graphics.drawString(font, "*", x, lineY, BOOKMARK, false);
                }
                drawSearchAwareText(graphics, font, line.text(), x + 14, lineY, color, state.searchQuery(), activeSearchResult);
            }
            lastVerse = line.verseNumber();
            lineY += LINE_HEIGHT;
        }
        graphics.disableScissor();
    }

    private static void renderHighlightFrame(GuiGraphics graphics, int left, int top, int width, int height, boolean selected) {
        int right = left + width;
        int bottom = top + height;
        graphics.fill(left, top, right, bottom, selected ? HIGHLIGHT_SELECTED_FILL : HIGHLIGHT_FILL);
        graphics.fill(left, top, left + 4, bottom, HIGHLIGHT_ACCENT);
        graphics.fill(left, top, right, top + 1, HIGHLIGHT_BORDER);
        graphics.fill(left, bottom - 1, right, bottom, HIGHLIGHT_BORDER);
        graphics.fill(right - 1, top, right, bottom, HIGHLIGHT_BORDER);
    }

    private static void renderActiveSearchFrame(GuiGraphics graphics, int left, int top, int width, int height) {
        int right = left + width;
        int bottom = top + height;
        graphics.fill(left, top, right, bottom, SEARCH_RESULT_FILL);
        graphics.fill(left, top, right, top + 1, SEARCH_RESULT_BORDER);
        graphics.fill(left + 8, bottom - 2, right - 5, bottom - 1, SEARCH_RESULT_UNDERLINE);
    }

    public static void drawSearchAwareText(GuiGraphics graphics, Font font, String text, int x, int y, int defaultColor, String query, boolean activeSearchResult) {
        if (!activeSearchResult) {
            graphics.drawString(font, text, x, y, defaultColor, false);
            return;
        }
        List<TextRange> ranges = searchHighlightRanges(text, query);
        if (ranges.isEmpty()) {
            graphics.drawString(font, text, x, y, defaultColor, false);
            return;
        }

        int cursorX = x;
        int cursor = 0;
        for (TextRange range : ranges) {
            if (range.start() > cursor) {
                String before = text.substring(cursor, range.start());
                graphics.drawString(font, before, cursorX, y, defaultColor, false);
                cursorX += font.width(before);
            }
            String match = text.substring(range.start(), range.end());
            graphics.drawString(font, match, cursorX, y, SEARCH_MATCH_TEXT, false);
            cursorX += font.width(match);
            cursor = range.end();
        }
        if (cursor < text.length()) {
            graphics.drawString(font, text.substring(cursor), cursorX, y, defaultColor, false);
        }
    }

    public static List<TextRange> searchHighlightRanges(String text, String query) {
        if (text == null || text.isBlank() || query == null || query.isBlank()) {
            return List.of();
        }
        String normalizedText = text.toLowerCase(Locale.ROOT);
        List<String> terms = searchTerms(query);
        List<TextRange> found = new ArrayList<>();
        for (String term : terms) {
            int fromIndex = 0;
            while (fromIndex < normalizedText.length()) {
                int index = normalizedText.indexOf(term, fromIndex);
                if (index < 0) {
                    break;
                }
                found.add(new TextRange(index, index + term.length()));
                fromIndex = index + term.length();
            }
        }
        if (found.isEmpty()) {
            return List.of();
        }
        found.sort(java.util.Comparator.comparingInt(TextRange::start).thenComparingInt(TextRange::end));
        List<TextRange> merged = new ArrayList<>();
        for (TextRange range : found) {
            if (merged.isEmpty()) {
                merged.add(range);
                continue;
            }
            TextRange previous = merged.getLast();
            if (range.start() <= previous.end()) {
                merged.set(merged.size() - 1, new TextRange(previous.start(), Math.max(previous.end(), range.end())));
            } else {
                merged.add(range);
            }
        }
        return List.copyOf(merged);
    }

    private static List<String> searchTerms(String query) {
        String normalized = query.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip();
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split("\\s+")).stream()
            .filter(term -> term.length() > 1)
            .distinct()
            .toList();
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

    public int scrollOffsetForVerse(ChapterData chapter, int height, int verseNumber) {
        return scrollOffsetForVerse(WrappedVerseLayout.wrap(chapter, 80, String::length), height, verseNumber);
    }

    public int scrollOffsetForVerse(ChapterData chapter, Font font, int width, int height, int verseNumber) {
        if (font == null) {
            return scrollOffsetForVerse(chapter, height, verseNumber);
        }
        return scrollOffsetForVerse(wrappedLines(chapter, font, width), height, verseNumber);
    }

    private static int scrollOffsetForVerse(List<WrappedVerseLayout.VisualLine> lines, int height, int verseNumber) {
        if (lines.isEmpty() || height <= 0) {
            return 0;
        }
        int selectedLineIndex = 0;
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).verseNumber() == verseNumber) {
                selectedLineIndex = index;
                break;
            }
        }
        int selectedLineMiddle = selectedLineIndex * LINE_HEIGHT + LINE_HEIGHT / 2;
        int desiredOffset = Math.max(0, selectedLineMiddle - height / 2);
        int maxOffset = Math.max(0, lines.size() * LINE_HEIGHT - height);
        return Math.min(desiredOffset, maxOffset);
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

    public record TextRange(int start, int end) {
    }
}
