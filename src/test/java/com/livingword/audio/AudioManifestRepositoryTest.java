package com.livingword.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AudioManifestRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsManifestFromClasspathResource() throws Exception {
        Path manifestPath = tempDir.resolve("data/livingword/audio/webp/default.json");
        Files.createDirectories(manifestPath.getParent());
        Files.writeString(manifestPath, """
            {
              "id": "webp-default",
              "translationId": "webp",
              "baseUri": "https://cdn.example.test/audio/webp/",
              "chapters": {
                "john:3": "abc123"
              }
            }
            """);
        try (URLClassLoader classLoader = new URLClassLoader(new java.net.URL[]{tempDir.toUri().toURL()})) {
            AudioManifest manifest = new AudioManifestRepository(classLoader)
                .manifestOrFallback("webp", "default", URI.create("https://fallback.example.test/webp/"));

            assertEquals("webp-default", manifest.id());
            assertEquals("abc123", manifest.expectedHash(new AudioChapterId("webp", "john", 3)).orElseThrow());
        }
    }

    @Test
    void createsFallbackManifestWhenResourceIsMissing() {
        AudioManifest manifest = new AudioManifestRepository(getClass().getClassLoader())
            .manifestOrFallback("kjv", "default", URI.create("https://fallback.example.test/kjv/"));

        assertEquals("kjv-default", manifest.id());
        assertEquals(URI.create("https://fallback.example.test/kjv/john/john_003.ogg"), manifest.chapterUri(new AudioChapterId("kjv", "john", 3)));
    }
}
