package com.livingword.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioTimingRepositoryTest {
    @TempDir
    private Path tempDir;

    @Test
    void loadsFilesystemTimingBeforeBundledResourceTiming() throws Exception {
        Path bundledRoot = tempDir.resolve("bundled");
        Path bundled = bundledRoot.resolve("data/livingword/audio/test_translation/default/test_book/test_book_003.timestamps.json");
        Files.createDirectories(bundled.getParent());
        Files.writeString(bundled, """
            {
              "verses": { "1": 0.0 },
              "words": {
                "1": [
                  { "text": "bundled", "start": 0.0, "end": 1.0 }
                ]
              }
            }
            """);

        Path cacheRoot = tempDir.resolve("cache");
        Path cached = cacheRoot.resolve("test_translation/default/test_book/test_book_003.timestamps.json");
        Files.createDirectories(cached.getParent());
        Files.writeString(cached, """
            {
              "verses": { "1": 0.0 },
              "words": {
                "1": [
                  { "text": "cached", "start": 0.0, "end": 1.0 }
                ]
              }
            }
            """);

        try (URLClassLoader classLoader = new URLClassLoader(new URL[] { bundledRoot.toUri().toURL() })) {
            Optional<VerseTimestampMap> timings = new AudioTimingRepository(classLoader, cacheRoot)
                .timestamps(new AudioChapterId("test_translation", "test_book", 3), "default");

            assertEquals("cached", timings.orElseThrow().wordAt(1, 500L).orElseThrow().text());
        }
    }

    @Test
    void loadsBundledTimingWhenNoFilesystemTimingExists() throws Exception {
        Path bundledRoot = tempDir.resolve("bundled");
        Path bundled = bundledRoot.resolve("data/livingword/audio/test_translation/default/test_book/test_book_003.timestamps.json");
        Files.createDirectories(bundled.getParent());
        Files.writeString(bundled, """
            {
              "verses": { "1": 0.0 },
              "words": {
                "1": [
                  { "text": "bundled", "start": 0.0, "end": 1.0 }
                ]
              }
            }
            """);

        try (URLClassLoader classLoader = new URLClassLoader(new URL[] { bundledRoot.toUri().toURL() })) {
            Optional<VerseTimestampMap> timings = new AudioTimingRepository(classLoader, tempDir.resolve("cache"))
                .timestamps(new AudioChapterId("test_translation", "test_book", 3), "default");

            assertEquals("bundled", timings.orElseThrow().wordAt(1, 500L).orElseThrow().text());
        }
    }

    @Test
    void includesBundledBsbDavidJohnThreePilotTimings() {
        Optional<VerseTimestampMap> timings = new AudioTimingRepository(getClass().getClassLoader())
            .timestamps(new AudioChapterId("bsb", "john", 3), "default");

        VerseTimestampMap timestampMap = timings.orElseThrow();
        assertTrue(timestampMap.startMillis(1).orElseThrow() > 5_000L);
        assertTrue(timestampMap.startMillis(36).orElseThrow() > timestampMap.startMillis(1).orElseThrow());
        assertTrue(timestampMap.startMillis(36).orElseThrow() < 420_000L);
        assertEquals("For", timestampMap.wordAt(16, timestampMap.startMillis(16).orElseThrow() + 100L).orElseThrow().text());
    }

    @Test
    void includesBundledKjvJohnThreeTimings() {
        Optional<VerseTimestampMap> timings = new AudioTimingRepository(getClass().getClassLoader())
            .timestamps(new AudioChapterId("kjv", "john", 3), "default");

        VerseTimestampMap timestampMap = timings.orElseThrow();
        assertTrue(timestampMap.startMillis(1).orElseThrow() > 1_000L);
        assertTrue(timestampMap.startMillis(36).orElseThrow() > timestampMap.startMillis(1).orElseThrow());
        assertEquals("For", timestampMap.wordAt(16, timestampMap.startMillis(16).orElseThrow() + 100L).orElseThrow().text());
    }

    @Test
    void includesBundledBsbAlternateNarratorTimings() {
        AudioTimingRepository repository = new AudioTimingRepository(getClass().getClassLoader());

        VerseTimestampMap souer = repository
            .timestamps(new AudioChapterId("bsb", "john", 3), "souer")
            .orElseThrow();
        VerseTimestampMap hays = repository
            .timestamps(new AudioChapterId("bsb", "john", 3), "hays")
            .orElseThrow();

        assertTrue(souer.startMillis(16).orElseThrow() > souer.startMillis(1).orElseThrow());
        assertTrue(hays.startMillis(16).orElseThrow() > hays.startMillis(1).orElseThrow());
        assertEquals("For", souer.wordAt(16, souer.startMillis(16).orElseThrow() + 100L).orElseThrow().text());
        assertEquals("For", hays.wordAt(16, hays.startMillis(16).orElseThrow() + 100L).orElseThrow().text());
    }
}
