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
    void reloadUsesBundledTranslationRegistry() {
        BibleDataManager manager = new BibleDataManager();
        BibleResourceLoader loader = new BibleResourceLoader(manager, BibleResourceLoaderTest.class.getClassLoader());

        loader.reload();

        assertTrue(manager.getTranslation("bsb").isPresent());
        assertTrue(manager.getTranslation("kjv").isPresent());
        assertTrue(manager.getTranslation("webp").isPresent());
        assertEquals("bsb", manager.translations().getFirst().id());
        assertEquals(66, manager.bookIds("bsb").size());
        assertEquals(1189, manager.bookIds("bsb").stream().mapToInt(bookId -> manager.chapters("bsb", bookId).size()).sum());
        assertEquals(31086, verseCount(manager, "bsb"));
        assertEquals("Blessed is the man who does not walk in the counsel of the wicked, or set foot on the path of sinners, or sit in the seat of mockers.",
            manager.getVerse(new BibleReference("bsb", "psalms", 1, 1)).orElseThrow());
        assertEquals("These are the proverbs of Solomon son of David, king of Israel,",
            manager.getVerse(new BibleReference("bsb", "proverbs", 1, 1)).orElseThrow());
        assertEquals("So God created man in His own image; in the image of God He created him; male and female He created them.",
            manager.getVerse(new BibleReference("bsb", "genesis", 1, 27)).orElseThrow());
        assertEquals("For God so loved the world that He gave His one and only Son, that everyone who believes in Him shall not perish but have eternal life.",
            manager.getVerse(new BibleReference("bsb", "john", 3, 16)).orElseThrow());
        assertEquals(66, manager.bookIds("webp").size());
        assertEquals(1189, manager.bookIds("webp").stream().mapToInt(bookId -> manager.chapters("webp", bookId).size()).sum());
        assertEquals("For God so loved the world, that he gave his only born Son, that whoever believes in him should not perish, but have eternal life.",
            manager.getVerse(new BibleReference("webp", "john", 3, 16)).orElseThrow());
        assertEquals("The grace of the Lord Jesus Christ be with all the saints. Amen.",
            manager.getVerse(new BibleReference("webp", "revelation", 22, 21)).orElseThrow());
    }

    @Test
    void bundledTranslationsHaveReadableChapterContent() {
        BibleDataManager manager = new BibleDataManager();
        BibleResourceLoader loader = new BibleResourceLoader(manager, BibleResourceLoaderTest.class.getClassLoader());

        loader.reload();

        for (TranslationManifest translation : manager.translations()) {
            for (String bookId : manager.bookIds(translation.id())) {
                assertFalse(manager.chapters(translation.id(), bookId).isEmpty(), translation.id() + " " + bookId + " has no chapters");
                boolean bookHasSubstantialVerse = false;
                for (int chapterNumber : manager.chapters(translation.id(), bookId)) {
                    ChapterData chapter = manager.getChapter(translation.id(), bookId, chapterNumber).orElseThrow();
                    assertFalse(chapter.verses().isEmpty(), chapterLabel(chapter) + " has no verses");
                    assertFalse(chapter.verses().values().stream().allMatch(BibleResourceLoaderTest::isSelahOnly), chapterLabel(chapter) + " is only Selah");
                    assertFalse(chapter.verses().values().stream().allMatch(BibleResourceLoaderTest::isBlank), chapterLabel(chapter) + " is blank");
                    for (String verseText : chapter.verses().values()) {
                        assertFalse(isBlank(verseText), chapterLabel(chapter) + " contains a blank verse");
                        if (verseText.strip().length() > 20 && !isSelahOnly(verseText)) {
                            bookHasSubstantialVerse = true;
                        }
                    }
                }
                assertTrue(bookHasSubstantialVerse, translation.id() + " " + bookId + " has no substantial readable verse");
            }
        }
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

    @Test
    void searchUnderstandsReferenceStyleQueries() {
        BibleDataManager manager = new BibleDataManager();
        BibleResourceLoader loader = new BibleResourceLoader(manager, BibleResourceLoaderTest.class.getClassLoader());
        loader.reload(List.of("kjv"));

        assertEquals(List.of(new BibleReference("kjv", "revelation", 1, 1)), manager.search("kjv", "revelation:", 10));
        assertEquals(List.of(new BibleReference("kjv", "revelation", 22, 21)), manager.search("kjv", "Rev 22:21", 10));
        assertEquals(List.of(new BibleReference("kjv", "john", 3, 16)), manager.search("kjv", "Jn 3:16", 10));
        assertEquals(List.of(new BibleReference("kjv", "john", 3, 16)), manager.search("kjv", "john 3 16", 10));
        assertEquals(List.of(new BibleReference("kjv", "1_john", 4, 8)), manager.search("kjv", "first john 4:8", 10));
    }

    @Test
    void searchFindsRelevantVersesWhenWordsAreNotContiguous() {
        BibleDataManager manager = new BibleDataManager();
        BibleResourceLoader loader = new BibleResourceLoader(manager, BibleResourceLoaderTest.class.getClassLoader());
        loader.reload(List.of("kjv"));

        List<BibleReference> results = manager.search("kjv", "loved world", 10);

        assertTrue(results.contains(new BibleReference("kjv", "john", 3, 16)));
    }

    @Test
    void broadSearchesCanReachLateBooksWithDefaultResultLimit() {
        BibleDataManager manager = new BibleDataManager();
        BibleResourceLoader loader = new BibleResourceLoader(manager, BibleResourceLoaderTest.class.getClassLoader());
        loader.reload(List.of("bsb"));

        List<BibleReference> results = manager.search("bsb", "beast", 500);

        assertTrue(results.contains(new BibleReference("bsb", "revelation", 13, 1)));
        assertTrue(results.contains(new BibleReference("bsb", "revelation", 20, 10)));
    }

    private static int verseCount(BibleDataManager manager, String translationId) {
        int count = 0;
        for (String bookId : manager.bookIds(translationId)) {
            for (int chapter : manager.chapters(translationId, bookId)) {
                count += manager.getChapter(translationId, bookId, chapter).orElseThrow().verses().size();
            }
        }
        return count;
    }

    private static String chapterLabel(ChapterData chapter) {
        return chapter.translationId() + " " + chapter.bookId() + " " + chapter.chapter();
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private static boolean isSelahOnly(String text) {
        return text != null && text.strip().equalsIgnoreCase("selah");
    }
}
