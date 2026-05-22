package com.livingword.bible;

public record BiblePassageKey(String bookId, int chapter, int verse) {
    public BiblePassageKey {
        if (isBlank(bookId)) {
            throw new IllegalArgumentException("bookId is required");
        }
        if (chapter < 1) {
            throw new IllegalArgumentException("chapter must be positive");
        }
        if (verse < 1) {
            throw new IllegalArgumentException("verse must be positive");
        }
    }

    public static BiblePassageKey from(BibleReference reference) {
        return new BiblePassageKey(reference.bookId(), reference.chapter(), reference.verse());
    }

    public BibleReference toReference(String translationId) {
        return new BibleReference(translationId, bookId, chapter, verse);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
