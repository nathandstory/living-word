package com.livingword.bible;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TranslationManifestTest {
    @Test
    void storesTranslationMetadataWithoutKjvAssumptions() {
        TranslationManifest manifest = new TranslationManifest(
            "web",
            "World English Bible",
            "en_us",
            "Public Domain",
            "World English Bible contributors",
            "ltr",
            List.of("genesis", "john"),
            "web-default"
        );

        assertEquals("web", manifest.id());
        assertEquals("World English Bible", manifest.displayName());
        assertTrue(manifest.bookOrder().contains("john"));
    }
}
