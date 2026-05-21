package com.livingword.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.livingword.bible.BibleReference;
import com.livingword.client.study.VerseCollection;
import com.livingword.client.study.VerseNote;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class BibleClientPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private BibleClientPreferences() {
    }

    public static StoredBibleState load(Path path) {
        if (!Files.exists(path)) {
            return StoredBibleState.empty();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            StoredBibleStateJson json = GSON.fromJson(reader, StoredBibleStateJson.class);
            if (json == null) {
                return StoredBibleState.empty();
            }
            return new StoredBibleState(
                BibleReference.parseStableId(json.lastReference),
                parseReferences(json.bookmarks),
                parseReferences(json.recentHistory),
                parseReferences(json.highlights),
                parseNotes(json.notes),
                parseCollections(json.collections)
            );
        } catch (IOException exception) {
            return StoredBibleState.empty();
        }
    }

    public static void save(Path path, StoredBibleState state) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(StoredBibleStateJson.from(state), writer);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save Bible reader preferences", exception);
        }
    }

    private static List<BibleReference> parseReferences(List<String> stableIds) {
        if (stableIds == null) {
            return List.of();
        }
        return stableIds.stream().map(BibleReference::parseStableId).flatMap(Optional::stream).toList();
    }

    private static List<VerseNote> parseNotes(List<StoredNoteJson> notes) {
        if (notes == null) {
            return List.of();
        }
        return notes.stream()
            .filter(note -> note != null)
            .map(note -> BibleReference.parseStableId(note.reference())
                .filter(ignored -> note.text() != null && !note.text().isBlank())
                .map(reference -> new VerseNote(reference, note.text())))
            .flatMap(Optional::stream)
            .toList();
    }

    private static List<VerseCollection> parseCollections(List<StoredCollectionJson> collections) {
        if (collections == null) {
            return List.of();
        }
        return collections.stream()
            .filter(collection -> collection != null)
            .map(collection -> new VerseCollection(collection.name(), parseReferences(collection.references())))
            .filter(collection -> !collection.references().isEmpty())
            .toList();
    }

    public record StoredBibleState(
        Optional<BibleReference> lastReference,
        List<BibleReference> bookmarks,
        List<BibleReference> recentHistory,
        List<BibleReference> highlights,
        List<VerseNote> notes,
        List<VerseCollection> collections
    ) {
        public StoredBibleState {
            lastReference = lastReference == null ? Optional.empty() : lastReference;
            bookmarks = List.copyOf(bookmarks == null ? List.of() : bookmarks);
            recentHistory = List.copyOf(recentHistory == null ? List.of() : recentHistory);
            highlights = List.copyOf(highlights == null ? List.of() : highlights);
            notes = List.copyOf(notes == null ? List.of() : notes);
            collections = List.copyOf(collections == null ? List.of() : collections);
        }

        public static StoredBibleState empty() {
            return new StoredBibleState(Optional.empty(), List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    private record StoredBibleStateJson(
        String lastReference,
        List<String> bookmarks,
        List<String> recentHistory,
        List<String> highlights,
        List<StoredNoteJson> notes,
        List<StoredCollectionJson> collections
    ) {
        static StoredBibleStateJson from(StoredBibleState state) {
            return new StoredBibleStateJson(
                state.lastReference().map(BibleReference::toStableId).orElse(""),
                state.bookmarks().stream().map(BibleReference::toStableId).toList(),
                state.recentHistory().stream().map(BibleReference::toStableId).toList(),
                state.highlights().stream().map(BibleReference::toStableId).toList(),
                state.notes().stream().map(StoredNoteJson::from).toList(),
                state.collections().stream().map(StoredCollectionJson::from).toList()
            );
        }
    }

    private record StoredNoteJson(String reference, String text) {
        static StoredNoteJson from(VerseNote note) {
            return new StoredNoteJson(note.reference().toStableId(), note.text());
        }
    }

    private record StoredCollectionJson(String name, List<String> references) {
        static StoredCollectionJson from(VerseCollection collection) {
            return new StoredCollectionJson(
                collection.name(),
                collection.references().stream().map(BibleReference::toStableId).toList()
            );
        }
    }
}
