package com.livingword.discs;

import java.util.List;
import java.util.Locale;

public record ScriptureDiscAudioSource(String translationId, String manifestId, String displayName) {
    private static final List<ScriptureDiscAudioSource> SOURCES = List.of(
        new ScriptureDiscAudioSource("bsb", "default", "David"),
        new ScriptureDiscAudioSource("bsb", "hays", "Hays"),
        new ScriptureDiscAudioSource("bsb", "souer", "Souer"),
        new ScriptureDiscAudioSource("kjv", "default", "Default"),
        new ScriptureDiscAudioSource("webp", "default", "Default")
    );

    public ScriptureDiscAudioSource {
        if (translationId == null || translationId.isBlank()) {
            throw new IllegalArgumentException("translationId is required");
        }
        if (manifestId == null || manifestId.isBlank()) {
            throw new IllegalArgumentException("manifestId is required");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName is required");
        }
        translationId = translationId.toLowerCase(Locale.ROOT);
        manifestId = manifestId.toLowerCase(Locale.ROOT);
    }

    public static List<ScriptureDiscAudioSource> forTranslation(String translationId) {
        String normalized = normalizeTranslationId(translationId);
        List<ScriptureDiscAudioSource> matching = SOURCES.stream()
            .filter(source -> source.translationId().equals(normalized))
            .toList();
        return matching.isEmpty() ? List.of(new ScriptureDiscAudioSource(normalized, "default", "Default")) : matching;
    }

    public static ScriptureDiscAudioSource defaultFor(String translationId) {
        return forTranslation(translationId).getFirst();
    }

    public static ScriptureDiscAudioSource byManifestId(String translationId, String manifestId) {
        String normalizedManifestId = manifestId == null || manifestId.isBlank() ? "default" : manifestId.toLowerCase(Locale.ROOT);
        return forTranslation(translationId).stream()
            .filter(source -> source.manifestId().equals(normalizedManifestId))
            .findFirst()
            .orElseGet(() -> defaultFor(translationId));
    }

    public static ScriptureDiscAudioSource cycle(String translationId, String manifestId, int direction) {
        List<ScriptureDiscAudioSource> sources = forTranslation(translationId);
        String normalizedManifestId = manifestId == null || manifestId.isBlank() ? "default" : manifestId.toLowerCase(Locale.ROOT);
        int currentIndex = 0;
        for (int index = 0; index < sources.size(); index++) {
            if (sources.get(index).manifestId().equals(normalizedManifestId)) {
                currentIndex = index;
                break;
            }
        }
        int nextIndex = Math.floorMod(currentIndex + direction, sources.size());
        return sources.get(nextIndex);
    }

    private static String normalizeTranslationId(String translationId) {
        return translationId == null || translationId.isBlank() ? "bsb" : translationId.toLowerCase(Locale.ROOT);
    }
}
