package com.livingword.bible;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class BibleJsonParser {
    public TranslationManifest parseManifest(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        return new TranslationManifest(
            requiredString(root, "id"),
            requiredString(root, "displayName"),
            optionalString(root, "language"),
            optionalString(root, "license"),
            optionalString(root, "attribution"),
            optionalString(root, "textDirection"),
            stringList(root, "bookOrder"),
            optionalString(root, "audioManifestId")
        );
    }

    public BibleTranslationIndex parseIndex(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        JsonObject booksObject = requiredObject(root, "books");
        Map<String, List<Integer>> books = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : booksObject.entrySet()) {
            List<Integer> chapters = new ArrayList<>();
            for (JsonElement chapter : entry.getValue().getAsJsonArray()) {
                chapters.add(chapter.getAsInt());
            }
            books.put(entry.getKey(), chapters);
        }
        return new BibleTranslationIndex(requiredString(root, "translationId"), books);
    }

    public List<String> parseTranslationRegistry(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        return stringList(root, "translations");
    }

    public ChapterData parseChapter(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        JsonObject versesObject = requiredObject(root, "verses");
        Map<Integer, String> verses = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : versesObject.entrySet()) {
            verses.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString());
        }
        return new ChapterData(
            requiredString(root, "translationId"),
            requiredString(root, "bookId"),
            root.get("chapter").getAsInt(),
            verses
        );
    }

    private static List<String> stringList(JsonObject root, String name) {
        if (!root.has(name) || root.get(name).isJsonNull()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray(name)) {
            values.add(element.getAsString());
        }
        return values;
    }

    private static String requiredString(JsonObject root, String name) {
        if (!root.has(name) || root.get(name).isJsonNull()) {
            throw new IllegalArgumentException("Missing required string: " + name);
        }
        return root.get(name).getAsString();
    }

    private static String optionalString(JsonObject root, String name) {
        if (!root.has(name) || root.get(name).isJsonNull()) {
            return "";
        }
        return root.get(name).getAsString();
    }

    private static JsonObject requiredObject(JsonObject root, String name) {
        if (!root.has(name) || root.get(name).isJsonNull()) {
            throw new IllegalArgumentException("Missing required object: " + name);
        }
        return root.getAsJsonObject(name);
    }
}
