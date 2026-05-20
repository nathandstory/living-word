package com.livingword.client.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScriptureDiscSelectionScreenTest {
    @Test
    void rightClickCyclesSelectionButtonsBackward() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/ScriptureDiscSelectionScreen.java"));

        assertTrue(source.contains("button == 1"));
        assertTrue(source.contains("navigateTranslation(-1)"));
        assertTrue(source.contains("navigateBook(-1)"));
        assertTrue(source.contains("navigateChapter(-1)"));
        assertTrue(source.contains("navigateSource(-1)"));
        assertTrue(source.contains("navigateMode(-1)"));
    }

    @Test
    void screenSupportsNarratorModeSearchAndPreviewControls() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/ScriptureDiscSelectionScreen.java"));

        assertTrue(source.contains("ScriptureDiscAudioSource"));
        assertTrue(source.contains("ScriptureDiscPlaybackMode"));
        assertTrue(source.contains("bookSearchBox"));
        assertTrue(source.contains("applyBookSearch"));
        assertTrue(source.contains("previewSelection"));
        assertTrue(source.contains("stopPreview"));
        assertTrue(source.contains("LivingWordClient.stopLocalPlayback()"));
        assertTrue(source.contains("new ScriptureDiscSelection(translationId, bookId, chapter, audioManifestId, playbackMode)"));
    }
}
