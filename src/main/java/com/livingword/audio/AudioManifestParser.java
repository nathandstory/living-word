package com.livingword.audio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public final class AudioManifestParser {
    public AudioManifest parse(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        String translationId = requiredString(root, "translationId");
        Map<AudioChapterId, String> hashes = new HashMap<>();
        Map<AudioChapterId, String> chapterPaths = new HashMap<>();
        if (root.has("chapters") && root.get("chapters").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("chapters").entrySet()) {
                hashes.put(parseChapterKey(translationId, entry.getKey()), entry.getValue().getAsString());
            }
        }
        if (root.has("chapterPaths") && root.get("chapterPaths").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("chapterPaths").entrySet()) {
                chapterPaths.put(parseChapterKey(translationId, entry.getKey()), entry.getValue().getAsString());
            }
        }
        return new AudioManifest(
            requiredString(root, "id"),
            translationId,
            URI.create(requiredString(root, "baseUri")),
            optionalString(root, "fileExtension", "ogg"),
            optionalString(root, "pathStrategy", "direct"),
            hashes,
            chapterPaths,
            optionalBoolean(root, "verseTimings", false)
        );
    }

    private static AudioChapterId parseChapterKey(String translationId, String key) {
        String[] parts = key.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid audio chapter key: " + key);
        }
        return new AudioChapterId(translationId, parts[0], Integer.parseInt(parts[1]));
    }

    private static String requiredString(JsonObject root, String name) {
        if (!root.has(name) || root.get(name).isJsonNull()) {
            throw new IllegalArgumentException("Missing required string: " + name);
        }
        return root.get(name).getAsString();
    }

    private static String optionalString(JsonObject root, String name, String fallback) {
        if (!root.has(name) || root.get(name).isJsonNull()) {
            return fallback;
        }
        return root.get(name).getAsString();
    }

    private static boolean optionalBoolean(JsonObject root, String name, boolean fallback) {
        if (!root.has(name) || root.get(name).isJsonNull()) {
            return fallback;
        }
        return root.get(name).getAsBoolean();
    }
}
