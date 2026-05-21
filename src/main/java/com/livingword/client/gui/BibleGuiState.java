package com.livingword.client.gui;

import com.livingword.bible.BibleReference;
import com.livingword.client.study.AudioQueueEntry;
import com.livingword.client.study.VerseCollection;
import com.livingword.client.study.VerseNote;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BibleGuiState {
    public enum ReaderView {
        READING,
        SEARCH,
        HIGHLIGHTED,
        NOTES,
        COLLECTIONS
    }

    private String translationId;
    private String bookId;
    private int chapter;
    private int selectedVerse;
    private String searchQuery;
    private ReaderView readerView = ReaderView.READING;
    private final List<BibleReference> searchResults = new ArrayList<>();
    private int searchResultIndex;
    private final List<BibleReference> recentHistory = new ArrayList<>();
    private final List<BibleReference> bookmarks = new ArrayList<>();
    private final List<BibleReference> highlights = new ArrayList<>();
    private final List<VerseNote> notes = new ArrayList<>();
    private final List<VerseCollection> collections = new ArrayList<>();
    private final List<AudioQueueEntry> audioQueue = new ArrayList<>();
    private int audioQueueIndex;

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

    public ReaderView readerView() {
        return readerView;
    }

    public List<BibleReference> searchResults() {
        return List.copyOf(searchResults);
    }

    public List<BibleReference> recentHistory() {
        return List.copyOf(recentHistory);
    }

    public List<BibleReference> bookmarks() {
        return List.copyOf(bookmarks);
    }

    public List<BibleReference> highlights() {
        return List.copyOf(highlights);
    }

    public List<VerseNote> notes() {
        return List.copyOf(notes);
    }

    public List<VerseCollection> collections() {
        return List.copyOf(collections);
    }

    public List<AudioQueueEntry> audioQueue() {
        return List.copyOf(audioQueue);
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

    public void showReading() {
        readerView = ReaderView.READING;
    }

    public void showSearchResults() {
        readerView = ReaderView.SEARCH;
    }

    public void showHighlighted() {
        readerView = ReaderView.HIGHLIGHTED;
    }

    public void showNotes() {
        readerView = ReaderView.NOTES;
    }

    public void showCollections() {
        readerView = ReaderView.COLLECTIONS;
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

    public void addHighlight(BibleReference reference) {
        if (!highlights.contains(reference)) {
            highlights.add(reference);
        }
    }

    public void toggleHighlight(BibleReference reference) {
        if (highlights.contains(reference)) {
            highlights.remove(reference);
            return;
        }
        highlights.add(reference);
    }

    public boolean isHighlighted(BibleReference reference) {
        return highlights.contains(reference);
    }

    public boolean isSelectedVerseHighlighted() {
        return isHighlighted(selectedReference());
    }

    public int highlightCount() {
        return highlights.size();
    }

    public void replaceHighlights(List<BibleReference> references) {
        highlights.clear();
        if (references != null) {
            for (BibleReference reference : references) {
                addHighlight(reference);
            }
        }
    }

    public Optional<String> noteFor(BibleReference reference) {
        return notes.stream()
            .filter(note -> note.reference().equals(reference))
            .map(VerseNote::text)
            .findFirst();
    }

    public void setNote(BibleReference reference, String text) {
        notes.removeIf(note -> note.reference().equals(reference));
        if (text != null && !text.isBlank()) {
            notes.add(new VerseNote(reference, text));
        }
    }

    public void replaceNotes(List<VerseNote> storedNotes) {
        notes.clear();
        if (storedNotes != null) {
            for (VerseNote note : storedNotes) {
                if (note != null && !note.text().isBlank()) {
                    setNote(note.reference(), note.text());
                }
            }
        }
    }

    public void addToCollection(String name, BibleReference reference) {
        String normalizedName = normalizeCollectionName(name);
        List<BibleReference> references = new ArrayList<>();
        boolean found = false;
        for (VerseCollection collection : collections) {
            if (collection.name().equals(normalizedName)) {
                references.addAll(collection.references());
                found = true;
                break;
            }
        }
        if (!references.contains(reference)) {
            references.add(reference);
        }
        replaceCollection(normalizedName, references);
        if (!found) {
            collections.sort(java.util.Comparator.comparing(VerseCollection::name));
        }
    }

    public void removeFromCollection(String name, BibleReference reference) {
        String normalizedName = normalizeCollectionName(name);
        Optional<VerseCollection> existing = collections.stream()
            .filter(collection -> collection.name().equals(normalizedName))
            .findFirst();
        if (existing.isEmpty()) {
            return;
        }
        List<BibleReference> references = new ArrayList<>(existing.orElseThrow().references());
        references.remove(reference);
        replaceCollection(normalizedName, references);
    }

    public void replaceCollections(List<VerseCollection> storedCollections) {
        collections.clear();
        if (storedCollections == null) {
            return;
        }
        Map<String, List<BibleReference>> merged = new LinkedHashMap<>();
        for (VerseCollection collection : storedCollections) {
            if (collection == null) {
                continue;
            }
            List<BibleReference> references = merged.computeIfAbsent(collection.name(), ignored -> new ArrayList<>());
            for (BibleReference reference : collection.references()) {
                if (!references.contains(reference)) {
                    references.add(reference);
                }
            }
        }
        merged.forEach(this::replaceCollection);
    }

    public void replaceAudioQueue(List<AudioQueueEntry> entries) {
        audioQueue.clear();
        if (entries != null) {
            for (AudioQueueEntry entry : entries) {
                if (entry != null && !audioQueue.contains(entry)) {
                    audioQueue.add(entry);
                }
            }
        }
        audioQueueIndex = 0;
    }

    public Optional<AudioQueueEntry> currentQueuedChapter() {
        if (audioQueue.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(audioQueue.get(audioQueueIndex));
    }

    public void advanceAudioQueue(int direction) {
        if (!audioQueue.isEmpty()) {
            audioQueueIndex = Math.floorMod(audioQueueIndex + direction, audioQueue.size());
        }
    }

    public String audioQueueSummary() {
        if (audioQueue.isEmpty()) {
            return "";
        }
        return (audioQueueIndex + 1) + " / " + audioQueue.size();
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

    private void replaceCollection(String name, List<BibleReference> references) {
        collections.removeIf(collection -> collection.name().equals(name));
        if (references != null && !references.isEmpty()) {
            collections.add(new VerseCollection(name, references));
        }
    }

    private static String normalizeCollectionName(String name) {
        if (name == null || name.isBlank()) {
            return "Study List";
        }
        return name.strip();
    }
}
