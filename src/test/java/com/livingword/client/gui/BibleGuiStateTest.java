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

    @Test
    void restoresBookmarksAndHistory() {
        BibleGuiState state = BibleGuiState.initial("kjv", "john", 3);
        BibleReference bookmark = new BibleReference("webp", "john", 3, 16);
        BibleReference recent = new BibleReference("kjv", "genesis", 1, 1);

        state.replaceBookmarks(java.util.List.of(bookmark));
        state.replaceRecentHistory(java.util.List.of(recent));

        assertEquals(bookmark, state.bookmarks().getFirst());
        assertEquals(recent, state.recentHistory().getFirst());
    }

    @Test
    void cyclesSearchResults() {
        BibleGuiState state = BibleGuiState.initial("kjv", "john", 3);
        BibleReference first = new BibleReference("kjv", "john", 3, 16);
        BibleReference second = new BibleReference("kjv", "1_john", 4, 8);

        state.replaceSearchResults(java.util.List.of(first, second));

        assertEquals(first, state.currentSearchResult().orElseThrow());
        assertEquals("1 / 2", state.searchResultSummary());

        state.advanceSearchResult(1);

        assertEquals(second, state.currentSearchResult().orElseThrow());
        assertEquals("2 / 2", state.searchResultSummary());
    }

    @Test
    void changingSearchQueryClearsPreviousResults() {
        BibleGuiState state = BibleGuiState.initial("kjv", "john", 3);
        state.replaceSearchResults(java.util.List.of(new BibleReference("kjv", "john", 3, 16)));

        state.setSearchQuery("peace");

        assertTrue(state.currentSearchResult().isEmpty());
        assertEquals("", state.searchResultSummary());
    }
}
