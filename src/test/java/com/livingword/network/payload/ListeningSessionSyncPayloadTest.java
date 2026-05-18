package com.livingword.network.payload;

import com.livingword.sync.ListeningSession;
import com.livingword.sync.PlaybackState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ListeningSessionSyncPayloadTest {
    @Test
    void syncPayloadIncludesSessionMetadataAndCurrentPosition() {
        UUID participant = UUID.randomUUID();
        ListeningSession session = ListeningSession
            .started("webp", "john", 3, 1_000L)
            .withParticipant(participant);

        ListeningSessionSyncPayload payload = ListeningSessionSyncPayload.fromSession(session, 6_500L);

        assertEquals(session.id(), payload.sessionId());
        assertEquals("webp", payload.translationId());
        assertEquals("john", payload.bookId());
        assertEquals(3, payload.chapter());
        assertEquals(PlaybackState.PLAYING, payload.state());
        assertEquals(5_500L, payload.positionMillis());
        assertEquals(6_500L, payload.serverTimeMillis());
        assertEquals(1, payload.participantCount());
    }
}
