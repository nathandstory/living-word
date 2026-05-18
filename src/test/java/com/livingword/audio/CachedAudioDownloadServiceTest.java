package com.livingword.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CachedAudioDownloadServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void downloadsChapterIntoCacheAndValidatesHash() throws Exception {
        Path sourceRoot = tempDir.resolve("source");
        Path sourceBook = sourceRoot.resolve("john");
        Files.createDirectories(sourceBook);
        byte[] audioBytes = "fake ogg bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(sourceBook.resolve("john_003.ogg"), audioBytes);
        AudioChapterId chapterId = new AudioChapterId("webp", "john", 3);
        AudioManifest manifest = new AudioManifest("webp-default", "webp", sourceRoot.toUri(), Map.of(chapterId, sha256(audioBytes)));
        AudioCacheManager cache = new AudioCacheManager(tempDir.resolve("cache"));
        CachedAudioDownloadService service = new CachedAudioDownloadService(cache, Executors.newSingleThreadExecutor());

        DownloadState state = service.requestChapter(manifest, chapterId).get();

        assertEquals(DownloadState.Status.CACHED, state.status());
        assertTrue(Files.exists(cache.chapterAudioPath(chapterId)));
        assertEquals(audioBytes.length, Files.size(cache.chapterAudioPath(chapterId)));
    }

    @Test
    void hashMismatchDoesNotLeaveCachedAudio() throws Exception {
        Path sourceRoot = tempDir.resolve("source");
        Path sourceBook = sourceRoot.resolve("john");
        Files.createDirectories(sourceBook);
        Files.writeString(sourceBook.resolve("john_003.ogg"), "wrong bytes");
        AudioChapterId chapterId = new AudioChapterId("webp", "john", 3);
        AudioManifest manifest = new AudioManifest("webp-default", "webp", sourceRoot.toUri(), Map.of(chapterId, "not-the-hash"));
        AudioCacheManager cache = new AudioCacheManager(tempDir.resolve("cache"));
        CachedAudioDownloadService service = new CachedAudioDownloadService(cache, Executors.newSingleThreadExecutor());

        DownloadState state = service.requestChapter(manifest, chapterId).get();

        assertEquals(DownloadState.Status.HASH_MISMATCH, state.status());
        assertTrue(Files.notExists(cache.chapterAudioPath(chapterId)));
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
