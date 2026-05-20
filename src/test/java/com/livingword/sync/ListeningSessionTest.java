package com.livingword.sync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ListeningSessionTest {
    @Test
    void playingSessionAdvancesFromStartTime() {
        ListeningSession session = ListeningSession.started("kjv", "john", 3, 1_000L);
        assertEquals(5_000L, session.positionMillisAt(6_000L));
    }

    @Test
    void startedSessionUsesDefaultAudioManifestWhenNotSpecified() {
        ListeningSession session = ListeningSession.started("kjv", "john", 3, 1_000L);

        assertEquals("default", session.audioManifestId());
    }

    @Test
    void startedSessionCanCarryDiscAudioManifest() {
        ListeningSession session = ListeningSession.started("bsb", "john", 3, "hays", 1_000L);

        assertEquals("hays", session.audioManifestId());
    }

    @Test
    void pausedSessionKeepsPausedPosition() {
        ListeningSession session = ListeningSession.started("kjv", "john", 3, 1_000L).pauseAt(6_000L);
        assertEquals(5_000L, session.positionMillisAt(20_000L));
    }

    @Test
    void stoppedSessionKeepsStopPosition() {
        ListeningSession session = ListeningSession.started("kjv", "john", 3, 1_000L).stopAt(6_000L);

        assertEquals(5_000L, session.positionMillisAt(20_000L));
    }
}
