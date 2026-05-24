package com.livingword.client.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void screenSupportsNarratorModeSearchAndPreviewToggle() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/ScriptureDiscSelectionScreen.java"));

        assertTrue(source.contains("ScriptureDiscAudioSource"));
        assertTrue(source.contains("ScriptureDiscPlaybackMode"));
        assertTrue(source.contains("bookSearchBox"));
        assertTrue(source.contains("applyBookSearch"));
        assertTrue(source.contains("togglePreview"));
        assertTrue(source.contains("previewing"));
        assertTrue(source.contains("LivingWordClient.stopScriptureDiscPreview()"));
        assertTrue(source.contains("new ScriptureDiscSelection(translationId, bookId, chapter, audioManifestId, playbackMode)"));
        assertFalse(source.contains("stopPreview()"));
    }

    @Test
    void screenExplainsDiscModeAndReverseClickBehavior() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/ScriptureDiscSelectionScreen.java"));
        String lang = Files.readString(Path.of("src/main/resources/assets/livingword/lang/en_us.json"));

        assertTrue(source.contains("modeDescription"));
        assertTrue(source.contains("gui.livingword.disc.mode.single"));
        assertTrue(source.contains("gui.livingword.disc.mode.continue"));
        assertTrue(source.contains("gui.livingword.disc.mode.loop"));
        assertTrue(source.contains("gui.livingword.disc.reverse_hint"));
        assertTrue(lang.contains("Right-click a row to go backward."));
    }

    @Test
    void explanatoryTextRendersInClearForegroundInstructionPanel() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/ScriptureDiscSelectionScreen.java"));

        assertTrue(source.contains("renderInstructionPanel"));
        assertTrue(source.contains("drawCenteredWrapped"));
        assertTrue(source.indexOf("super.render(graphics, mouseX, mouseY, partialTick);") < source.indexOf("renderInstructionPanel"));
        assertTrue(source.contains("Math.min(320"));
        assertTrue(source.contains("int infoHeight"));
        assertTrue(source.contains("Math.min(infoTop + infoHeight"));
        assertFalse(source.contains("Math.min(356"));
    }

    @Test
    void topLabelsRenderInForegroundWithoutVanillaShadowBlur() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/ScriptureDiscSelectionScreen.java"));

        assertTrue(source.contains("renderTopLabels"));
        assertTrue(source.indexOf("super.render(graphics, mouseX, mouseY, partialTick);") < source.indexOf("renderTopLabels"));
        assertTrue(source.contains("drawCenteredPlain"));
        assertFalse(source.contains("graphics.drawCenteredString(this.font, this.title"));
        assertFalse(source.contains("graphics.drawCenteredString(this.font, trimmed"));
    }
}
