package com.livingword.audio;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

public record AudioManifest(String id, String translationId, URI baseUri, Map<AudioChapterId, String> chapterHashes) {
    public AudioManifest {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("audio manifest id is required");
        }
        if (translationId == null || translationId.isBlank()) {
            throw new IllegalArgumentException("translationId is required");
        }
        if (baseUri == null) {
            throw new IllegalArgumentException("baseUri is required");
        }
        chapterHashes = Map.copyOf(chapterHashes == null ? Map.of() : chapterHashes);
    }

    public URI chapterUri(AudioChapterId chapterId) {
        return baseUri.resolve("%s/%s".formatted(chapterId.bookId(), chapterId.fileName()));
    }

    public Optional<String> expectedHash(AudioChapterId chapterId) {
        return Optional.ofNullable(chapterHashes.get(chapterId));
    }
}
