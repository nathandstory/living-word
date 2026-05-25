package com.livingword.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        try (URLClassLoader classLoader = new URLClassLoader(new java.net.URL[]{tempDir.toUri().toURL()}, null)) {
            AudioManifest manifest = new AudioManifestRepository(classLoader)
                .manifestOrFallback("webp", "default", URI.create("https://fallback.example.test/webp/"));

            assertEquals("webp-default", manifest.id());
            assertEquals("abc123", manifest.expectedHash(new AudioChapterId("webp", "john", 3)).orElseThrow());
        }
    }

    @Test
    void createsFallbackManifestWhenResourceIsMissing() {
        AudioManifest manifest = new AudioManifestRepository(getClass().getClassLoader())
            .manifestOrFallback("missing", "default", URI.create("https://fallback.example.test/missing/"));

        assertEquals("missing-default", manifest.id());
        assertEquals(URI.create("https://fallback.example.test/missing/john/john_003.ogg"), manifest.chapterUri(new AudioChapterId("missing", "john", 3)));
    }

    @Test
    void includesBundledWebpHumanAudioManifest() {
        AudioManifest manifest = new AudioManifestRepository(getClass().getClassLoader())
            .find("webp", "default")
            .orElseThrow();

        assertEquals("webp-david-williams", manifest.id());
        assertEquals("mp3", manifest.fileExtension());
        assertEquals("public-domain-audio-bibles", manifest.pathStrategy());
        assertTrue(!manifest.verseTimings());
        assertTrue(manifest.baseUri().toString().startsWith("https://publicdomainaudiobibles.com/content/mp3/WEBD/"));
    }

    @Test
    void includesBundledKjvHumanAudioFallbackManifest() {
        AudioManifest manifest = new AudioManifestRepository(getClass().getClassLoader())
            .find("kjv", "default")
            .orElseThrow();

        assertEquals("kjv-audiotreasure-voice", manifest.id());
        assertEquals("kjv", manifest.translationId());
        assertEquals("mp3", manifest.fileExtension());
        assertEquals("audiotreasure-kjv", manifest.pathStrategy());
    }

    @Test
    void includesBundledBsbDavidAudioManifest() {
        AudioManifest manifest = new AudioManifestRepository(getClass().getClassLoader())
            .find("bsb", "default")
            .orElseThrow();

        assertEquals("bsb-helloao-david", manifest.id());
        assertEquals("bsb", manifest.translationId());
        assertEquals("mp3", manifest.fileExtension());
        assertEquals("helloao-bsb-david", manifest.pathStrategy());
        assertTrue(manifest.verseTimings());
        assertEquals(URI.create("https://audio.bible.helloao.org/api/BSB/"), manifest.baseUri());
    }

    @Test
    void includesBundledBsbAlternateNarratorManifests() {
        AudioManifest hays = new AudioManifestRepository(getClass().getClassLoader())
            .find("bsb", "hays")
            .orElseThrow();
        AudioManifest souer = new AudioManifestRepository(getClass().getClassLoader())
            .find("bsb", "souer")
            .orElseThrow();

        assertEquals("bsb-helloao-hays", hays.id());
        assertEquals("helloao-bsb-hays", hays.pathStrategy());
        assertEquals("bsb-helloao-souer", souer.id());
        assertEquals("helloao-bsb-souer", souer.pathStrategy());
    }
}
