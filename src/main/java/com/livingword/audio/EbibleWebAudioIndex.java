package com.livingword.audio;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Arrays;

public final class EbibleWebAudioIndex {
    private static final Pattern MP3_HREF = Pattern.compile("href=\"([^\"]+\\.mp3)\"", Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> BOOK_FOLDERS = bookFolders();
    private static final String[] ONES = {
        "",
        "one",
        "two",
        "three",
        "four",
        "five",
        "six",
        "seven",
        "eight",
        "nine",
        "ten",
        "eleven",
        "twelve",
        "thirteen",
        "fourteen",
        "fifteen",
        "sixteen",
        "seventeen",
        "eighteen",
        "nineteen"
    };
    private static final String[] TENS = {
        "",
        "",
        "twenty",
        "thirty",
        "forty",
        "fifty",
        "sixty",
        "seventy",
        "eighty",
        "ninety"
    };

    private EbibleWebAudioIndex() {
    }

    public static URI resolveChapterUri(URI baseUri, AudioChapterId chapterId, String directoryHtml) {
        String folder = folderFor(chapterId.bookId());
        URI folderUri = baseUri.resolve(folder + "/");
        String expectedChapter = normalize("chapter " + chapterWords(chapterId.chapter()));

        Matcher matcher = MP3_HREF.matcher(directoryHtml);
        while (matcher.find()) {
            String href = matcher.group(1);
            String decoded = URLDecoder.decode(href, StandardCharsets.UTF_8);
            if (normalize(decoded).contains(expectedChapter)) {
                return folderUri.resolve(URI.create(encodePath(href)));
            }
        }

        throw new IllegalArgumentException("No eBible WEB audio link found for " + chapterId);
    }

    public static URI bookDirectoryUri(URI baseUri, AudioChapterId chapterId) {
        return baseUri.resolve(folderFor(chapterId.bookId()) + "/");
    }

    private static String folderFor(String bookId) {
        return Optional.ofNullable(BOOK_FOLDERS.get(bookId))
            .orElseThrow(() -> new IllegalArgumentException("Unsupported eBible WEB audio book: " + bookId));
    }

    private static String chapterWords(int chapter) {
        if (chapter <= 0 || chapter > 150) {
            throw new IllegalArgumentException("Unsupported eBible WEB chapter number: " + chapter);
        }
        if (chapter < 20) {
            return ONES[chapter];
        }
        if (chapter < 100) {
            int tens = chapter / 10;
            int ones = chapter % 10;
            return ones == 0 ? TENS[tens] : TENS[tens] + " " + ONES[ones];
        }
        int remainder = chapter - 100;
        return remainder == 0 ? "one hundred" : "one hundred " + chapterWords(remainder);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
            .replace('_', ' ')
            .replace('-', ' ')
            .replaceAll("[^a-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String encodePath(String path) {
        if (path.contains("%")) {
            return path;
        }
        return Arrays.stream(path.split("/", -1))
            .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
            .collect(Collectors.joining("/"));
    }

    private static Map<String, String> bookFolders() {
        Map<String, String> folders = new LinkedHashMap<>();
        folders.put("genesis", "01_Genesis");
        folders.put("exodus", "02_Exodus");
        folders.put("leviticus", "03_Leviticus");
        folders.put("numbers", "04_Numbers");
        folders.put("deuteronomy", "05_Deuteronomy");
        folders.put("joshua", "06_Joshua");
        folders.put("judges", "07_Judges");
        folders.put("ruth", "08_Ruth");
        folders.put("1_samuel", "09_First_Samuel");
        folders.put("2_samuel", "10_Second_Samuel");
        folders.put("1_kings", "11_First_Kings");
        folders.put("2_kings", "12_Second_Kings");
        folders.put("1_chronicles", "13_First_Chronicles");
        folders.put("2_chronicles", "14_Second_Chronicles");
        folders.put("ezra", "15_Ezra");
        folders.put("nehemiah", "16_Nehemiah");
        folders.put("esther", "17_Esther");
        folders.put("job", "18_Job");
        folders.put("psalms", "19_Psalms");
        folders.put("proverbs", "20_Proverbs");
        folders.put("ecclesiastes", "21_Ecclesiastes");
        folders.put("song_of_solomon", "22_Song_of_Solomon");
        folders.put("isaiah", "23_Isaiah");
        folders.put("jeremiah", "24_Jeremiah");
        folders.put("lamentations", "25_Lamentations");
        folders.put("ezekiel", "26_Ezekiel");
        folders.put("daniel", "27_Daniel");
        folders.put("hosea", "28_Hosea");
        folders.put("joel", "29_Joel");
        folders.put("amos", "30_Amos");
        folders.put("obadiah", "31_Obadiah");
        folders.put("jonah", "32_Jonah");
        folders.put("micah", "33_Micah");
        folders.put("nahum", "34_Nahum");
        folders.put("habakkuk", "35_Habakkuk");
        folders.put("zephaniah", "36_Zephaniah");
        folders.put("haggai", "37_Haggai");
        folders.put("zechariah", "38_Zechariah");
        folders.put("malachi", "39_Malachi");
        folders.put("matthew", "40_Matthew");
        folders.put("mark", "41_Mark");
        folders.put("luke", "42_Luke");
        folders.put("john", "43_John");
        folders.put("acts", "44_Acts");
        folders.put("romans", "45_Romans");
        folders.put("1_corinthians", "46_First_Corinthians");
        folders.put("2_corinthians", "47_Second_Corinthians");
        folders.put("galatians", "48_Galatians");
        folders.put("ephesians", "49_Ephesians");
        folders.put("philippians", "50_Philippians");
        folders.put("colossians", "51_Colossians");
        folders.put("1_thessalonians", "52_First_Thessalonians");
        folders.put("2_thessalonians", "53_Second_Thessalonians");
        folders.put("1_timothy", "54_First_Timothy");
        folders.put("2_timothy", "55_Second_Timothy");
        folders.put("titus", "56_Titus");
        folders.put("philemon", "57_Philemon");
        folders.put("hebrews", "58_Hebrews");
        folders.put("james", "59_James");
        folders.put("1_peter", "60_First_Peter");
        folders.put("2_peter", "61_Second_Peter");
        folders.put("1_john", "62_First_John");
        folders.put("2_john", "63_Second_John");
        folders.put("3_john", "64_Third_John");
        folders.put("jude", "65_Jude");
        folders.put("revelation", "66_Revelations");
        return Map.copyOf(folders);
    }
}
