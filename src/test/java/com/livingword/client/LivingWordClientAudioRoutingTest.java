package com.livingword.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LivingWordClientAudioRoutingTest {
    @Test
    void bibleAudioUsesDedicatedPrivateControllerInsteadOfMainWorldSessionController() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/LivingWordClient.java"));
        int playLocalStart = source.indexOf("public static void playLocalChapter(String translationId, String bookId, int chapter, String audioManifestId, long positionMillis)");
        int previewStart = source.indexOf("public static void previewScriptureDiscChapter", playLocalStart);
        String playLocalMethod = source.substring(playLocalStart, previewStart);

        assertTrue(source.contains("private static ClientAudioSessionController bibleAudioController"));
        assertTrue(playLocalMethod.contains("bibleAudioController().handleSessionSync"));
        assertFalse(playLocalMethod.contains("handleSessionSync(new ListeningSessionSyncPayload"));
        assertFalse(playLocalMethod.contains("controller().handleSessionSync"));
    }
}
