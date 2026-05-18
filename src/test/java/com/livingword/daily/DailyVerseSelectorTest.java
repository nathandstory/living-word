package com.livingword.daily;

import com.livingword.bible.BibleDataManager;
import com.livingword.bible.BibleReference;
import com.livingword.bible.ChapterData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DailyVerseSelectorTest {
    @Test
    void sameDateAndSeedSelectSameVerse() {
        BibleDataManager bible = bible();
        DailyVerseSelector selector = new DailyVerseSelector(List.of(
            new BibleReference("kjv", "psalms", 23, 1),
            new BibleReference("kjv", "john", 3, 16)
        ));

        DailyVerse first = selector.select(bible, LocalDate.of(2026, 5, 18), 42L).orElseThrow();
        DailyVerse second = selector.select(bible, LocalDate.of(2026, 5, 18), 42L).orElseThrow();

        assertEquals(first, second);
        assertTrue(first.text().contains("Lord") || first.text().contains("God"));
    }

    @Test
    void unavailablePoolReturnsEmpty() {
        DailyVerseSelector selector = new DailyVerseSelector(List.of(new BibleReference("kjv", "missing", 1, 1)));

        assertTrue(selector.select(bible(), LocalDate.of(2026, 5, 18), 42L).isEmpty());
    }

    private static BibleDataManager bible() {
        BibleDataManager manager = new BibleDataManager();
        manager.registerChapter(new ChapterData("kjv", "psalms", 23, Map.of(1, "The Lord is my shepherd; I shall not want.")));
        manager.registerChapter(new ChapterData("kjv", "john", 3, Map.of(16, "For God so loved the world.")));
        return manager;
    }
}
