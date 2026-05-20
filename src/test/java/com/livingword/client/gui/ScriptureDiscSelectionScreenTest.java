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
    }
}
