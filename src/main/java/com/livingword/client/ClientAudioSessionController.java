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
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.LongSupplier;

public final class ClientAudioSessionController {
    private final Function<String, AudioManifest> manifestProvider;
    private final AudioDownloadService downloadService;
    private final AudioPlaybackService playbackService;
    private final boolean autoplay;
    private final boolean spatial;
    private final long syncToleranceMillis;
    private final LongSupplier clock;

    private UUID activeSessionId;
    private AudioChapterId activeChapter;
    private long lastCommandedPositionMillis;
    private PlaybackState activeState = PlaybackState.STOPPED;
    private long positionAnchorMillis;
    private long clockAnchorMillis;

    public ClientAudioSessionController(
        Function<String, AudioManifest> manifestProvider,
        AudioDownloadService downloadService,
        AudioPlaybackService playbackService,
        boolean autoplay,
        boolean spatial,
        long syncToleranceMillis
    ) {
        this(manifestProvider, downloadService, playbackService, autoplay, spatial, syncToleranceMillis, System::currentTimeMillis);
    }

    ClientAudioSessionController(
        Function<String, AudioManifest> manifestProvider,
        AudioDownloadService downloadService,
        AudioPlaybackService playbackService,
        boolean autoplay,
        boolean spatial,
        long syncToleranceMillis,
        LongSupplier clock
    ) {
        this.manifestProvider = Objects.requireNonNull(manifestProvider, "manifestProvider");
        this.downloadService = Objects.requireNonNull(downloadService, "downloadService");
        this.playbackService = Objects.requireNonNull(playbackService, "playbackService");
        this.autoplay = autoplay;
        this.spatial = spatial;
        this.syncToleranceMillis = Math.max(0L, syncToleranceMillis);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public CompletableFuture<DownloadState> handleSessionSync(ListeningSessionSyncPayload payload) {
        Objects.requireNonNull(payload, "payload");
        AudioChapterId chapterId = new AudioChapterId(payload.translationId(), payload.bookId(), payload.chapter());
        activeSessionId = payload.sessionId();
        activeChapter = chapterId;
        markPosition(payload.positionMillis(), payload.state());

        return switch (payload.state()) {
            case PLAYING -> autoplay
                ? downloadThenPlay(chapterId, payload.positionMillis())
                : CompletableFuture.completedFuture(unavailable(chapterId, "Autoplay is disabled."));
            case PAUSED -> {
                playbackService.pause(chapterId);
                markPosition(payload.positionMillis(), PlaybackState.PAUSED);
                yield CompletableFuture.completedFuture(unavailable(chapterId, "Session is paused."));
            }
            case STOPPED -> {
                playbackService.stop(chapterId);
                markPosition(payload.positionMillis(), PlaybackState.STOPPED);
                yield CompletableFuture.completedFuture(unavailable(chapterId, "Session is stopped."));
            }
        };
    }

    public CompletableFuture<DownloadState> handlePlaybackControl(PlaybackControlPayload payload) {
        Objects.requireNonNull(payload, "payload");
        if (activeSessionId == null || !activeSessionId.equals(payload.sessionId()) || activeChapter == null) {
            return CompletableFuture.completedFuture(unavailable(new AudioChapterId("unknown", "unknown", 1), "Session is not active on this client."));
        }
        markPosition(payload.positionMillis(), payload.state());
        return switch (payload.state()) {
            case PLAYING -> downloadThenPlay(activeChapter, payload.positionMillis());
            case PAUSED -> {
                playbackService.pause(activeChapter);
                markPosition(payload.positionMillis(), PlaybackState.PAUSED);
                yield CompletableFuture.completedFuture(unavailable(activeChapter, "Session is paused."));
            }
            case STOPPED -> {
                playbackService.stop(activeChapter);
                markPosition(payload.positionMillis(), PlaybackState.STOPPED);
                yield CompletableFuture.completedFuture(unavailable(activeChapter, "Session is stopped."));
            }
        };
    }

    public void handleTimestampCorrection(TimestampCorrectionPayload payload) {
        Objects.requireNonNull(payload, "payload");
        if (activeSessionId == null || activeChapter == null || !activeSessionId.equals(payload.sessionId())) {
            return;
        }
        long driftMillis = Math.abs(payload.positionMillis() - currentPositionMillis());
        if (driftMillis > syncToleranceMillis) {
            playbackService.seek(activeChapter, payload.positionMillis());
            markPosition(payload.positionMillis(), PlaybackState.PLAYING);
        }
    }

    private CompletableFuture<DownloadState> downloadThenPlay(AudioChapterId chapterId, long positionMillis) {
        AudioManifest manifest;
        try {
            manifest = manifestProvider.apply(chapterId.translationId());
        } catch (RuntimeException exception) {
            return CompletableFuture.completedFuture(failed(chapterId, exception));
        }

        CompletableFuture<DownloadState> downloadFuture;
        try {
            downloadFuture = downloadService.requestChapter(manifest, chapterId);
        } catch (RuntimeException exception) {
            return CompletableFuture.completedFuture(failed(chapterId, exception));
        }

        return downloadFuture.handle((state, exception) -> {
            if (exception != null) {
                return failed(chapterId, exception);
            }
            if (state.status() == DownloadState.Status.CACHED) {
                try {
                    playbackService.stopAll();
                    playbackService.play(chapterId, positionMillis, spatial);
                    markPosition(positionMillis, PlaybackState.PLAYING);
                } catch (RuntimeException playbackException) {
                    return failed(chapterId, playbackException);
                }
            }
            return state;
        });
    }

    public long pauseActive() {
        long positionMillis = currentPositionMillis();
        if (activeChapter != null) {
            playbackService.pause(activeChapter);
        }
        markPosition(positionMillis, PlaybackState.PAUSED);
        return positionMillis;
    }

    public long stopActive() {
        long positionMillis = currentPositionMillis();
        if (activeChapter != null) {
            playbackService.stop(activeChapter);
        }
        markPosition(positionMillis, PlaybackState.STOPPED);
        activeChapter = null;
        activeSessionId = null;
        return positionMillis;
    }

    public long currentPositionMillis() {
        if (activeState == PlaybackState.PLAYING) {
            return Math.max(0L, positionAnchorMillis + Math.max(0L, clock.getAsLong() - clockAnchorMillis));
        }
        return Math.max(0L, positionAnchorMillis);
    }

    private void markPosition(long positionMillis, PlaybackState state) {
        long clampedPosition = Math.max(0L, positionMillis);
        this.lastCommandedPositionMillis = clampedPosition;
        this.positionAnchorMillis = clampedPosition;
        this.clockAnchorMillis = clock.getAsLong();
        this.activeState = state;
    }

    private static DownloadState failed(AudioChapterId chapterId, Throwable exception) {
        Throwable unwrapped = unwrap(exception);
        String message = unwrapped.getMessage();
        if (message == null || message.isBlank()) {
            message = unwrapped.getClass().getSimpleName();
        }
        return DownloadState.failed(chapterId, message);
    }

    private static Throwable unwrap(Throwable exception) {
        if (exception instanceof CompletionException && exception.getCause() != null) {
            return exception.getCause();
        }
        return exception;
    }

    private static DownloadState unavailable(AudioChapterId chapterId, String message) {
        return new DownloadState(chapterId, DownloadState.Status.UNAVAILABLE, 0.0D, message);
    }
}
