package com.livingword.client.gui;

import com.livingword.bible.BibleReference;
import com.livingword.client.study.AudioQueueEntry;
import com.livingword.client.study.VerseCollection;
import com.livingword.client.study.VerseNote;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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
    void togglesBookmarksAndReportsCurrentSelection() {
        BibleGuiState state = BibleGuiState.initial("bsb", "john", 3);
        BibleReference reference = new BibleReference("bsb", "john", 3, 16);

        state.selectVerse(16);

        assertTrue(!state.isBookmarked(reference));
        assertTrue(!state.isSelectedVerseBookmarked());

        state.toggleBookmark(reference);

        assertTrue(state.isBookmarked(reference));
        assertTrue(state.isSelectedVerseBookmarked());
        assertEquals(1, state.bookmarkCount());

        state.toggleBookmark(reference);

        assertTrue(!state.isBookmarked(reference));
        assertEquals(0, state.bookmarkCount());
    }

    @Test
    void togglesHighlightsSeparatelyFromBookmarks() {
        BibleGuiState state = BibleGuiState.initial("bsb", "john", 3);
        BibleReference reference = new BibleReference("bsb", "john", 3, 16);

        state.selectVerse(16);

        assertTrue(!state.isHighlighted(reference));
        assertTrue(!state.isSelectedVerseHighlighted());

        state.toggleHighlight(reference);

        assertTrue(state.isHighlighted(reference));
        assertTrue(state.isSelectedVerseHighlighted());
        assertEquals(1, state.highlightCount());
        assertTrue(state.bookmarks().isEmpty());

        state.toggleHighlight(reference);

        assertTrue(!state.isHighlighted(reference));
        assertEquals(0, state.highlightCount());
    }

    @Test
    void restoresHighlightsWithoutDuplicates() {
        BibleGuiState state = BibleGuiState.initial("kjv", "john", 3);
        BibleReference highlight = new BibleReference("webp", "john", 3, 16);

        state.replaceHighlights(java.util.List.of(highlight, highlight));

        assertEquals(java.util.List.of(highlight), state.highlights());
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

    @Test
    void storesVerseNotesAndRemovesBlankNotes() {
        BibleGuiState state = BibleGuiState.initial("kjv", "john", 3);
        BibleReference reference = new BibleReference("kjv", "john", 3, 16);

        state.setNote(reference, "For memorization");

        assertEquals(Optional.of("For memorization"), state.noteFor(reference));
        assertEquals(List.of(new VerseNote(reference, "For memorization")), state.notes());

        state.setNote(reference, "   ");

        assertTrue(state.noteFor(reference).isEmpty());
        assertTrue(state.notes().isEmpty());
    }

    @Test
    void managesNamedVerseCollectionsWithoutDuplicates() {
        BibleGuiState state = BibleGuiState.initial("kjv", "john", 3);
        BibleReference reference = new BibleReference("kjv", "john", 3, 16);

        state.addToCollection("Comfort", reference);
        state.addToCollection("Comfort", reference);

        assertEquals(List.of(new VerseCollection("Comfort", List.of(reference))), state.collections());

        state.removeFromCollection("Comfort", reference);

        assertTrue(state.collections().isEmpty());
    }

    @Test
    void tracksReaderViewForSearchNotesCollectionsAndHighlights() {
        BibleGuiState state = BibleGuiState.initial("kjv", "john", 3);

        assertEquals(BibleGuiState.ReaderView.READING, state.readerView());

        state.showSearchResults();
        assertEquals(BibleGuiState.ReaderView.SEARCH, state.readerView());

        state.showHighlighted();
        assertEquals(BibleGuiState.ReaderView.HIGHLIGHTED, state.readerView());

        state.showNotes();
        assertEquals(BibleGuiState.ReaderView.NOTES, state.readerView());

        state.showCollections();
        assertEquals(BibleGuiState.ReaderView.COLLECTIONS, state.readerView());

        state.showReading();
        assertEquals(BibleGuiState.ReaderView.READING, state.readerView());
    }

    @Test
    void queuesLocalAudioChaptersForPlaybackControls() {
        BibleGuiState state = BibleGuiState.initial("kjv", "john", 3);
        AudioQueueEntry first = new AudioQueueEntry("kjv", "john", 3);
        AudioQueueEntry second = new AudioQueueEntry("kjv", "john", 4);

        state.replaceAudioQueue(List.of(first, second));

        assertEquals(Optional.of(first), state.currentQueuedChapter());
        assertEquals("1 / 2", state.audioQueueSummary());

        state.advanceAudioQueue(1);

        assertEquals(Optional.of(second), state.currentQueuedChapter());
        assertEquals("2 / 2", state.audioQueueSummary());

        state.advanceAudioQueue(1);

        assertEquals(Optional.of(first), state.currentQueuedChapter());
    }
}
