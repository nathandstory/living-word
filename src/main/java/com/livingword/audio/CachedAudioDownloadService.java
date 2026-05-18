package com.livingword.audio;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class CachedAudioDownloadService implements AudioDownloadService {
    private final AudioCacheManager cacheManager;
    private final Executor executor;

    public CachedAudioDownloadService(AudioCacheManager cacheManager, Executor executor) {
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public CompletableFuture<DownloadState> requestChapter(AudioManifest manifest, AudioChapterId chapterId) {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(chapterId, "chapterId");
        if (cacheManager.isCached(chapterId)) {
            return CompletableFuture.completedFuture(DownloadState.cached(chapterId));
        }
        return CompletableFuture.supplyAsync(() -> downloadChapter(manifest, chapterId), executor);
    }

    private DownloadState downloadChapter(AudioManifest manifest, AudioChapterId chapterId) {
        Path temporaryPath = cacheManager.temporaryDownloadPath(chapterId);
        Path finalPath = cacheManager.chapterAudioPath(chapterId);
        try {
            Files.createDirectories(finalPath.getParent());
            URI chapterUri = manifest.chapterUri(chapterId);
            try (InputStream inputStream = chapterUri.toURL().openStream()) {
                Files.copy(inputStream, temporaryPath, StandardCopyOption.REPLACE_EXISTING);
            }
            if (manifest.expectedHash(chapterId).isPresent() && !manifest.expectedHash(chapterId).orElseThrow().equalsIgnoreCase(sha256(temporaryPath))) {
                Files.deleteIfExists(temporaryPath);
                return DownloadState.hashMismatch(chapterId);
            }
            Files.move(temporaryPath, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return DownloadState.cached(chapterId);
        } catch (Exception exception) {
            try {
                Files.deleteIfExists(temporaryPath);
            } catch (java.io.IOException ignored) {
                // Best-effort cleanup; the failure state below is the useful signal.
            }
            return DownloadState.failed(chapterId, exception.getMessage());
        }
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
