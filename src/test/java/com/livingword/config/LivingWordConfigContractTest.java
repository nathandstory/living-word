package com.livingword.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LivingWordConfigContractTest {
    @Test
    void clientConfigControlsSearchResultLimitAndAudioStatusMessages() throws Exception {
        String configSource = Files.readString(Path.of("src/main/java/com/livingword/config/LivingWordConfig.java"));
        String screenSource = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));
        String clientSource = Files.readString(Path.of("src/main/java/com/livingword/client/LivingWordClient.java"));

        assertTrue(configSource.contains("SEARCH_RESULT_LIMIT"));
        assertTrue(configSource.contains("AUDIO_STATUS_MESSAGES"));
        assertTrue(configSource.contains("defineInRange(\"searchResultLimit\", 500, 10, 1000)"));
        assertTrue(screenSource.contains("LivingWordConfig.SEARCH_RESULT_LIMIT.get()"));
        assertTrue(screenSource.contains("Math.max(500, LivingWordConfig.SEARCH_RESULT_LIMIT.get())"));
        assertTrue(clientSource.contains("LivingWordConfig.AUDIO_STATUS_MESSAGES.get()"));
    }
}
