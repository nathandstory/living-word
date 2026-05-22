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

    @Test
    void screenHasHighlightedVerseViewAndHighlightAction() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));

        assertTrue(source.contains("highlightedView"));
        assertTrue(source.contains("highlightButton"));
        assertTrue(source.contains("renderHighlightedVerses"));
        assertTrue(source.contains("state.toggleHighlight"));
        assertTrue(source.contains("state.highlights()"));
    }

    @Test
    void screenShowsSearchResultsAsClickableListWithoutStudyToolViews() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));

        assertTrue(source.contains("searchResultsView"));
        assertTrue(source.contains("renderSearchResults"));
        assertTrue(source.contains("searchResultAt"));
        assertTrue(source.contains("state.searchResults()"));
        assertTrue(source.contains("returnToReading"));
        assertTrue(source.contains("gui.livingword.bible.back_to_reading"));
        assertFalse(source.contains("renderNotes"));
        assertFalse(source.contains("renderCollections"));
        assertFalse(source.contains("previousAudioButton"));
    }

    @Test
    void copyAndSearchActionsHavePlayerVisibleFeedback() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));

        assertTrue(source.contains("statusLine"));
        assertTrue(source.contains("setStatus"));
        assertTrue(source.contains("message.livingword.bible.copied"));
        assertTrue(source.contains("message.livingword.bible.search_results"));
        assertTrue(source.contains("message.livingword.bible.search_none"));
    }

    @Test
    void listenButtonReflectsWhetherCurrentChapterIsPlaying() throws Exception {
        String screenSource = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));
        String clientSource = Files.readString(Path.of("src/main/java/com/livingword/client/LivingWordClient.java"));

        assertTrue(screenSource.contains("gui.livingword.bible.stop_listen"));
        assertTrue(screenSource.contains("LivingWordClient.isLocalBibleChapterActive"));
        assertTrue(clientSource.contains("public static boolean isLocalBibleChapterActive"));
    }

    @Test
    void highlightedTabUsesObviousFramedHighlightRows() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));

        assertTrue(source.contains("LIST_HIGHLIGHT_FILL"));
        assertTrue(source.contains("LIST_HIGHLIGHT_BORDER"));
        assertTrue(source.contains("renderListHighlightFrame"));
        assertTrue(source.contains("graphics.fill(left, top, left + 4"));
        assertFalse(source.contains("0x55E7B844"));
    }

    @Test
    void searchHasPreviousNavigationAndObviousActiveResultTreatment() throws Exception {
        String screenSource = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));
        String layoutSource = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreenLayout.java"));
        String lang = Files.readString(Path.of("src/main/resources/assets/livingword/lang/en_us.json"));

        assertTrue(screenSource.contains("searchPreviousButton"));
        assertTrue(screenSource.contains("jumpToPreviousSearchResult"));
        assertTrue(screenSource.contains("gui.livingword.bible.search_previous"));
        assertTrue(layoutSource.contains("searchPrevious"));
        assertTrue(lang.contains("gui.livingword.bible.search_previous"));
        assertTrue(screenSource.contains("renderListHighlightFrame(graphics"));
        assertTrue(screenSource.contains("drawSearchResultText"));
    }

    @Test
    void searchNavigationScrollsSelectedVerseIntoView() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/livingword/client/gui/BibleScreen.java"));

        assertTrue(source.contains("scrollSelectedVerseIntoView"));
        assertTrue(source.contains("verseList.scrollOffsetForVerse"));
        assertTrue(source.contains("pendingSelectedVerseScroll"));
        assertTrue(source.contains("this.font == null"));
    }
}
