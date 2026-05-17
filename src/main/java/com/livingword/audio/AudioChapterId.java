package com.livingword.audio;

public record AudioChapterId(String translationId, String bookId, int chapter) {
    public AudioChapterId {
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

    public String fileName() {
        return "%s_%03d.ogg".formatted(bookId, chapter);
    }

    public String timestampsFileName() {
        return "%s_%03d.timestamps.json".formatted(bookId, chapter);
    }
}
