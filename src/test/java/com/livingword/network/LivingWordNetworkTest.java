package com.livingword.network;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LivingWordNetworkTest {
    @Test
    void serverPeriodicallySendsTimestampCorrectionsToListeningParticipants() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/network/LivingWordNetwork.java"));

        assertTrue(source.contains("ServerTickEvent.Post"));
        assertTrue(source.contains("SYNC_CORRECTION_INTERVAL_TICKS"));
        assertTrue(source.contains("TimestampCorrectionPayload"));
        assertTrue(source.contains("sendTimestampCorrection"));
    }

    @Test
    void serverHandlesClientChapterFinishedPayloadForDiscSequencing() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/network/LivingWordNetwork.java"));

        assertTrue(source.contains("ChapterFinishedPayload.TYPE"));
        assertTrue(source.contains("handleChapterFinished"));
        assertTrue(source.contains("completeJukeboxChapter"));
    }
}
