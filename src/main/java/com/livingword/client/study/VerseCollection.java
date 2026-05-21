package com.livingword.client.study;

import com.livingword.bible.BibleReference;

import java.util.LinkedHashSet;
import java.util.List;

public record VerseCollection(String name, List<BibleReference> references) {
    public VerseCollection {
        name = normalizeName(name);
        references = List.copyOf(new LinkedHashSet<>(references == null ? List.of() : references));
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return "Study List";
        }
        return name.strip();
    }
}
