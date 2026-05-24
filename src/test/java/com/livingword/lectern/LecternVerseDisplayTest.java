package com.livingword.lectern;

import com.livingword.bible.ChapterData;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LecternVerseDisplayTest {
    @Test
    void choosesCurrentVerseFromEstimatedPlaybackPosition() {
        ChapterData chapter = new ChapterData("bsb", "john", 3, Map.of(
            1, "Short verse.",
            2, "This is a much longer verse with enough words to occupy more narration time.",
            3, "Final verse."
        ));

        assertEquals(1, LecternVerseDisplay.verseAt(chapter, 0L));
        assertEquals(2, LecternVerseDisplay.verseAt(chapter, 1_500L));
        assertEquals(2, LecternVerseDisplay.verseAt(chapter, 6_000L));
        assertEquals(3, LecternVerseDisplay.verseAt(chapter, 7_500L));
    }

    @Test
    void appliesNarratorSpecificDisplayOffset() {
        ChapterData chapter = new ChapterData("bsb", "john", 3, Map.of(
            1, "Short verse.",
            2, "Next verse."
        ));

        assertEquals(2, LecternVerseDisplay.verseAt(chapter, 1_300L));
        assertEquals(1, LecternVerseDisplay.verseAt(chapter, 1_300L, "bsb", "default"));
    }

    @Test
    void punctuationHoldsVerseLongEnoughForNaturalNarrationPauses() {
        ChapterData chapter = new ChapterData("bsb", "psalms", 1, Map.of(
            1, "Listen, my son; wait.",
            2, "Next."
        ));

        assertEquals(1, LecternVerseDisplay.verseAt(chapter, 1_900L));
    }

    @Test
    void formatsCompactFloatingVerseText() {
        String text = LecternVerseDisplay.text("BSB John 3", 2, "For God so loved the world.");

        assertEquals("BSB John 3:2 - For God so loved the world.", text);
    }

    @Test
    void wrapsLongFloatingTextWithoutLineFeedGlyphsInEntityNames() {
        var lines = LecternVerseDisplay.lines("BSB Psalms 119", 105, "A".repeat(120));

        assertTrue(lines.size() > 1);
        assertTrue(lines.stream().noneMatch(line -> line.contains("\n")));
        assertTrue(String.join("", lines).length() > 120);
    }

    @Test
    void cyclesReadableDisplayColorsGradually() {
        int first = LecternVerseDisplay.colorAt(0L);
        int later = LecternVerseDisplay.colorAt(1_500L);

        assertTrue(first > 0);
        assertTrue(later > 0);
        assertTrue(first != later);
    }
}
