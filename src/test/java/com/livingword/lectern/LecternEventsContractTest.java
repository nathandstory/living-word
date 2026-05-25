package com.livingword.lectern;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LecternEventsContractTest {
    @Test
    void lecternMessagesExplainSetupAndSneakStartMechanic() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/lectern/LecternEvents.java"));
        String lang = Files.readString(Path.of("src/main/resources/assets/livingword/lang/en_us.json"));

        assertTrue(source.contains("formatReference"));
        assertTrue(source.contains("LecternBlock.tryPlaceBook"));
        assertTrue(source.contains("OpenLecternStationPayload"));
        assertTrue(source.contains("startPositionedListeningSession"));
        assertTrue(source.contains("completeLecternChapter"));
        assertTrue(source.contains("resetStation"));
        assertTrue(source.contains("withResumePosition(0L)"));
        assertTrue(source.contains("hasUsableVerseTiming"));
        assertTrue(source.contains("return List.of();"));
        assertTrue(source.contains("message.livingword.lectern.station_ready"));
        assertTrue(source.contains("message.livingword.lectern.session_started"));
        assertTrue(source.contains("message.livingword.lectern.session_reset"));
        assertTrue(lang.contains("Bible placed on lectern"));
        assertTrue(lang.contains("Lectern listening started"));
        assertTrue(lang.contains("Lectern reset"));
        assertTrue(lang.contains("Sneak empty-hand use removes the Bible"));
    }

    @Test
    void lecternPlaybackPausesOtherWorldSourceSessionsForThePlayer() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/lectern/LecternEvents.java"));

        assertTrue(source.contains("ScriptureDiscEvents.pauseSessionsForParticipant(player);"));
        assertTrue(source.contains("pauseSessionsForParticipant(ServerPlayer player)"));
        assertTrue(source.contains("LivingWordNetwork.stopListeningSession(snapshot.sessionId())"));
        assertTrue(source.contains("syncDisplayEntities(level, paused, resumePosition)"));
    }
}
