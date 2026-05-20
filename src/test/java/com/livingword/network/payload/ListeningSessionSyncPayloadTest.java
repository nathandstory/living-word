package com.livingword.network.payload;

import com.livingword.sync.ListeningSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ListeningSessionSyncPayloadTest {
    @Test
    void payloadIncludesAudioManifestFromSession() {
        ListeningSession session = ListeningSession.started("bsb", "john", 3, "souer", 1_000L);

        ListeningSessionSyncPayload payload = ListeningSessionSyncPayload.fromSession(session, 3_000L);

        assertEquals("souer", payload.audioManifestId());
    }
}
