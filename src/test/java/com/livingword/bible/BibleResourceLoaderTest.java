package com.livingword.bible;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BibleResourceLoaderTest {
    @Test
    void loadsBundledIndexedResources() {
        BibleDataManager manager = new BibleDataManager();
        BibleResourceLoader loader = new BibleResourceLoader(manager, BibleResourceLoaderTest.class.getClassLoader());

        loader.reload(List.of("kjv"));

        assertTrue(manager.getTranslation("kjv").isPresent());
        assertEquals(List.of("john", "psalms"), manager.bookIds("kjv"));
        assertEquals(List.of(3), manager.chapters("kjv", "john"));
        assertEquals("For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
            manager.getVerse(new BibleReference("kjv", "john", 3, 16)).orElseThrow());
    }

    @Test
    void searchesLoadedVersesDeterministically() {
        BibleDataManager manager = new BibleDataManager();
        BibleResourceLoader loader = new BibleResourceLoader(manager, BibleResourceLoaderTest.class.getClassLoader());
        loader.reload(List.of("kjv"));

        List<BibleReference> results = manager.search("kjv", "world", 10);

        assertFalse(results.isEmpty());
        assertEquals(new BibleReference("kjv", "john", 3, 16), results.getFirst());
        assertTrue(results.contains(new BibleReference("kjv", "john", 3, 17)));
    }
}
