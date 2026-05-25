package com.livingword.audio;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VerseTimestampParserTest {
    @Test
    void parsesVerseStartTimesAndFindsActiveVerse() {
        VerseTimestampMap timestamps = new VerseTimestampParser().parse(new StringReader("""
            {
              "1": 0.0,
              "2": 14.2,
              "3": 28.6
            }
            """));

        assertEquals(14_200L, timestamps.startMillis(2).orElseThrow());
        assertEquals(1, timestamps.verseAt(10_000L).orElseThrow());
        assertEquals(2, timestamps.verseAt(20_000L).orElseThrow());
        assertEquals(3, timestamps.verseAt(40_000L).orElseThrow());
    }

    @Test
    void parsesNestedVerseAndWordTimingsForExactHighlighting() {
        VerseTimestampMap timestamps = new VerseTimestampParser().parse(new StringReader("""
            {
              "verses": {
                "1": 0.0,
                "2": 5.0
              },
              "words": {
                "1": [
                  { "text": "For", "start": 0.0, "end": 0.35 },
                  { "text": "God", "start": 0.35, "end": 0.8 },
                  { "text": "so", "start": 0.8, "end": 1.0 }
                ]
              }
            }
            """));

        assertEquals(5_000L, timestamps.startMillis(2).orElseThrow());
        assertEquals("God", timestamps.wordAt(1, 500L).orElseThrow().text());
        assertEquals(1, timestamps.wordIndexAt(1, 500L).orElseThrow());
    }
}
