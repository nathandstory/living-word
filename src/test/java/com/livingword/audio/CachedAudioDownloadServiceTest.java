package com.livingword.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void concurrentRequestsForSameChapterShareOneDownload() throws Exception {
        Path source = tempDir.resolve("source.mp3");
        Files.writeString(source, "fake mp3 bytes");
        AudioChapterId chapterId = new AudioChapterId("kjv", "john", 3);
        AudioManifest manifest = new AudioManifest("kjv-audiotreasure", "kjv", tempDir.toUri(), "mp3", "direct", Map.of(), Map.of());
        AudioCacheManager cache = new AudioCacheManager(tempDir.resolve("cache"));
        AtomicInteger resolverCalls = new AtomicInteger();
        CachedAudioDownloadService service = new CachedAudioDownloadService(
            cache,
            (ignoredManifest, ignoredChapter) -> {
                resolverCalls.incrementAndGet();
                try {
                    Thread.sleep(150L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new java.io.IOException("interrupted", exception);
                }
                return source.toUri();
            },
            Executors.newFixedThreadPool(2)
        );

        CompletableFuture<DownloadState> first = service.requestChapter(manifest, chapterId);
        CompletableFuture<DownloadState> second = service.requestChapter(manifest, chapterId);

        assertEquals(DownloadState.Status.CACHED, first.get().status());
        assertEquals(DownloadState.Status.CACHED, second.get().status());
        assertEquals(1, resolverCalls.get());
        assertTrue(Files.exists(cache.chapterAudioPath(chapterId, "mp3")));
    }

    @Test
    void cachedAudioWithoutMatchingSourceMarkerIsRedownloaded() throws Exception {
        Path sourceRoot = tempDir.resolve("source");
        Files.createDirectories(sourceRoot.resolve("john"));
        Files.writeString(sourceRoot.resolve("john").resolve("john_003.mp3"), "new source");
        AudioChapterId chapterId = new AudioChapterId("kjv", "john", 3);
        AudioManifest manifest = new AudioManifest("kjv-audiotreasure", "kjv", sourceRoot.toUri(), "mp3", "direct", Map.of(), Map.of());
        AudioCacheManager cache = new AudioCacheManager(tempDir.resolve("cache"));
        Files.createDirectories(cache.chapterAudioPath(chapterId, "mp3").getParent());
        Files.writeString(cache.chapterAudioPath(chapterId, "mp3"), "old source");
        CachedAudioDownloadService service = new CachedAudioDownloadService(cache, Executors.newSingleThreadExecutor());

        DownloadState state = service.requestChapter(manifest, chapterId).get();

        assertEquals(DownloadState.Status.CACHED, state.status());
        assertEquals("new source", Files.readString(cache.chapterAudioPath(chapterId, "mp3")));
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
