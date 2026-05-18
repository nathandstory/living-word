package com.livingword.bible;

import java.util.Optional;

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

    public static Optional<BibleReference> parseStableId(String stableId) {
        if (stableId == null || stableId.isBlank()) {
            return Optional.empty();
        }
        String[] parts = stableId.split(":");
        if (parts.length != 4) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BibleReference(parts[0], parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
