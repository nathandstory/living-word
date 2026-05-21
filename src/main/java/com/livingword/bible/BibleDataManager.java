package com.livingword.bible;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BibleDataManager {
    private static final Pattern CHAPTER_VERSE_PATTERN = Pattern.compile("^(\\d+)(?:(?:\\s*[:.]\\s*|\\s+)(\\d+))?.*$");
    private static final Pattern SIMPLE_TOKEN_PATTERN = Pattern.compile("[a-z0-9]+");

    private final Map<String, TranslationManifest> translations = new HashMap<>();
    private final Map<String, ChapterData> chapters = new HashMap<>();

    public void registerTranslation(TranslationManifest manifest) {
        translations.put(manifest.id(), manifest);
    }

    public void registerChapter(ChapterData chapter) {
        chapters.put(chapterKey(chapter.translationId(), chapter.bookId(), chapter.chapter()), chapter);
    }

    public Optional<TranslationManifest> getTranslation(String translationId) {
        return Optional.ofNullable(translations.get(translationId));
    }

    public List<TranslationManifest> translations() {
        List<TranslationManifest> ordered = new ArrayList<>(translations.values());
        ordered.sort(Comparator.comparing(TranslationManifest::displayName));
        return List.copyOf(ordered);
    }

    public Optional<ChapterData> getChapter(String translationId, String bookId, int chapter) {
        return Optional.ofNullable(chapters.get(chapterKey(translationId, bookId, chapter)));
    }

    public Optional<String> getVerse(BibleReference reference) {
        return getChapter(reference.translationId(), reference.bookId(), reference.chapter())
            .map(chapter -> chapter.verseText(reference.verse()))
            .filter(text -> !text.isEmpty());
    }

    public List<String> bookIds(String translationId) {
        Set<String> loadedBooks = new HashSet<>();
        for (ChapterData chapter : chapters.values()) {
            if (chapter.translationId().equals(translationId)) {
                loadedBooks.add(chapter.bookId());
            }
        }
        List<String> ordered = new ArrayList<>();
        getTranslation(translationId).ifPresent(manifest -> {
            for (String bookId : manifest.bookOrder()) {
                if (loadedBooks.remove(bookId)) {
                    ordered.add(bookId);
                }
            }
        });
        List<String> remaining = new ArrayList<>(loadedBooks);
        remaining.sort(String::compareTo);
        ordered.addAll(remaining);
        return List.copyOf(ordered);
    }

    public List<Integer> chapters(String translationId, String bookId) {
        List<Integer> ordered = new ArrayList<>();
        for (ChapterData chapter : chapters.values()) {
            if (chapter.translationId().equals(translationId) && chapter.bookId().equals(bookId)) {
                ordered.add(chapter.chapter());
            }
        }
        ordered.sort(Integer::compareTo);
        return List.copyOf(ordered);
    }

    public Optional<ChapterData> firstChapter(String translationId) {
        for (String bookId : bookIds(translationId)) {
            List<Integer> chapterNumbers = chapters(translationId, bookId);
            if (!chapterNumbers.isEmpty()) {
                return getChapter(translationId, bookId, chapterNumbers.getFirst());
            }
        }
        return Optional.empty();
    }

    public List<BibleReference> search(String translationId, String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }
        Optional<BibleReference> reference = parseReferenceQuery(translationId, query);
        if (reference.isPresent()) {
            return List.of(reference.orElseThrow());
        }

        String normalizedQuery = normalizeSearchText(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }
        List<String> terms = searchTerms(normalizedQuery);
        List<BibleReference> results = new ArrayList<>();
        for (String bookId : bookIds(translationId)) {
            for (int chapterNumber : chapters(translationId, bookId)) {
                ChapterData chapter = getChapter(translationId, bookId, chapterNumber).orElseThrow();
                for (Map.Entry<Integer, String> verse : new TreeMap<>(chapter.verses()).entrySet()) {
                    String normalizedVerse = normalizeSearchText(verse.getValue());
                    if (matchesSearch(normalizedVerse, normalizedQuery, terms)) {
                        results.add(new BibleReference(translationId, bookId, chapterNumber, verse.getKey()));
                        if (results.size() >= limit) {
                            return List.copyOf(results);
                        }
                    }
                }
            }
        }
        return List.copyOf(results);
    }

    private Optional<BibleReference> parseReferenceQuery(String translationId, String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT)
            .replace('_', ' ')
            .replace('-', ' ')
            .replaceAll("\\s+", " ")
            .replaceAll("\\s*:\\s*", ":")
            .trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        List<BookAlias> aliases = new ArrayList<>();
        for (String bookId : bookIds(translationId)) {
            aliases.addAll(bookAliases(bookId));
        }
        aliases.sort(Comparator.comparingInt((BookAlias alias) -> alias.alias().length()).reversed());

        for (BookAlias alias : aliases) {
            Optional<String> remainder = referenceRemainder(normalized, alias.alias());
            if (remainder.isEmpty()) {
                continue;
            }
            return referenceFromRemainder(translationId, alias.bookId(), remainder.orElseThrow());
        }
        return Optional.empty();
    }

    private Optional<BibleReference> referenceFromRemainder(String translationId, String bookId, String rawRemainder) {
        String remainder = rawRemainder.strip();
        if (remainder.startsWith(":")) {
            remainder = remainder.substring(1).strip();
        }

        int chapter = firstChapterNumber(translationId, bookId).orElse(1);
        int verse = 1;
        if (!remainder.isEmpty()) {
            Matcher matcher = CHAPTER_VERSE_PATTERN.matcher(remainder);
            if (!matcher.matches()) {
                return Optional.empty();
            }
            chapter = Integer.parseInt(matcher.group(1));
            if (matcher.group(2) != null) {
                verse = Integer.parseInt(matcher.group(2));
            }
        }

        Optional<ChapterData> chapterData = getChapter(translationId, bookId, chapter);
        if (chapterData.isEmpty()) {
            return Optional.empty();
        }
        int selectedVerse = chapterData.orElseThrow().verses().containsKey(verse)
            ? verse
            : chapterData.orElseThrow().verses().keySet().stream().min(Integer::compareTo).orElse(1);
        return Optional.of(new BibleReference(translationId, bookId, chapter, selectedVerse));
    }

    private Optional<Integer> firstChapterNumber(String translationId, String bookId) {
        return chapters(translationId, bookId).stream().findFirst();
    }

    private static Optional<String> referenceRemainder(String query, String alias) {
        if (query.equals(alias)) {
            return Optional.of("");
        }
        if (query.startsWith(alias + " ")) {
            return Optional.of(query.substring(alias.length() + 1));
        }
        if (query.startsWith(alias + ":")) {
            return Optional.of(query.substring(alias.length()));
        }
        return Optional.empty();
    }

    private static List<BookAlias> bookAliases(String bookId) {
        String spaced = bookId.replace('_', ' ');
        List<BookAlias> aliases = new ArrayList<>();
        aliases.add(new BookAlias(bookId, spaced));
        aliases.add(new BookAlias(bookId, bookId.replace("_", "")));
        numberedBookAliases(bookId).forEach(aliases::add);
        switch (bookId) {
            case "genesis" -> aliases.add(new BookAlias(bookId, "gen"));
            case "exodus" -> aliases.add(new BookAlias(bookId, "exo"));
            case "matthew" -> aliases.add(new BookAlias(bookId, "matt"));
            case "mark" -> aliases.add(new BookAlias(bookId, "mrk"));
            case "luke" -> aliases.add(new BookAlias(bookId, "luk"));
            case "john" -> {
                aliases.add(new BookAlias(bookId, "jn"));
                aliases.add(new BookAlias(bookId, "jhn"));
            }
            case "romans" -> aliases.add(new BookAlias(bookId, "rom"));
            case "psalms" -> aliases.add(new BookAlias(bookId, "psalm"));
            case "song_of_solomon" -> {
                aliases.add(new BookAlias(bookId, "song of songs"));
                aliases.add(new BookAlias(bookId, "songs"));
            }
            case "revelation" -> {
                aliases.add(new BookAlias(bookId, "rev"));
                aliases.add(new BookAlias(bookId, "revelations"));
                aliases.add(new BookAlias(bookId, "the revelation"));
                aliases.add(new BookAlias(bookId, "revelation of john"));
            }
            default -> {
            }
        }
        return aliases;
    }

    private static boolean matchesSearch(String normalizedVerse, String normalizedQuery, List<String> terms) {
        if (normalizedVerse.contains(normalizedQuery)) {
            return true;
        }
        return !terms.isEmpty() && terms.stream().allMatch(normalizedVerse::contains);
    }

    private static String normalizeSearchText(String value) {
        Matcher matcher = SIMPLE_TOKEN_PATTERN.matcher(value.toLowerCase(Locale.ROOT));
        StringBuilder normalized = new StringBuilder();
        while (matcher.find()) {
            if (!normalized.isEmpty()) {
                normalized.append(' ');
            }
            normalized.append(matcher.group());
        }
        return normalized.toString();
    }

    private static List<String> searchTerms(String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            return List.of();
        }
        return List.of(normalizedQuery.split("\\s+"));
    }

    private static List<BookAlias> numberedBookAliases(String bookId) {
        if (bookId.length() < 3 || bookId.charAt(1) != '_' || bookId.charAt(0) < '1' || bookId.charAt(0) > '3') {
            return List.of();
        }
        String rest = bookId.substring(2).replace('_', ' ');
        String number = String.valueOf(bookId.charAt(0));
        String ordinal = switch (bookId.charAt(0)) {
            case '1' -> "first";
            case '2' -> "second";
            case '3' -> "third";
            default -> number;
        };
        String suffix = switch (bookId.charAt(0)) {
            case '1' -> "1st";
            case '2' -> "2nd";
            case '3' -> "3rd";
            default -> number;
        };
        String roman = switch (bookId.charAt(0)) {
            case '1' -> "i";
            case '2' -> "ii";
            case '3' -> "iii";
            default -> number;
        };
        return List.of(
            new BookAlias(bookId, ordinal + " " + rest),
            new BookAlias(bookId, suffix + " " + rest),
            new BookAlias(bookId, roman + " " + rest)
        );
    }

    private static String chapterKey(String translationId, String bookId, int chapter) {
        return translationId + ':' + bookId + ':' + chapter;
    }

    private record BookAlias(String bookId, String alias) {
    }
}
