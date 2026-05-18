package com.livingword.audio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.Map;
import java.util.TreeMap;

public final class VerseTimestampParser {
    public VerseTimestampMap parse(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        Map<Integer, Long> starts = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            starts.put(Integer.parseInt(entry.getKey()), Math.round(entry.getValue().getAsDouble() * 1000.0D));
        }
        return new VerseTimestampMap(starts);
    }
}
