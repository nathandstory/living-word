package com.livingword.discs;

import com.livingword.bible.BibleDataManager;
import com.livingword.bible.ChapterData;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ScriptureDiscPlaybackSequencerTest {
    @Test
    void singleChapterDoesNotAdvanceAfterCompletion() {
        ScriptureDiscSelection selection = new ScriptureDiscSelection("bsb", "john", 1, "default", ScriptureDiscPlaybackMode.SINGLE_CHAPTER);

        assertEquals(Optional.empty(), ScriptureDiscPlaybackSequencer.nextSelection(bible(), selection));
    }

    @Test
    void loopChapterRepeatsTheSameSelection() {
        ScriptureDiscSelection selection = new ScriptureDiscSelection("bsb", "john", 1, "default", ScriptureDiscPlaybackMode.LOOP_CHAPTER);

        assertEquals(Optional.of(selection), ScriptureDiscPlaybackSequencer.nextSelection(bible(), selection));
    }

    @Test
    void continueBookAdvancesToNextChapterInSameBook() {
        ScriptureDiscSelection selection = new ScriptureDiscSelection("bsb", "john", 1, "hays", ScriptureDiscPlaybackMode.CONTINUE_BOOK);

        assertEquals(
            Optional.of(new ScriptureDiscSelection("bsb", "john", 2, "hays", ScriptureDiscPlaybackMode.CONTINUE_BOOK)),
            ScriptureDiscPlaybackSequencer.nextSelection(bible(), selection)
        );
    }

    @Test
    void continueBookStopsAtEndOfBook() {
        ScriptureDiscSelection selection = new ScriptureDiscSelection("bsb", "john", 2, "hays", ScriptureDiscPlaybackMode.CONTINUE_BOOK);

        assertEquals(Optional.empty(), ScriptureDiscPlaybackSequencer.nextSelection(bible(), selection));
    }

    private static BibleDataManager bible() {
        BibleDataManager manager = new BibleDataManager();
        manager.registerChapter(new ChapterData("bsb", "john", 1, Map.of(1, "In the beginning.")));
        manager.registerChapter(new ChapterData("bsb", "john", 2, Map.of(1, "On the third day.")));
        manager.registerChapter(new ChapterData("bsb", "romans", 1, Map.of(1, "Paul.")));
        return manager;
    }
}
