package com.livingword.network.payload;

import com.livingword.sync.AudioSourcePosition;
import com.livingword.sync.ListeningSession;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ListeningSessionSyncPayloadTest {
    @Test
    void payloadIncludesAudioManifestFromSession() {
        ListeningSession session = ListeningSession.started("bsb", "john", 3, "souer", 1_000L);

        ListeningSessionSyncPayload payload = ListeningSessionSyncPayload.fromSession(session, 3_000L);

        assertEquals("souer", payload.audioManifestId());
    }

    @Test
    void payloadIncludesWorldAudioSourcePositionFromSession() {
        AudioSourcePosition source = new AudioSourcePosition(10.5D, 64.5D, -2.5D);
        ListeningSession session = ListeningSession.started("bsb", "john", 3, "souer", Optional.of(source), 1_000L);

        ListeningSessionSyncPayload payload = ListeningSessionSyncPayload.fromSession(session, 3_000L);

        assertEquals(Optional.of(source), payload.sourcePosition());
    }
}
