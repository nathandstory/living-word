package com.livingword.client;

import com.livingword.audio.AudioChapterId;
import com.livingword.audio.AudioDownloadService;
import com.livingword.audio.AudioManifest;
import com.livingword.audio.AudioPlaybackService;
import com.livingword.audio.DownloadState;
import com.livingword.network.payload.ListeningSessionSyncPayload;
import com.livingword.network.payload.TimestampCorrectionPayload;
import com.livingword.sync.PlaybackState;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientAudioSessionControllerTest {
    @Test
    void playingSyncDownloadsChapterThenStartsPlaybackAtServerPosition() {
        AudioManifest manifest = manifest("webp");
        FakeDownloadService downloader = new FakeDownloadService(DownloadState.cached(new AudioChapterId("webp", "john", 3)));
        FakePlaybackService playback = new FakePlaybackService();
        ClientAudioSessionController controller = new ClientAudioSessionController(
            translationId -> manifest,
            downloader,
            playback,
            true,
            true,
            250L
        );

        controller.handleSessionSync(syncPayload(PlaybackState.PLAYING, 28_600L)).join();

        assertEquals(new AudioChapterId("webp", "john", 3), downloader.requestedChapter);
        assertEquals(new AudioChapterId("webp", "john", 3), playback.playedChapter);
        assertEquals(28_600L, playback.playedPositionMillis);
        assertEquals(true, playback.playedSpatial);
    }

    @Test
    void failedDownloadDoesNotStartPlayback() {
        FakeDownloadService downloader = new FakeDownloadService(DownloadState.hashMismatch(new AudioChapterId("webp", "john", 3)));
        FakePlaybackService playback = new FakePlaybackService();
        ClientAudioSessionController controller = new ClientAudioSessionController(
            ClientAudioSessionControllerTest::manifest,
            downloader,
            playback,
            true,
            false,
            250L
        );

        controller.handleSessionSync(syncPayload(PlaybackState.PLAYING, 10_000L)).join();

        assertNull(playback.playedChapter);
    }

    @Test
    void startingPlaybackStopsAnyPreviousLocalChapter() {
        FakePlaybackService playback = new FakePlaybackService();
        ClientAudioSessionController controller = new ClientAudioSessionController(
            ClientAudioSessionControllerTest::manifest,
            new FakeDownloadService(DownloadState.cached(new AudioChapterId("webp", "john", 3))),
            playback,
            true,
            false,
            250L
        );

        controller.handleSessionSync(syncPayload(PlaybackState.PLAYING, 0L)).join();

        assertEquals(1, playback.stopAllCount);
        assertEquals(new AudioChapterId("webp", "john", 3), playback.playedChapter);
    }

    @Test
    void stopActiveStopsCurrentChapter() {
        FakePlaybackService playback = new FakePlaybackService();
        AtomicLong clock = new AtomicLong(1_000L);
        ClientAudioSessionController controller = new ClientAudioSessionController(
            ClientAudioSessionControllerTest::manifest,
            new FakeDownloadService(DownloadState.cached(new AudioChapterId("webp", "john", 3))),
            playback,
            true,
            false,
            250L,
            clock::get
        );
        controller.handleSessionSync(syncPayload(PlaybackState.PLAYING, 0L)).join();
        clock.set(3_500L);

        long stoppedPositionMillis = controller.stopActive();

        assertEquals(new AudioChapterId("webp", "john", 3), playback.stoppedChapter);
        assertEquals(2_500L, stoppedPositionMillis);
    }

    @Test
    void currentPositionAdvancesWhilePlayingAndFreezesWhenPaused() {
        AtomicLong clock = new AtomicLong(10_000L);
        ClientAudioSessionController controller = new ClientAudioSessionController(
            ClientAudioSessionControllerTest::manifest,
            new FakeDownloadService(DownloadState.cached(new AudioChapterId("webp", "john", 3))),
            new FakePlaybackService(),
            true,
            false,
            250L,
            clock::get
        );

        controller.handleSessionSync(syncPayload(PlaybackState.PLAYING, 4_000L)).join();
        clock.set(12_250L);

        assertEquals(6_250L, controller.currentPositionMillis());
        assertEquals(6_250L, controller.pauseActive());
        clock.set(15_000L);
        assertEquals(6_250L, controller.currentPositionMillis());
    }

    @Test
    void manifestFailuresReturnFailedStateInsteadOfThrowingFromListenButton() {
        FakePlaybackService playback = new FakePlaybackService();
        ClientAudioSessionController controller = new ClientAudioSessionController(
            translationId -> {
                throw new IllegalStateException("manifest unavailable");
            },
            new FakeDownloadService(DownloadState.cached(new AudioChapterId("webp", "john", 3))),
            playback,
            true,
            false,
            250L
        );

        DownloadState state = controller.handleSessionSync(syncPayload(PlaybackState.PLAYING, 0L)).join();

        assertEquals(DownloadState.Status.FAILED, state.status());
        assertTrue(state.message().contains("manifest unavailable"));
        assertNull(playback.playedChapter);
    }

    @Test
    void downloaderFailuresReturnFailedStateInsteadOfCrashingListenButton() {
        FakeDownloadService downloader = new FakeDownloadService(
            CompletableFuture.failedFuture(new IllegalStateException("download exploded"))
        );
        FakePlaybackService playback = new FakePlaybackService();
        ClientAudioSessionController controller = new ClientAudioSessionController(
            ClientAudioSessionControllerTest::manifest,
            downloader,
            playback,
            true,
            false,
            250L
        );

        DownloadState state = controller.handleSessionSync(syncPayload(PlaybackState.PLAYING, 0L)).join();

        assertEquals(DownloadState.Status.FAILED, state.status());
        assertTrue(state.message().contains("download exploded"));
        assertNull(playback.playedChapter);
    }

    @Test
    void timestampCorrectionSeeksWhenOutsideTolerance() {
        UUID sessionId = UUID.randomUUID();
        FakePlaybackService playback = new FakePlaybackService();
        ClientAudioSessionController controller = new ClientAudioSessionController(
            ClientAudioSessionControllerTest::manifest,
            new FakeDownloadService(DownloadState.cached(new AudioChapterId("webp", "john", 3))),
            playback,
            true,
            false,
            250L
        );
        controller.handleSessionSync(syncPayload(sessionId, PlaybackState.PLAYING, 1_000L)).join();

        controller.handleTimestampCorrection(new TimestampCorrectionPayload(sessionId, 1_400L, 20_000L));

        assertEquals(new AudioChapterId("webp", "john", 3), playback.seekedChapter);
        assertEquals(1_400L, playback.seekedPositionMillis);
    }

    private static ListeningSessionSyncPayload syncPayload(PlaybackState state, long positionMillis) {
        return syncPayload(UUID.randomUUID(), state, positionMillis);
    }

    private static ListeningSessionSyncPayload syncPayload(UUID sessionId, PlaybackState state, long positionMillis) {
        return new ListeningSessionSyncPayload(sessionId, "webp", "john", 3, state, positionMillis, 15_000L, 2);
    }

    private static AudioManifest manifest(String translationId) {
        return new AudioManifest(translationId + "-default", translationId, URI.create("https://cdn.example.test/" + translationId + "/"), Map.of());
    }

    private static final class FakeDownloadService implements AudioDownloadService {
        private final CompletableFuture<DownloadState> future;
        private AudioChapterId requestedChapter;

        private FakeDownloadService(DownloadState state) {
            this(CompletableFuture.completedFuture(state));
        }

        private FakeDownloadService(CompletableFuture<DownloadState> future) {
            this.future = future;
        }

        @Override
        public CompletableFuture<DownloadState> requestChapter(AudioManifest manifest, AudioChapterId chapterId) {
            requestedChapter = chapterId;
            return future;
        }
    }

    private static final class FakePlaybackService implements AudioPlaybackService {
        private AudioChapterId playedChapter;
        private long playedPositionMillis;
        private boolean playedSpatial;
        private AudioChapterId seekedChapter;
        private long seekedPositionMillis;
        private AudioChapterId stoppedChapter;
        private int stopAllCount;

        @Override
        public void play(AudioChapterId chapterId, long positionMillis, boolean spatial) {
            playedChapter = chapterId;
            playedPositionMillis = positionMillis;
            playedSpatial = spatial;
        }

        @Override
        public void pause(AudioChapterId chapterId) {
        }

        @Override
        public void seek(AudioChapterId chapterId, long positionMillis) {
            seekedChapter = chapterId;
            seekedPositionMillis = positionMillis;
        }

        @Override
        public void stop(AudioChapterId chapterId) {
            stoppedChapter = chapterId;
        }

        @Override
        public void stopAll() {
            stopAllCount++;
        }
    }
}
