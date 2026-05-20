package com.livingword.client.gui;

import com.livingword.bible.BibleReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BibleGuiState {
    private String translationId;
    private String bookId;
    private int chapter;
    private int selectedVerse;
    private String searchQuery;
    private final List<BibleReference> searchResults = new ArrayList<>();
    private int searchResultIndex;
    private final List<BibleReference> recentHistory = new ArrayList<>();
    private final List<BibleReference> bookmarks = new ArrayList<>();

    private BibleGuiState(String translationId, String bookId, int chapter) {
        this.translationId = translationId;
        this.bookId = bookId;
        this.chapter = chapter;
        this.selectedVerse = 1;
        this.searchQuery = "";
    }

    public static BibleGuiState initial(String translationId, String bookId, int chapter) {
        return new BibleGuiState(translationId, bookId, chapter);
    }

    public String translationId() {
        return translationId;
    }

    public String bookId() {
        return bookId;
    }

    public int chapter() {
        return chapter;
    }

    public int selectedVerse() {
        return selectedVerse;
    }

    public String searchQuery() {
        return searchQuery;
    }

    public List<BibleReference> recentHistory() {
        return List.copyOf(recentHistory);
    }

    public List<BibleReference> bookmarks() {
        return List.copyOf(bookmarks);
    }

    public void selectVerse(int selectedVerse) {
        if (selectedVerse < 1) {
            throw new IllegalArgumentException("selectedVerse must be positive");
        }
        this.selectedVerse = selectedVerse;
    }

    public void setSearchQuery(String searchQuery) {
        String normalized = searchQuery == null ? "" : searchQuery;
        if (!this.searchQuery.equals(normalized)) {
            searchResults.clear();
            searchResultIndex = 0;
        }
        this.searchQuery = normalized;
    }

    public void replaceSearchResults(List<BibleReference> references) {
        searchResults.clear();
        if (references != null) {
            searchResults.addAll(references);
        }
        searchResultIndex = 0;
    }

    public Optional<BibleReference> currentSearchResult() {
        if (searchResults.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(searchResults.get(searchResultIndex));
    }

    public void advanceSearchResult(int direction) {
        if (!searchResults.isEmpty()) {
            searchResultIndex = Math.floorMod(searchResultIndex + direction, searchResults.size());
        }
    }

    public String searchResultSummary() {
        if (searchResults.isEmpty()) {
            return "";
        }
        return (searchResultIndex + 1) + " / " + searchResults.size();
    }

    public void setPassage(String translationId, String bookId, int chapter) {
        if (isBlank(translationId)) {
            throw new IllegalArgumentException("translationId is required");
        }
        if (isBlank(bookId)) {
            throw new IllegalArgumentException("bookId is required");
        }
        if (chapter < 1) {
            throw new IllegalArgumentException("chapter must be positive");
        }
        this.translationId = translationId;
        this.bookId = bookId;
        this.chapter = chapter;
        this.selectedVerse = 1;
    }

    public void addBookmark(BibleReference reference) {
        if (!bookmarks.contains(reference)) {
            bookmarks.add(reference);
        }
    }

    public void toggleBookmark(BibleReference reference) {
        if (bookmarks.contains(reference)) {
            bookmarks.remove(reference);
            return;
        }
        bookmarks.add(reference);
    }

    public boolean isBookmarked(BibleReference reference) {
        return bookmarks.contains(reference);
    }

    public boolean isSelectedVerseBookmarked() {
        return isBookmarked(selectedReference());
    }

    public int bookmarkCount() {
        return bookmarks.size();
    }

    public void replaceBookmarks(List<BibleReference> references) {
        bookmarks.clear();
        if (references != null) {
            for (BibleReference reference : references) {
                addBookmark(reference);
            }
        }
    }

    public void recordHistory(BibleReference reference) {
        recentHistory.remove(reference);
        recentHistory.addFirst(reference);
    }

    public void replaceRecentHistory(List<BibleReference> references) {
        recentHistory.clear();
        if (references != null) {
            for (int index = references.size() - 1; index >= 0; index--) {
                recordHistory(references.get(index));
            }
        }
    }

    public BibleReference selectedReference() {
        return new BibleReference(translationId, bookId, chapter, selectedVerse);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
