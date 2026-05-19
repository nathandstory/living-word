package com.livingword.client.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BibleScreenRenderContractTest {
    @Test
    void renderDoesNotCallSuperRenderAfterDrawingBiblePage() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));

        assertFalse(source.contains("super.render(graphics, mouseX, mouseY, partialTick);"));
        assertTrue(source.contains("renderBibleWidgets(graphics, mouseX, mouseY, partialTick);"));
    }
}
