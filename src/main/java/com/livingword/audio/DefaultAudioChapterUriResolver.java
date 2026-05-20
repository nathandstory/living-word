package com.livingword.audio;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultAudioChapterUriResolver implements AudioChapterUriResolver {
    private static final Map<String, AudioBookPath> BOOK_PATHS = bookPaths();
    private static final Map<String, String> HELLO_AO_BOOK_CODES = helloAoBookCodes();

    private final RemoteResourceReader resourceReader;
    private final Map<URI, String> directoryCache = new ConcurrentHashMap<>();

    public DefaultAudioChapterUriResolver() {
        this(uri -> uri.toURL().openStream());
    }

    DefaultAudioChapterUriResolver(RemoteResourceReader resourceReader) {
        this.resourceReader = Objects.requireNonNull(resourceReader, "resourceReader");
    }

    @Override
    public URI resolve(AudioManifest manifest, AudioChapterId chapterId) throws IOException {
        if ("ebible-web-directory".equals(manifest.pathStrategy())) {
            URI directoryUri = EbibleWebAudioIndex.bookDirectoryUri(manifest.baseUri(), chapterId);
            String directoryHtml = directoryCache.computeIfAbsent(directoryUri, this::readUnchecked);
            return EbibleWebAudioIndex.resolveChapterUri(manifest.baseUri(), chapterId, directoryHtml);
        }
        if ("audiotreasure-kjv".equals(manifest.pathStrategy())) {
            return audioTreasureKjvUri(manifest.baseUri(), chapterId);
        }
        if ("public-domain-audio-bibles".equals(manifest.pathStrategy())) {
            return publicDomainAudioBiblesUri(manifest.baseUri(), chapterId);
        }
        if (manifest.pathStrategy().startsWith("helloao-bsb-")) {
            return helloAoBsbUri(manifest.baseUri(), chapterId, manifest.pathStrategy().substring("helloao-bsb-".length()));
        }
        if ("restricted-licensed-provider".equals(manifest.pathStrategy())) {
            throw new IOException("This audio source requires a licensed or bring-your-own provider configuration.");
        }
        return manifest.chapterUri(chapterId);
    }

    private static URI audioTreasureKjvUri(URI baseUri, AudioChapterId chapterId) {
        AudioBookPath path = pathFor(chapterId);
        String fileName = path.kjvChapterlessSingle()
            ? path.kjvPrefix() + ".mp3"
            : path.kjvPrefix() + "%03d.mp3".formatted(chapterId.chapter());
        return baseUri.resolve(fileName);
    }

    private static URI publicDomainAudioBiblesUri(URI baseUri, AudioChapterId chapterId) {
        AudioBookPath path = pathFor(chapterId);
        String fileName = path.webPrefix() + "%02d.mp3".formatted(chapterId.chapter());
        return baseUri.resolve(path.webFolder() + "/" + fileName);
    }

    private static URI helloAoBsbUri(URI baseUri, AudioChapterId chapterId, String narratorId) {
        if (!narratorId.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Unsupported HelloAO narrator id: " + narratorId);
        }
        String bookCode = Optional.ofNullable(HELLO_AO_BOOK_CODES.get(chapterId.bookId()))
            .orElseThrow(() -> new IllegalArgumentException("Unsupported HelloAO audio Bible book: " + chapterId.bookId()));
        return baseUri.resolve("%s/%d/audio/%s.mp3".formatted(bookCode, chapterId.chapter(), narratorId));
    }

    private static AudioBookPath pathFor(AudioChapterId chapterId) {
        return Optional.ofNullable(BOOK_PATHS.get(chapterId.bookId()))
            .orElseThrow(() -> new IllegalArgumentException("Unsupported audio Bible book: " + chapterId.bookId()));
    }

    private String readUnchecked(URI uri) {
        try (InputStream inputStream = resourceReader.open(uri)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read audio directory index " + uri, exception);
        }
    }

    @FunctionalInterface
    interface RemoteResourceReader {
        InputStream open(URI uri) throws IOException;
    }

    private record AudioBookPath(String kjvPrefix, boolean kjvChapterlessSingle, String webFolder, String webPrefix) {
    }

    private static Map<String, AudioBookPath> bookPaths() {
        Map<String, AudioBookPath> paths = new LinkedHashMap<>();
        put(paths, "genesis", "01_Genesis", "01_Gen", "01_Genesis_");
        put(paths, "exodus", "02_Exodus", "02_Exo", "02_Exodus_");
        put(paths, "leviticus", "03_Leviticus", "03_Lev", "03_Leviticus_");
        put(paths, "numbers", "04_Numbers", "04_Num", "04_Numbers_");
        put(paths, "deuteronomy", "05_Deuteronomy", "05_Deu", "05_Deuteronomy_");
        put(paths, "joshua", "06_Joshua", "06_Jos", "06_Joshua_");
        put(paths, "judges", "07_Judges", "07_Jdg", "07_Judges_");
        put(paths, "ruth", "08_Ruth", "08_Rut", "08_Ruth_");
        put(paths, "1_samuel", "09_1Samuel", "09_1Sam", "09_1Samuel_");
        put(paths, "2_samuel", "10_2Samuel", "10_2Sam", "10_2Samuel_");
        put(paths, "1_kings", "11_1Kings", "11_1Kings", "11_1Kings_");
        put(paths, "2_kings", "12_2Kings", "12_2Kings", "12_2Kings_");
        put(paths, "1_chronicles", "13_1Chronicles", "13_1Chron", "13_1Chronicles_");
        put(paths, "2_chronicles", "14_2Chronicles", "14_2Chron", "14_2Chronicles_");
        put(paths, "ezra", "15_Ezra", "15_Ezra", "15_Ezra_");
        put(paths, "nehemiah", "16_Nehemiah", "16_Neh", "16_Nehemiah_");
        put(paths, "esther", "17_Esther", "17_Est", "17_Esther_");
        put(paths, "job", "18_Job", "18_Job", "18_Job_");
        put(paths, "psalms", "19_Psalms", "19_Psa", "19_Psalms_");
        put(paths, "proverbs", "20_Proverbs", "20_Prov", "20_Proverbs_");
        put(paths, "ecclesiastes", "21_Ecclesiastes", "21_Ecc", "21_Ecclesiastes_");
        put(paths, "song_of_solomon", "22_Song_of_Soloman", "22_Song", "22_SongofSolomon_");
        put(paths, "isaiah", "23_Isaiah", "23_Isa", "23_Isaiah_");
        put(paths, "jeremiah", "24_Jeremiah", "24_Jer", "24_Jeremiah_");
        put(paths, "lamentations", "25_Lamentations", "25_Lam", "25_Lamentations_");
        put(paths, "ezekiel", "26_Ezekiel", "26_Ezk", "26_Ezekiel_");
        put(paths, "daniel", "27_Daniel", "27_Dan", "27_Daniel_");
        put(paths, "hosea", "28_Hosea", "28_Hos", "28_Hosea_");
        put(paths, "joel", "29_Joel", "29_Joel", "29_Joel_");
        put(paths, "amos", "30_Amos", "30_Amos", "30_Amos_");
        put(paths, "obadiah", "31_Obadiah", "31_Oba", "31_Obadiah_");
        put(paths, "jonah", "32_Jonah", "32_Jon", "32_Jonah_");
        put(paths, "micah", "33_Micah", "33_Mic", "33_Micah_");
        put(paths, "nahum", "34_Nahum", "34_Nah", "34_Nahum_");
        put(paths, "habakkuk", "35_Habakkuk", "35_Hab", "35_Habakkuk_");
        put(paths, "zephaniah", "36_Zephaniah", "36_Zep", "36_Zephaniah_");
        put(paths, "haggai", "37_Haggai", "37_Hag", "37_Haggai_");
        put(paths, "zechariah", "38_Zechariah", "38_Zec", "38_Zechariah_");
        put(paths, "malachi", "39_Malachi", "39_Mal", "39_Malachi_");
        put(paths, "matthew", "40_Matthew", "40_Mat", "40_Matthew_");
        put(paths, "mark", "41_Mark", "41_Mark", "41_Mark_");
        put(paths, "luke", "42_Luke", "42_Luke", "42_Luke_");
        put(paths, "john", "43_John", "43_John", "43_John_");
        put(paths, "acts", "44_Acts", "44_Acts", "44_Acts_");
        put(paths, "romans", "45_Romans", "45_Rom", "45_Romans_");
        put(paths, "1_corinthians", "46_1Corinthians", "46_1Cor", "46_1Corinthians_");
        put(paths, "2_corinthians", "47_2Corinthians", "47_2Cor", "47_2Corinthians_");
        put(paths, "galatians", "48_Galatians", "48_Gal", "48_Galatians_");
        put(paths, "ephesians", "49_Ephesians", "49_Eph", "49_Ephesians_");
        put(paths, "philippians", "50_Philippians", "50_Phi", "50_Philippians_");
        put(paths, "colossians", "51_Colossians", "51_Col", "51_Colossians_");
        put(paths, "1_thessalonians", "52_1Thessalonians", "52_1Th", "52_1Thessalonians_");
        put(paths, "2_thessalonians", "53_2Thessalonians", "53_2Th", "53_2Thessalonians_");
        put(paths, "1_timothy", "54_1Timothy", "54_1Ti", "54_1Timothy_");
        put(paths, "2_timothy", "55_2Timothy", "55_2Ti", "55_2Timothy_");
        put(paths, "titus", "56_Titus", "56_Tit", "56_Titus_");
        putSingleChapterKjv(paths, "philemon", "57_Philemon", "57_Phm", "57_Philemon_");
        put(paths, "hebrews", "58_Hebrews", "58_Heb", "58_Hebrews_");
        put(paths, "james", "59_James", "59_Jas", "59_James_");
        put(paths, "1_peter", "60_1Peter", "60_1Pe", "60_1Peter_");
        put(paths, "2_peter", "61_2Peter", "61_2Pe", "61_2Peter_");
        put(paths, "1_john", "62_1John", "62_1Jn", "62_1John_");
        putSingleChapterKjv(paths, "2_john", "63_2John", "63_2Jn", "63_2John_");
        putSingleChapterKjv(paths, "3_john", "64_3John", "64_3Jn", "64_3John_");
        putSingleChapterKjv(paths, "jude", "65_Jude", "65_Jud", "65_Jude_");
        put(paths, "revelation", "66_Revelation", "66_Rev", "66_Revelation_");
        return Map.copyOf(paths);
    }

    private static Map<String, String> helloAoBookCodes() {
        Map<String, String> codes = new LinkedHashMap<>();
        codes.put("genesis", "GEN");
        codes.put("exodus", "EXO");
        codes.put("leviticus", "LEV");
        codes.put("numbers", "NUM");
        codes.put("deuteronomy", "DEU");
        codes.put("joshua", "JOS");
        codes.put("judges", "JDG");
        codes.put("ruth", "RUT");
        codes.put("1_samuel", "1SA");
        codes.put("2_samuel", "2SA");
        codes.put("1_kings", "1KI");
        codes.put("2_kings", "2KI");
        codes.put("1_chronicles", "1CH");
        codes.put("2_chronicles", "2CH");
        codes.put("ezra", "EZR");
        codes.put("nehemiah", "NEH");
        codes.put("esther", "EST");
        codes.put("job", "JOB");
        codes.put("psalms", "PSA");
        codes.put("proverbs", "PRO");
        codes.put("ecclesiastes", "ECC");
        codes.put("song_of_solomon", "SNG");
        codes.put("isaiah", "ISA");
        codes.put("jeremiah", "JER");
        codes.put("lamentations", "LAM");
        codes.put("ezekiel", "EZK");
        codes.put("daniel", "DAN");
        codes.put("hosea", "HOS");
        codes.put("joel", "JOL");
        codes.put("amos", "AMO");
        codes.put("obadiah", "OBA");
        codes.put("jonah", "JON");
        codes.put("micah", "MIC");
        codes.put("nahum", "NAM");
        codes.put("habakkuk", "HAB");
        codes.put("zephaniah", "ZEP");
        codes.put("haggai", "HAG");
        codes.put("zechariah", "ZEC");
        codes.put("malachi", "MAL");
        codes.put("matthew", "MAT");
        codes.put("mark", "MRK");
        codes.put("luke", "LUK");
        codes.put("john", "JHN");
        codes.put("acts", "ACT");
        codes.put("romans", "ROM");
        codes.put("1_corinthians", "1CO");
        codes.put("2_corinthians", "2CO");
        codes.put("galatians", "GAL");
        codes.put("ephesians", "EPH");
        codes.put("philippians", "PHP");
        codes.put("colossians", "COL");
        codes.put("1_thessalonians", "1TH");
        codes.put("2_thessalonians", "2TH");
        codes.put("1_timothy", "1TI");
        codes.put("2_timothy", "2TI");
        codes.put("titus", "TIT");
        codes.put("philemon", "PHM");
        codes.put("hebrews", "HEB");
        codes.put("james", "JAS");
        codes.put("1_peter", "1PE");
        codes.put("2_peter", "2PE");
        codes.put("1_john", "1JN");
        codes.put("2_john", "2JN");
        codes.put("3_john", "3JN");
        codes.put("jude", "JUD");
        codes.put("revelation", "REV");
        return Map.copyOf(codes);
    }

    private static void put(Map<String, AudioBookPath> paths, String bookId, String kjvPrefix, String webFolder, String webPrefix) {
        paths.put(bookId, new AudioBookPath(kjvPrefix, false, webFolder, webPrefix));
    }

    private static void putSingleChapterKjv(Map<String, AudioBookPath> paths, String bookId, String kjvPrefix, String webFolder, String webPrefix) {
        paths.put(bookId, new AudioBookPath(kjvPrefix, true, webFolder, webPrefix));
    }
}
