package com.livingword.audio;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class VerseTimestampParser {
    public VerseTimestampMap parse(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        Map<Integer, Long> starts = new TreeMap<>();
        JsonObject verses = root.has("verses") && root.get("verses").isJsonObject()
            ? root.getAsJsonObject("verses")
            : root;
        for (Map.Entry<String, JsonElement> entry : verses.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                continue;
            }
            starts.put(Integer.parseInt(entry.getKey()), Math.round(entry.getValue().getAsDouble() * 1000.0D));
        }
        return new VerseTimestampMap(starts, parseWords(root));
    }

    private static Map<Integer, List<VerseTimestampMap.TimedWord>> parseWords(JsonObject root) {
        if (!root.has("words") || !root.get("words").isJsonObject()) {
            return Map.of();
        }
        Map<Integer, List<VerseTimestampMap.TimedWord>> wordsByVerse = new TreeMap<>();
        JsonObject words = root.getAsJsonObject("words");
        for (Map.Entry<String, JsonElement> verseEntry : words.entrySet()) {
            if (!verseEntry.getValue().isJsonArray()) {
                continue;
            }
            wordsByVerse.put(Integer.parseInt(verseEntry.getKey()), parseVerseWords(verseEntry.getValue().getAsJsonArray()));
        }
        return wordsByVerse;
    }

    private static List<VerseTimestampMap.TimedWord> parseVerseWords(JsonArray words) {
        List<VerseTimestampMap.TimedWord> parsed = new ArrayList<>();
        for (JsonElement wordElement : words) {
            if (!wordElement.isJsonObject()) {
                continue;
            }
            JsonObject word = wordElement.getAsJsonObject();
            long startMillis = secondsToMillis(numberOrDefault(word, "start", numberOrDefault(word, "time", 0.0D)));
            long endMillis = secondsToMillis(numberOrDefault(word, "end", startMillis / 1000.0D));
            parsed.add(new VerseTimestampMap.TimedWord(stringOrDefault(word, "text", ""), startMillis, endMillis));
        }
        return List.copyOf(parsed);
    }

    private static String stringOrDefault(JsonObject root, String name, String fallback) {
        if (!root.has(name) || root.get(name).isJsonNull()) {
            return fallback;
        }
        return root.get(name).getAsString();
    }

    private static double numberOrDefault(JsonObject root, String name, double fallback) {
        if (!root.has(name) || root.get(name).isJsonNull()) {
            return fallback;
        }
        return root.get(name).getAsDouble();
    }

    private static long secondsToMillis(double seconds) {
        return Math.round(seconds * 1000.0D);
    }
}
