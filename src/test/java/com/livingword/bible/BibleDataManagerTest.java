package com.livingword.bible;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BibleDataManagerTest {
    @Test
    void retrievesVerseByStableReference() {
        BibleDataManager manager = new BibleDataManager();
        manager.registerTranslation(new TranslationManifest("kjv", "King James Version", "en_us", "Public Domain", "", "ltr", List.of("john"), "kjv-default"));
        manager.registerChapter(new ChapterData("kjv", "john", 3, Map.of(16, "For God so loved the world...")));

        assertEquals("For God so loved the world...", manager.getVerse(new BibleReference("kjv", "john", 3, 16)).orElseThrow());
        assertTrue(manager.translations().stream().anyMatch(t -> t.id().equals("kjv")));
    }
}
