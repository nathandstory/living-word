package com.livingword.client.gui;

import com.livingword.bible.BibleReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BibleGuiStateTest {
    @Test
    void tracksNavigationSearchBookmarksAndHistory() {
        BibleGuiState state = BibleGuiState.initial("kjv", "john", 3);
        BibleReference reference = new BibleReference("kjv", "john", 3, 16);

        state.selectVerse(16);
        state.setSearchQuery("love");
        state.addBookmark(reference);
        state.recordHistory(reference);

        assertEquals(reference, state.selectedReference());
        assertEquals("love", state.searchQuery());
        assertTrue(state.bookmarks().contains(reference));
        assertEquals(reference, state.recentHistory().getFirst());
    }

    @Test
    void changingPassageResetsVerseSelection() {
        BibleGuiState state = BibleGuiState.initial("kjv", "john", 3);

        state.selectVerse(16);
        state.setPassage("web", "psalms", 23);

        assertEquals(new BibleReference("web", "psalms", 23, 1), state.selectedReference());
    }
}
