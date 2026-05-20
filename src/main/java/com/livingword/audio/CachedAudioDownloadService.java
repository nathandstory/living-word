package com.livingword.audio;

import com.livingword.LivingWord;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

public final class CachedAudioDownloadService implements AudioDownloadService {
    private final AudioCacheManager cacheManager;
    private final AudioChapterUriResolver chapterUriResolver;
    private final Executor executor;
    private final ConcurrentMap<DownloadRequestKey, CompletableFuture<DownloadState>> inFlight = new ConcurrentHashMap<>();

    public CachedAudioDownloadService(AudioCacheManager cacheManager, Executor executor) {
        this(cacheManager, new DefaultAudioChapterUriResolver(), executor);
    }

    public CachedAudioDownloadService(AudioCacheManager cacheManager, AudioChapterUriResolver chapterUriResolver, Executor executor) {
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager");
        this.chapterUriResolver = Objects.requireNonNull(chapterUriResolver, "chapterUriResolver");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public CompletableFuture<DownloadState> requestChapter(AudioManifest manifest, AudioChapterId chapterId) {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(chapterId, "chapterId");
        String sourceSignature = sourceSignature(manifest);
        if (isUsableCached(chapterId, manifest.fileExtension(), sourceSignature)) {
            return CompletableFuture.completedFuture(DownloadState.cached(chapterId));
        }
        DownloadRequestKey key = new DownloadRequestKey(chapterId, manifest.fileExtension(), sourceSignature);
        CompletableFuture<DownloadState> future = inFlight.computeIfAbsent(key, ignored ->
            CompletableFuture.supplyAsync(() -> downloadChapter(manifest, chapterId, sourceSignature), executor)
        );
        future.whenComplete((state, exception) -> inFlight.remove(key, future));
        return future;
    }

    private boolean isUsableCached(AudioChapterId chapterId, String extension, String sourceSignature) {
        if (!cacheManager.isCached(chapterId, extension)) {
            return false;
        }
        Path markerPath = cacheManager.sourceMarkerPath(chapterId, extension);
        try {
            return Files.isRegularFile(markerPath) && sourceSignature.equals(Files.readString(markerPath));
        } catch (Exception exception) {
            return false;
        }
    }

    private DownloadState downloadChapter(AudioManifest manifest, AudioChapterId chapterId, String sourceSignature) {
        Path temporaryPath = cacheManager.temporaryDownloadPath(chapterId, manifest.fileExtension());
        Path finalPath = cacheManager.chapterAudioPath(chapterId, manifest.fileExtension());
        Path sourceMarkerPath = cacheManager.sourceMarkerPath(chapterId, manifest.fileExtension());
        try {
            Files.createDirectories(finalPath.getParent());
            URI chapterUri = chapterUriResolver.resolve(manifest, chapterId);
            try (InputStream inputStream = chapterUri.toURL().openStream()) {
                Files.copy(inputStream, temporaryPath, StandardCopyOption.REPLACE_EXISTING);
            }
            if (manifest.expectedHash(chapterId).isPresent() && !manifest.expectedHash(chapterId).orElseThrow().equalsIgnoreCase(sha256(temporaryPath))) {
                Files.deleteIfExists(temporaryPath);
                return DownloadState.hashMismatch(chapterId);
            }
            moveIntoCache(temporaryPath, finalPath);
            Files.writeString(sourceMarkerPath, sourceSignature);
            return DownloadState.cached(chapterId);
        } catch (Exception exception) {
            try {
                Files.deleteIfExists(temporaryPath);
            } catch (java.io.IOException ignored) {
                // Best-effort cleanup; the failure state below is the useful signal.
            }
            LivingWord.LOGGER.warn("Living Word audio download failed for {}", chapterId, exception);
            return DownloadState.failed(chapterId, "Unable to download chapter audio. Internet is required for first playback.");
        }
    }

    private static void moveIntoCache(Path temporaryPath, Path finalPath) throws java.io.IOException {
        try {
            Files.move(temporaryPath, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sourceSignature(AudioManifest manifest) {
        return "%s|%s|%s|%s".formatted(
            manifest.id(),
            manifest.baseUri(),
            manifest.fileExtension(),
            manifest.pathStrategy()
        );
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

    private record DownloadRequestKey(AudioChapterId chapterId, String extension, String sourceSignature) {
    }
}
