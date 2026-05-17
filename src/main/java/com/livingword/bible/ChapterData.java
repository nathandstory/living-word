package com.livingword.bible;

import java.util.Map;

public record ChapterData(String translationId, String bookId, int chapter, Map<Integer, String> verses) {
    public ChapterData {
        if (chapter < 1) {
            throw new IllegalArgumentException("chapter must be positive");
        }
        verses = Map.copyOf(verses == null ? Map.of() : verses);
    }

    public String verseText(int verse) {
        return verses.getOrDefault(verse, "");
    }

    public java.util.Optional<String> getVerse(int verse) {
        return java.util.Optional.ofNullable(verses.get(verse));
    }
}
