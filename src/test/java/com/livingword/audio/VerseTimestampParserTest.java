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
}
