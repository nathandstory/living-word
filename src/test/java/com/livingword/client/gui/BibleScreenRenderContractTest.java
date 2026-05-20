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

    @Test
    void renderDoesNotDrawBookmarkCountIntoHeadingStatusRow() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));

        assertFalse(source.contains("bookmarkSummary()"));
        assertFalse(source.contains("\"Saved: \""));
        assertTrue(source.contains("state.searchResultSummary()"));
    }

    @Test
    void screenDoesNotExposeBookmarkAsPrimaryActionButton() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));

        assertFalse(source.contains("bookmarkButton"));
        assertFalse(source.contains("toggleBookmark"));
        assertFalse(source.contains("gui.livingword.bible.bookmark"));
    }

    @Test
    void searchAndToolRowsAreToggleableInsteadOfAlwaysRendered() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));

        assertTrue(source.contains("searchExpanded"));
        assertTrue(source.contains("toolsExpanded"));
        assertTrue(source.contains("setSearchControlsVisible"));
        assertTrue(source.contains("setToolControlsVisible"));
    }

    @Test
    void passageHeadingIsRenderedAsSeparateBookAndTranslationLines() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));

        assertTrue(source.contains("renderPassageHeading"));
        assertTrue(source.contains("currentBookChapterHeading()"));
        assertTrue(source.contains("currentTranslationHeading()"));
        assertFalse(source.contains("currentHeading()"));
    }
}
