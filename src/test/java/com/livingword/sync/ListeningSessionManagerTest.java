package com.livingword.sync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ListeningSessionManagerTest {
    @Test
    void controlAppliesClientRequestedPausePlayAndSeek() {
        ListeningSessionManager manager = new ListeningSessionManager();
        ListeningSession session = manager.create("kjv", "psalms", 23, 1_000L);

        manager.control(session.id(), PlaybackState.PAUSED, 2_400L, 5_000L);
        ListeningSession paused = manager.get(session.id()).orElseThrow();
        assertEquals(PlaybackState.PAUSED, paused.state());
        assertEquals(2_400L, paused.positionMillisAt(20_000L));

        manager.control(session.id(), PlaybackState.PLAYING, 2_400L, 7_000L);
        ListeningSession playing = manager.get(session.id()).orElseThrow();
        assertEquals(PlaybackState.PLAYING, playing.state());
        assertEquals(4_400L, playing.positionMillisAt(9_000L));
    }

    @Test
    void stopControlPreservesRequestedStopPosition() {
        ListeningSessionManager manager = new ListeningSessionManager();
        ListeningSession session = manager.create("kjv", "john", 3, 1_000L);

        manager.control(session.id(), PlaybackState.STOPPED, 12_000L, 20_000L);

        ListeningSession stopped = manager.get(session.id()).orElseThrow();
        assertEquals(PlaybackState.STOPPED, stopped.state());
        assertEquals(12_000L, stopped.positionMillisAt(40_000L));
    }
}
