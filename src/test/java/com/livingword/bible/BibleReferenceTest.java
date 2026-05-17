package com.livingword.bible;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BibleReferenceTest {
    @Test
    void serializesStableTranslationScopedReference() {
        BibleReference reference = new BibleReference("kjv", "john", 3, 16);
        assertEquals("kjv:john:3:16", reference.toStableId());
    }

    @Test
    void rejectsInvalidChapterOrVerse() {
        assertThrows(IllegalArgumentException.class, () -> new BibleReference("kjv", "john", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new BibleReference("kjv", "john", 3, 0));
    }
}
