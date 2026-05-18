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
        assertEquals(66, manager.bookIds("kjv").size());
        assertEquals(1189, manager.bookIds("kjv").stream().mapToInt(bookId -> manager.chapters("kjv", bookId).size()).sum());
        assertEquals(50, manager.chapters("kjv", "genesis").size());
        assertEquals(22, manager.chapters("kjv", "revelation").size());
        assertEquals("In the beginning God created the heaven and the earth.",
            manager.getVerse(new BibleReference("kjv", "genesis", 1, 1)).orElseThrow());
        assertEquals("For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
            manager.getVerse(new BibleReference("kjv", "john", 3, 16)).orElseThrow());
        assertEquals("The grace of our Lord Jesus Christ be with you all. Amen.",
            manager.getVerse(new BibleReference("kjv", "revelation", 22, 21)).orElseThrow());
    }

    @Test
    void searchesLoadedVersesDeterministically() {
        BibleDataManager manager = new BibleDataManager();
        BibleResourceLoader loader = new BibleResourceLoader(manager, BibleResourceLoaderTest.class.getClassLoader());
        loader.reload(List.of("kjv"));

        List<BibleReference> results = manager.search("kjv", "world", 10);

        assertFalse(results.isEmpty());
        assertEquals(new BibleReference("kjv", "1_samuel", 2, 8), results.getFirst());
        assertTrue(manager.search("kjv", "begotten Son", 10).contains(new BibleReference("kjv", "john", 3, 16)));
    }
}
