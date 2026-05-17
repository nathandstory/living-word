package com.livingword.bible;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BibleTranslationIndex(String translationId, Map<String, List<Integer>> books) {
    public BibleTranslationIndex {
        if (translationId == null || translationId.isBlank()) {
            throw new IllegalArgumentException("translationId is required");
        }
        Map<String, List<Integer>> copied = new LinkedHashMap<>();
        if (books != null) {
            books.forEach((bookId, chapters) -> {
                if (bookId == null || bookId.isBlank()) {
                    throw new IllegalArgumentException("book id is required");
                }
                copied.put(bookId, List.copyOf(chapters == null ? List.of() : chapters));
            });
        }
        books = Collections.unmodifiableMap(copied);
    }

    public List<String> bookIds() {
        return List.copyOf(books.keySet());
    }

    public List<Integer> chapters(String bookId) {
        return books.getOrDefault(bookId, List.of());
    }
}
