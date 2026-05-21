package com.livingword.client;

import com.livingword.bible.BibleReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BibleClientPreferencesTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsReaderStateAsStableReferences() {
        Path stateFile = tempDir.resolve("livingword/bible_state.json");
        BibleClientPreferences.StoredBibleState state = new BibleClientPreferences.StoredBibleState(
            Optional.of(new BibleReference("webp", "john", 3, 16)),
            List.of(new BibleReference("kjv", "psalms", 23, 1)),
            List.of(new BibleReference("webp", "john", 3, 16)),
            List.of(new BibleReference("bsb", "romans", 8, 1))
        );

        BibleClientPreferences.save(stateFile, state);
        BibleClientPreferences.StoredBibleState loaded = BibleClientPreferences.load(stateFile);

        assertEquals(state, loaded);
    }

    @Test
    void missingStateFileLoadsEmptyState() {
        BibleClientPreferences.StoredBibleState loaded = BibleClientPreferences.load(tempDir.resolve("missing.json"));

        assertTrue(loaded.bookmarks().isEmpty());
        assertTrue(loaded.highlights().isEmpty());
        assertTrue(loaded.recentHistory().isEmpty());
        assertTrue(loaded.lastReference().isEmpty());
    }
}
