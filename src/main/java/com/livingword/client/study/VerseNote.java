package com.livingword.client.study;

import com.livingword.bible.BibleReference;

import java.util.Objects;

public record VerseNote(BibleReference reference, String text) {
    public VerseNote {
        reference = Objects.requireNonNull(reference, "reference");
        text = text == null ? "" : text.strip();
    }
}
