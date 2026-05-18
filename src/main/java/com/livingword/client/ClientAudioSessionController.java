package com.livingword.client;

import com.livingword.audio.AudioChapterId;
import com.livingword.audio.AudioDownloadService;
import com.livingword.audio.AudioManifest;
import com.livingword.audio.AudioPlaybackService;
import com.livingword.audio.DownloadState;
import com.livingword.network.payload.ListeningSessionSyncPayload;
import com.livingword.network.payload.PlaybackControlPayload;
import com.livingword.network.payload.TimestampCorrectionPayload;
import com.livingword.sync.PlaybackState;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class ClientAudioSessionController {
    private final Function<String, AudioManifest> manifestProvider;
    private final AudioDownloadService downloadService;
    private final AudioPlaybackService playbackService;
    private final boolean autoplay;
    private final boolean spatial;
    private final long syncToleranceMillis;

    private UUID activeSessionId;
    private AudioChapterId activeChapter;
    private long lastCommandedPositionMillis;

    public ClientAudioSessionController(
        Function<String, AudioManifest> manifestProvider,
        AudioDownloadService downloadService,
        AudioPlaybackService playbackService,
        boolean autoplay,
        boolean spatial,
        long syncToleranceMillis
    ) {
        this.manifestProvider = Objects.requireNonNull(manifestProvider, "manifestProvider");
        this.downloadService = Objects.requireNonNull(downloadService, "downloadService");
        this.playbackService = Objects.requireNonNull(playbackService, "playbackService");
        this.autoplay = autoplay;
        this.spatial = spatial;
        this.syncToleranceMillis = Math.max(0L, syncToleranceMillis);
    }

    public CompletableFuture<DownloadState> handleSessionSync(ListeningSessionSyncPayload payload) {
        Objects.requireNonNull(payload, "payload");
        AudioChapterId chapterId = new AudioChapterId(payload.translationId(), payload.bookId(), payload.chapter());
        activeSessionId = payload.sessionId();
        activeChapter = chapterId;
        lastCommandedPositionMillis = payload.positionMillis();

        return switch (payload.state()) {
            case PLAYING -> autoplay
                ? downloadThenPlay(chapterId, payload.positionMillis())
                : CompletableFuture.completedFuture(unavailable(chapterId, "Autoplay is disabled."));
            case PAUSED -> {
                playbackService.pause(chapterId);
                yield CompletableFuture.completedFuture(unavailable(chapterId, "Session is paused."));
            }
            case STOPPED -> {
                playbackService.stop(chapterId);
                yield CompletableFuture.completedFuture(unavailable(chapterId, "Session is stopped."));
            }
        };
    }

    public CompletableFuture<DownloadState> handlePlaybackControl(PlaybackControlPayload payload) {
        Objects.requireNonNull(payload, "payload");
        if (activeSessionId == null || !activeSessionId.equals(payload.sessionId()) || activeChapter == null) {
            return CompletableFuture.completedFuture(unavailable(new AudioChapterId("unknown", "unknown", 1), "Session is not active on this client."));
        }
        lastCommandedPositionMillis = payload.positionMillis();
        return switch (payload.state()) {
            case PLAYING -> downloadThenPlay(activeChapter, payload.positionMillis());
            case PAUSED -> {
                playbackService.pause(activeChapter);
                yield CompletableFuture.completedFuture(unavailable(activeChapter, "Session is paused."));
            }
            case STOPPED -> {
                playbackService.stop(activeChapter);
                yield CompletableFuture.completedFuture(unavailable(activeChapter, "Session is stopped."));
            }
        };
    }

    public void handleTimestampCorrection(TimestampCorrectionPayload payload) {
        Objects.requireNonNull(payload, "payload");
        if (activeSessionId == null || activeChapter == null || !activeSessionId.equals(payload.sessionId())) {
            return;
        }
        long driftMillis = Math.abs(payload.positionMillis() - lastCommandedPositionMillis);
        if (driftMillis > syncToleranceMillis) {
            playbackService.seek(activeChapter, payload.positionMillis());
            lastCommandedPositionMillis = payload.positionMillis();
        }
    }

    private CompletableFuture<DownloadState> downloadThenPlay(AudioChapterId chapterId, long positionMillis) {
        AudioManifest manifest = manifestProvider.apply(chapterId.translationId());
        return downloadService.requestChapter(manifest, chapterId).thenApply(state -> {
            if (state.status() == DownloadState.Status.CACHED) {
                playbackService.play(chapterId, positionMillis, spatial);
                lastCommandedPositionMillis = positionMillis;
            }
            return state;
        });
    }

    private static DownloadState unavailable(AudioChapterId chapterId, String message) {
        return new DownloadState(chapterId, DownloadState.Status.UNAVAILABLE, 0.0D, message);
    }
}
