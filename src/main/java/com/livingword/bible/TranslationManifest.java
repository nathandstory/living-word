package com.livingword.bible;

import java.util.List;

public record TranslationManifest(
    String id,
    String displayName,
    String language,
    String license,
    String attribution,
    String textDirection,
    List<String> bookOrder,
    String audioManifestId
) {
    public TranslationManifest {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("translation id is required");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("display name is required");
        }
        bookOrder = List.copyOf(bookOrder == null ? List.of() : bookOrder);
        textDirection = textDirection == null || textDirection.isBlank() ? "ltr" : textDirection;
    }
}
