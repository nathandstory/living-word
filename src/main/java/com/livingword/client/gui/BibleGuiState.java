package com.livingword.client.gui;

import com.livingword.bible.BibleReference;

import java.util.ArrayList;
import java.util.List;

public final class BibleGuiState {
    private String translationId;
    private String bookId;
    private int chapter;
    private int selectedVerse;
    private String searchQuery;
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
        this.searchQuery = searchQuery == null ? "" : searchQuery;
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

    public void recordHistory(BibleReference reference) {
        recentHistory.remove(reference);
        recentHistory.addFirst(reference);
    }

    public BibleReference selectedReference() {
        return new BibleReference(translationId, bookId, chapter, selectedVerse);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
