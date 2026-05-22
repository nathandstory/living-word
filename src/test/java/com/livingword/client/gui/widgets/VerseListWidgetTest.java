package com.livingword.client.gui.widgets;

import com.livingword.bible.ChapterData;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VerseListWidgetTest {
    @Test
    void wrapsLongVersesIntoReadableVisualLines() {
        ChapterData chapter = new ChapterData("kjv", "john", 3, Map.of(
            16, "For God so loved the world, that he gave his only begotten Son."
        ));

        var lines = WrappedVerseLayout.wrap(chapter, 24, String::length);

        assertTrue(lines.size() > 1);
        assertTrue(lines.stream().allMatch(line -> line.text().length() <= 24));
        assertEquals(16, lines.getFirst().verseNumber());
        assertEquals(16, lines.getLast().verseNumber());
    }

    @Test
    void hitTestingAccountsForScrollOffset() {
        VerseListWidget widget = new VerseListWidget();
        ChapterData chapter = new ChapterData("kjv", "psalms", 119, Map.of(
            1, "Blessed are the undefiled in the way.",
            2, "Blessed are they that keep his testimonies.",
            3, "They also do no iniquity."
        ));

        assertEquals(1, widget.verseAt(chapter, 100, 100, 0).orElseThrow());
        assertEquals(2, widget.verseAt(chapter, 100, 100, 14).orElseThrow());
        assertEquals(3, widget.verseAt(chapter, 100, 114, 14).orElseThrow());
        assertTrue(widget.verseAt(chapter, 100, 200, 14).isEmpty());
    }

    @Test
    void computesScrollOffsetThatBringsLaterSelectedVerseIntoView() {
        VerseListWidget widget = new VerseListWidget();
        ChapterData chapter = new ChapterData("kjv", "psalms", 1, Map.of(
            1, "verse one",
            2, "verse two",
            3, "verse three",
            4, "verse four",
            5, "verse five",
            6, "verse six",
            7, "verse seven",
            8, "verse eight"
        ));

        int scrollOffset = widget.scrollOffsetForVerse(chapter, 42, 8);

        assertTrue(scrollOffset > 0);
        assertEquals(8, widget.verseAt(chapter, 100, 135, scrollOffset).orElseThrow());
    }

    @Test
    void scrollOffsetFallsBackWhenScreenFontIsNotReadyYet() {
        VerseListWidget widget = new VerseListWidget();
        ChapterData chapter = new ChapterData("kjv", "psalms", 1, Map.of(
            1, "verse one",
            2, "verse two",
            3, "verse three",
            4, "verse four",
            5, "verse five"
        ));

        int scrollOffset = widget.scrollOffsetForVerse(chapter, null, 120, 28, 5);

        assertTrue(scrollOffset > 0);
    }

    @Test
    void highlightedVersesUseStrongFillBorderAndLeftAccent() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/widgets/VerseListWidget.java"));

        assertTrue(source.contains("HIGHLIGHT_SELECTED_FILL"));
        assertTrue(source.contains("HIGHLIGHT_BORDER"));
        assertTrue(source.contains("renderHighlightFrame"));
        assertTrue(source.contains("graphics.fill(left, top, left + 4"));
        assertFalse(source.contains("private static final int HIGHLIGHT = 0x55E7B844"));
        assertFalse(source.contains("graphics.drawString(font, \"|\""));
    }

    @Test
    void activeSearchResultUnderlinesVerseAndColorsMatchedText() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/widgets/VerseListWidget.java"));

        assertTrue(source.contains("SEARCH_RESULT_FILL"));
        assertTrue(source.contains("SEARCH_MATCH_TEXT"));
        assertTrue(source.contains("SEARCH_RESULT_UNDERLINE"));
        assertTrue(source.contains("renderActiveSearchFrame"));
        assertTrue(source.contains("drawSearchAwareText"));
        assertTrue(source.contains("searchHighlightRanges"));
    }

    @Test
    void searchHighlightRangesFindsCaseInsensitiveQueryTerms() {
        var ranges = VerseListWidget.searchHighlightRanges("16. For God so loved the world", "god loved");

        assertEquals(java.util.List.of(
            new VerseListWidget.TextRange(8, 11),
            new VerseListWidget.TextRange(15, 20)
        ), ranges);
    }
}
