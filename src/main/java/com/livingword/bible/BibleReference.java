package com.livingword.bible;

public record BibleReference(String translationId, String bookId, int chapter, int verse) {
    public BibleReference {
        if (isBlank(translationId)) {
            throw new IllegalArgumentException("translationId is required");
        }
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

    public String toStableId() {
        return translationId + ':' + bookId + ':' + chapter + ':' + verse;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
