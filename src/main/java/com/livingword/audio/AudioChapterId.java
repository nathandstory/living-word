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
        return fileName("ogg");
    }

    public String fileName(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("extension is required");
        }
        String normalizedExtension = extension.startsWith(".") ? extension.substring(1) : extension;
        return "%s_%03d.%s".formatted(bookId, chapter, normalizedExtension.toLowerCase(java.util.Locale.ROOT));
    }

    public String timestampsFileName() {
        return "%s_%03d.timestamps.json".formatted(bookId, chapter);
    }
}
