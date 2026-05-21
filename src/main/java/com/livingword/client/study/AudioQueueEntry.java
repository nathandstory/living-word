package com.livingword.client.study;

public record AudioQueueEntry(String translationId, String bookId, int chapter) {
    public AudioQueueEntry {
        if (translationId == null || translationId.isBlank()) {
            throw new IllegalArgumentException("translationId is required");
        }
        if (bookId == null || bookId.isBlank()) {
            throw new IllegalArgumentException("bookId is required");
        }
        if (chapter < 1) {
            throw new IllegalArgumentException("chapter must be positive");
        }
    }
}
