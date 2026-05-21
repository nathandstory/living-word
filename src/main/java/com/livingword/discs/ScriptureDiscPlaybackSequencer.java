package com.livingword.discs;

import com.livingword.bible.BibleDataManager;

import java.util.List;
import java.util.Optional;

public final class ScriptureDiscPlaybackSequencer {
    private ScriptureDiscPlaybackSequencer() {
    }

    public static Optional<ScriptureDiscSelection> nextSelection(BibleDataManager bible, ScriptureDiscSelection current) {
        return switch (current.playbackMode()) {
            case SINGLE_CHAPTER -> Optional.empty();
            case LOOP_CHAPTER -> Optional.of(current);
            case CONTINUE_BOOK -> nextChapterInBook(bible, current);
        };
    }

    private static Optional<ScriptureDiscSelection> nextChapterInBook(BibleDataManager bible, ScriptureDiscSelection current) {
        List<Integer> chapters = bible.chapters(current.translationId(), current.bookId());
        int currentIndex = chapters.indexOf(current.chapter());
        if (currentIndex < 0 || currentIndex + 1 >= chapters.size()) {
            return Optional.empty();
        }
        return Optional.of(new ScriptureDiscSelection(
            current.translationId(),
            current.bookId(),
            chapters.get(currentIndex + 1),
            current.audioManifestId(),
            current.playbackMode()
        ));
    }
}
