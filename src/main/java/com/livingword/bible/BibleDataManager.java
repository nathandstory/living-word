package com.livingword.bible;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BibleDataManager {
    private final Map<String, TranslationManifest> translations = new HashMap<>();
    private final Map<String, ChapterData> chapters = new HashMap<>();

    public void registerTranslation(TranslationManifest manifest) {
        translations.put(manifest.id(), manifest);
    }

    public void registerChapter(ChapterData chapter) {
        chapters.put(chapterKey(chapter.translationId(), chapter.bookId(), chapter.chapter()), chapter);
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

    private static String chapterKey(String translationId, String bookId, int chapter) {
        return translationId + ':' + bookId + ':' + chapter;
    }
}
