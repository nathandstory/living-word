package com.livingword.bible;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public final class BibleDataManager {
    private final Map<String, TranslationManifest> translations = new HashMap<>();
    private final Map<String, ChapterData> chapters = new HashMap<>();

    public void registerTranslation(TranslationManifest manifest) {
        translations.put(manifest.id(), manifest);
    }

    public void registerChapter(ChapterData chapter) {
        chapters.put(chapterKey(chapter.translationId(), chapter.bookId(), chapter.chapter()), chapter);
    }

    public Optional<TranslationManifest> getTranslation(String translationId) {
        return Optional.ofNullable(translations.get(translationId));
    }

    public List<TranslationManifest> translations() {
        List<TranslationManifest> ordered = new ArrayList<>(translations.values());
        ordered.sort(Comparator.comparing(TranslationManifest::displayName));
        return List.copyOf(ordered);
    }

    public Optional<ChapterData> getChapter(String translationId, String bookId, int chapter) {
        return Optional.ofNullable(chapters.get(chapterKey(translationId, bookId, chapter)));
    }

    public Optional<String> getVerse(BibleReference reference) {
        return getChapter(reference.translationId(), reference.bookId(), reference.chapter())
            .map(chapter -> chapter.verseText(reference.verse()))
            .filter(text -> !text.isEmpty());
    }

    public List<String> bookIds(String translationId) {
        Set<String> loadedBooks = new HashSet<>();
        for (ChapterData chapter : chapters.values()) {
            if (chapter.translationId().equals(translationId)) {
                loadedBooks.add(chapter.bookId());
            }
        }
        List<String> ordered = new ArrayList<>();
        getTranslation(translationId).ifPresent(manifest -> {
            for (String bookId : manifest.bookOrder()) {
                if (loadedBooks.remove(bookId)) {
                    ordered.add(bookId);
                }
            }
        });
        List<String> remaining = new ArrayList<>(loadedBooks);
        remaining.sort(String::compareTo);
        ordered.addAll(remaining);
        return List.copyOf(ordered);
    }

    public List<Integer> chapters(String translationId, String bookId) {
        List<Integer> ordered = new ArrayList<>();
        for (ChapterData chapter : chapters.values()) {
            if (chapter.translationId().equals(translationId) && chapter.bookId().equals(bookId)) {
                ordered.add(chapter.chapter());
            }
        }
        ordered.sort(Integer::compareTo);
        return List.copyOf(ordered);
    }

    public Optional<ChapterData> firstChapter(String translationId) {
        for (String bookId : bookIds(translationId)) {
            List<Integer> chapterNumbers = chapters(translationId, bookId);
            if (!chapterNumbers.isEmpty()) {
                return getChapter(translationId, bookId, chapterNumbers.getFirst());
            }
        }
        return Optional.empty();
    }

    public List<BibleReference> search(String translationId, String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }
        String normalizedQuery = query.toLowerCase(java.util.Locale.ROOT);
        List<BibleReference> results = new ArrayList<>();
        for (String bookId : bookIds(translationId)) {
            for (int chapterNumber : chapters(translationId, bookId)) {
                ChapterData chapter = getChapter(translationId, bookId, chapterNumber).orElseThrow();
                for (Map.Entry<Integer, String> verse : new TreeMap<>(chapter.verses()).entrySet()) {
                    if (verse.getValue().toLowerCase(java.util.Locale.ROOT).contains(normalizedQuery)) {
                        results.add(new BibleReference(translationId, bookId, chapterNumber, verse.getKey()));
                        if (results.size() >= limit) {
                            return List.copyOf(results);
                        }
                    }
                }
            }
        }
        return List.copyOf(results);
    }

    private static String chapterKey(String translationId, String bookId, int chapter) {
        return translationId + ':' + bookId + ':' + chapter;
    }
}
