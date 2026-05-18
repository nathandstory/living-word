package com.livingword.bible;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BibleReferenceTest {
    @Test
    void serializesStableTranslationScopedReference() {
        BibleReference reference = new BibleReference("kjv", "john", 3, 16);
        assertEquals("kjv:john:3:16", reference.toStableId());
    }

    @Test
    void parsesStableTranslationScopedReference() {
        Optional<BibleReference> reference = BibleReference.parseStableId("webp:john:3:16");

        assertEquals(new BibleReference("webp", "john", 3, 16), reference.orElseThrow());
        assertTrue(BibleReference.parseStableId("bad").isEmpty());
    }

    @Test
    void rejectsInvalidChapterOrVerse() {
        assertThrows(IllegalArgumentException.class, () -> new BibleReference("kjv", "john", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new BibleReference("kjv", "john", 3, 0));
    }
}
