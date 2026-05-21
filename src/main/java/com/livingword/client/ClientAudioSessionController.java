package com.livingword.client;

import com.livingword.audio.AudioChapterId;
import com.livingword.audio.AudioDownloadService;
import com.livingword.audio.AudioManifest;
import com.livingword.audio.AudioPlaybackService;
import com.livingword.audio.DownloadState;
import com.livingword.network.payload.ListeningSessionSyncPayload;
import com.livingword.network.payload.PlaybackControlPayload;
import com.livingword.network.payload.TimestampCorrectionPayload;
import com.livingword.sync.AudioSourcePosition;
import com.livingword.sync.PlaybackState;

import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongSupplier;

public final class ClientAudioSessionController {
    private final BiFunction<String, String, AudioManifest> manifestProvider;
    private final AudioDownloadService downloadService;
    private final AudioPlaybackService playbackService;
    private final boolean autoplay;
    private final boolean spatial;
    private final boolean stopExistingBeforePlayback;
    private final long syncToleranceMillis;
    private final LongSupplier clock;

    private UUID activeSessionId;
    private AudioChapterId activeChapter;
    private String activeAudioManifestId = "default";
    private String activeFileExtension = "ogg";
    private Optional<AudioSourcePosition> activeSourcePosition = Optional.empty();
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
        this((translationId, ignored) -> manifestProvider.apply(translationId), downloadService, playbackService, autoplay, spatial, true, syncToleranceMillis, System::currentTimeMillis);
    }

    public ClientAudioSessionController(
        BiFunction<String, String, AudioManifest> manifestProvider,
        AudioDownloadService downloadService,
        AudioPlaybackService playbackService,
        boolean autoplay,
        boolean spatial,
        long syncToleranceMillis
    ) {
        this(manifestProvider, downloadService, playbackService, autoplay, spatial, true, syncToleranceMillis, System::currentTimeMillis);
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
        this((translationId, ignored) -> manifestProvider.apply(translationId), downloadService, playbackService, autoplay, spatial, true, syncToleranceMillis, clock);
    }

    ClientAudioSessionController(
        BiFunction<String, String, AudioManifest> manifestProvider,
        AudioDownloadService downloadService,
        AudioPlaybackService playbackService,
        boolean autoplay,
        boolean spatial,
        long syncToleranceMillis,
        LongSupplier clock
    ) {
        this(manifestProvider, downloadService, playbackService, autoplay, spatial, true, syncToleranceMillis, clock);
    }

    private ClientAudioSessionController(
        BiFunction<String, String, AudioManifest> manifestProvider,
        AudioDownloadService downloadService,
        AudioPlaybackService playbackService,
        boolean autoplay,
        boolean spatial,
        boolean stopExistingBeforePlayback,
        long syncToleranceMillis,
        LongSupplier clock
    ) {
        this.manifestProvider = Objects.requireNonNull(manifestProvider, "manifestProvider");
        this.downloadService = Objects.requireNonNull(downloadService, "downloadService");
        this.playbackService = Objects.requireNonNull(playbackService, "playbackService");
        this.autoplay = autoplay;
        this.spatial = spatial;
        this.stopExistingBeforePlayback = stopExistingBeforePlayback;
        this.syncToleranceMillis = Math.max(0L, syncToleranceMillis);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static ClientAudioSessionController preview(
        Function<String, AudioManifest> manifestProvider,
        AudioDownloadService downloadService,
        AudioPlaybackService playbackService,
        long syncToleranceMillis
    ) {
        return preview((translationId, ignored) -> manifestProvider.apply(translationId), downloadService, playbackService, syncToleranceMillis);
    }

    public static ClientAudioSessionController preview(
        BiFunction<String, String, AudioManifest> manifestProvider,
        AudioDownloadService downloadService,
        AudioPlaybackService playbackService,
        long syncToleranceMillis
    ) {
        return new ClientAudioSessionController(manifestProvider, downloadService, playbackService, true, false, false, syncToleranceMillis, System::currentTimeMillis);
    }

    public CompletableFuture<DownloadState> handleSessionSync(ListeningSessionSyncPayload payload) {
        Objects.requireNonNull(payload, "payload");
        AudioChapterId chapterId = new AudioChapterId(payload.translationId(), payload.bookId(), payload.chapter());
        activeSessionId = payload.sessionId();
        activeChapter = chapterId;
        activeAudioManifestId = payload.audioManifestId();
        activeSourcePosition = payload.sourcePosition();
        markPosition(payload.positionMillis(), payload.state());

        return switch (payload.state()) {
            case PLAYING -> autoplay
                ? downloadThenPlay(chapterId, payload.positionMillis(), payload.sourcePosition())
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
            case PLAYING -> downloadThenPlay(activeChapter, payload.positionMillis(), activeSourcePosition);
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
            playbackService.seek(activeChapter, payload.positionMillis(), activeFileExtension, activeSourcePosition);
            markPosition(payload.positionMillis(), PlaybackState.PLAYING);
        }
    }

    private CompletableFuture<DownloadState> downloadThenPlay(AudioChapterId chapterId, long positionMillis, Optional<AudioSourcePosition> sourcePosition) {
        AudioManifest manifest;
        try {
            manifest = manifestProvider.apply(chapterId.translationId(), activeAudioManifestId);
            activeFileExtension = manifest.fileExtension();
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
                    if (stopExistingBeforePlayback) {
                        playbackService.stopAll();
                    }
                    playbackService.play(chapterId, positionMillis, spatial || sourcePosition.isPresent(), manifest.fileExtension(), sourcePosition);
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
        activeAudioManifestId = "default";
        activeFileExtension = "ogg";
        activeSourcePosition = Optional.empty();
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
