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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        private final DownloadState state;
        private AudioChapterId requestedChapter;

        private FakeDownloadService(DownloadState state) {
            this.state = state;
        }

        @Override
        public CompletableFuture<DownloadState> requestChapter(AudioManifest manifest, AudioChapterId chapterId) {
            requestedChapter = chapterId;
            return CompletableFuture.completedFuture(state);
        }
    }

    private static final class FakePlaybackService implements AudioPlaybackService {
        private AudioChapterId playedChapter;
        private long playedPositionMillis;
        private boolean playedSpatial;
        private AudioChapterId seekedChapter;
        private long seekedPositionMillis;

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
        }
    }
}
