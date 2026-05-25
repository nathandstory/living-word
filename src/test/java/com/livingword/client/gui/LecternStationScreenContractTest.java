package com.livingword.client.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LecternStationScreenContractTest {
    @Test
    void lecternScreenUsesStationPlaybackControlsWithoutPreviewControls() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/LecternStationScreen.java"));

        assertTrue(source.contains("configureLecternStation"));
        assertTrue(source.contains("LecternStationAction.SAVE"));
        assertTrue(source.contains("LecternStationAction.PLAY"));
        assertTrue(source.contains("LecternStationAction.PAUSE"));
        assertTrue(source.contains("LecternStationAction.RESET"));
        assertTrue(source.contains("LecternStationAction.TOGGLE_DISPLAY"));
        assertTrue(source.contains("togglePlayback"));
        assertTrue(source.contains("resetStation"));
        assertTrue(source.contains("toggleFloatingVerse"));
        assertTrue(source.contains("AudioTimingRepository"));
        assertTrue(source.contains("verseTimingAvailable"));
        assertTrue(source.contains("displayToggleButton.active"));
        assertTrue(source.contains("gui.livingword.lectern.reset"));
        assertTrue(source.contains("gui.livingword.lectern.display_unavailable"));
        assertTrue(source.contains("reverseClick"));
        assertFalse(source.contains("toggleListening"));
        assertFalse(source.contains("gui.livingword.lectern.start"));
        assertFalse(source.contains("gui.livingword.lectern.stop"));
        assertFalse(source.contains("previewScriptureDiscChapter"));
        assertFalse(source.contains("stopScriptureDiscPreview"));
    }
}
