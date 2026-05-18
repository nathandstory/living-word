#!/usr/bin/env python3
"""Convert 66-book USFM Bible text into Living Word translation resources."""

from __future__ import annotations

import argparse
import json
import re
from collections import OrderedDict
from pathlib import Path


BOOKS = OrderedDict(
    [
        ("genesis", "Genesis"),
        ("exodus", "Exodus"),
        ("leviticus", "Leviticus"),
        ("numbers", "Numbers"),
        ("deuteronomy", "Deuteronomy"),
        ("joshua", "Joshua"),
        ("judges", "Judges"),
        ("ruth", "Ruth"),
        ("1_samuel", "1 Samuel"),
        ("2_samuel", "2 Samuel"),
        ("1_kings", "1 Kings"),
        ("2_kings", "2 Kings"),
        ("1_chronicles", "1 Chronicles"),
        ("2_chronicles", "2 Chronicles"),
        ("ezra", "Ezra"),
        ("nehemiah", "Nehemiah"),
        ("esther", "Esther"),
        ("job", "Job"),
        ("psalms", "Psalms"),
        ("proverbs", "Proverbs"),
        ("ecclesiastes", "Ecclesiastes"),
        ("song_of_solomon", "Song of Solomon"),
        ("isaiah", "Isaiah"),
        ("jeremiah", "Jeremiah"),
        ("lamentations", "Lamentations"),
        ("ezekiel", "Ezekiel"),
        ("daniel", "Daniel"),
        ("hosea", "Hosea"),
        ("joel", "Joel"),
        ("amos", "Amos"),
        ("obadiah", "Obadiah"),
        ("jonah", "Jonah"),
        ("micah", "Micah"),
        ("nahum", "Nahum"),
        ("habakkuk", "Habakkuk"),
        ("zephaniah", "Zephaniah"),
        ("haggai", "Haggai"),
        ("zechariah", "Zechariah"),
        ("malachi", "Malachi"),
        ("matthew", "Matthew"),
        ("mark", "Mark"),
        ("luke", "Luke"),
        ("john", "John"),
        ("acts", "Acts"),
        ("romans", "Romans"),
        ("1_corinthians", "1 Corinthians"),
        ("2_corinthians", "2 Corinthians"),
        ("galatians", "Galatians"),
        ("ephesians", "Ephesians"),
        ("philippians", "Philippians"),
        ("colossians", "Colossians"),
        ("1_thessalonians", "1 Thessalonians"),
        ("2_thessalonians", "2 Thessalonians"),
        ("1_timothy", "1 Timothy"),
        ("2_timothy", "2 Timothy"),
        ("titus", "Titus"),
        ("philemon", "Philemon"),
        ("hebrews", "Hebrews"),
        ("james", "James"),
        ("1_peter", "1 Peter"),
        ("2_peter", "2 Peter"),
        ("1_john", "1 John"),
        ("2_john", "2 John"),
        ("3_john", "3 John"),
        ("jude", "Jude"),
        ("revelation", "Revelation"),
    ]
)

USFM_BOOK_IDS = {
    "GEN": "genesis",
    "EXO": "exodus",
    "LEV": "leviticus",
    "NUM": "numbers",
    "DEU": "deuteronomy",
    "JOS": "joshua",
    "JDG": "judges",
    "RUT": "ruth",
    "1SA": "1_samuel",
    "2SA": "2_samuel",
    "1KI": "1_kings",
    "2KI": "2_kings",
    "1CH": "1_chronicles",
    "2CH": "2_chronicles",
    "EZR": "ezra",
    "NEH": "nehemiah",
    "EST": "esther",
    "JOB": "job",
    "PSA": "psalms",
    "PRO": "proverbs",
    "ECC": "ecclesiastes",
    "SNG": "song_of_solomon",
    "ISA": "isaiah",
    "JER": "jeremiah",
    "LAM": "lamentations",
    "EZK": "ezekiel",
    "DAN": "daniel",
    "HOS": "hosea",
    "JOL": "joel",
    "AMO": "amos",
    "OBA": "obadiah",
    "JON": "jonah",
    "MIC": "micah",
    "NAM": "nahum",
    "HAB": "habakkuk",
    "ZEP": "zephaniah",
    "HAG": "haggai",
    "ZEC": "zechariah",
    "MAL": "malachi",
    "MAT": "matthew",
    "MRK": "mark",
    "LUK": "luke",
    "JHN": "john",
    "ACT": "acts",
    "ROM": "romans",
    "1CO": "1_corinthians",
    "2CO": "2_corinthians",
    "GAL": "galatians",
    "EPH": "ephesians",
    "PHP": "philippians",
    "COL": "colossians",
    "1TH": "1_thessalonians",
    "2TH": "2_thessalonians",
    "1TI": "1_timothy",
    "2TI": "2_timothy",
    "TIT": "titus",
    "PHM": "philemon",
    "HEB": "hebrews",
    "JAS": "james",
    "1PE": "1_peter",
    "2PE": "2_peter",
    "1JN": "1_john",
    "2JN": "2_john",
    "3JN": "3_john",
    "JUD": "jude",
    "REV": "revelation",
}

CHAPTER = re.compile(r"^\\c\s+(\d+)")
VERSE = re.compile(r"^\\v\s+(\d+)(?:-\d+)?\s*(.*)$")
BOOK_ID = re.compile(r"^\\id\s+([A-Z0-9]{3})\b")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--translation-id", required=True)
    parser.add_argument("--display-name", required=True)
    parser.add_argument("--language", default="en_us")
    parser.add_argument("--license", default="Public Domain")
    parser.add_argument("--attribution", required=True)
    parser.add_argument("--audio-manifest-id", default="")
    args = parser.parse_args()

    records = parse_usfm_directory(args.source)
    write_translation(args, records)
    print(f"Wrote {sum(len(chapters) for chapters in records.values())} chapters to {args.output}")
    return 0


def parse_usfm_directory(source: Path) -> OrderedDict[str, OrderedDict[int, OrderedDict[int, str]]]:
    parsed = {}
    for path in sorted(source.glob("*.usfm")):
        book_id, chapters = parse_usfm_text(path.read_text(encoding="utf-8"))
        if book_id is not None:
            parsed[book_id] = chapters
    return OrderedDict((book_id, parsed[book_id]) for book_id in BOOKS if book_id in parsed)


def parse_usfm_text(source: str) -> tuple[str | None, OrderedDict[int, OrderedDict[int, str]]]:
    source = strip_spans(source, "f")
    source = strip_spans(source, "x")
    chapters: OrderedDict[int, OrderedDict[int, str]] = OrderedDict()
    book_id = None
    current_chapter = None
    current_verse = None

    for raw_line in source.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        book_match = BOOK_ID.match(line)
        if book_match:
            book_id = USFM_BOOK_IDS.get(book_match.group(1))
            continue
        if book_id is None:
            continue
        chapter_match = CHAPTER.match(line)
        if chapter_match:
            current_chapter = int(chapter_match.group(1))
            chapters.setdefault(current_chapter, OrderedDict())
            current_verse = None
            continue
        verse_match = VERSE.match(line)
        if verse_match and current_chapter is not None:
            current_verse = int(verse_match.group(1))
            chapters[current_chapter][current_verse] = clean_text(verse_match.group(2))
            continue
        if current_chapter is not None and current_verse is not None and not line.startswith("\\"):
            chapters[current_chapter][current_verse] = clean_text(chapters[current_chapter][current_verse] + " " + line)

    return book_id, OrderedDict((chapter, verses) for chapter, verses in chapters.items() if verses)


def strip_spans(source: str, marker: str) -> str:
    return re.sub(rf"\\{marker}\b.*?\\{marker}\*", "", source, flags=re.DOTALL)


def clean_text(value: str) -> str:
    text = value
    text = re.sub(r"\\\+?w\s+([^|\\]+)(?:\|[^\\]*)?\\\+?w\*", r"\1", text)
    text = re.sub(r"\\fig\b.*?\\fig\*", "", text)
    text = re.sub(r"\\(?:wj|qs|qt|nd)\*?", "", text)
    text = re.sub(r"\\\+?(?:add|bd|bk|em|it|pn|sc|tl)\*?", "", text)
    text = re.sub(r"\\[a-z0-9+]+\*?", "", text)
    text = re.sub(r"\s+", " ", text).strip()
    text = re.sub(r"\s+([,.;:!?])", r"\1", text)
    return text


def write_translation(args: argparse.Namespace, records: OrderedDict[str, OrderedDict[int, OrderedDict[int, str]]]) -> None:
    args.output.mkdir(parents=True, exist_ok=True)
    manifest = OrderedDict(
        [
            ("id", args.translation_id),
            ("displayName", args.display_name),
            ("language", args.language),
            ("license", args.license),
            ("attribution", args.attribution),
            ("textDirection", "ltr"),
            ("bookOrder", list(BOOKS.keys())),
            ("audioManifestId", args.audio_manifest_id or f"{args.translation_id}-default"),
        ]
    )
    write_json(args.output / "translation.json", manifest)

    index_books = OrderedDict()
    for book_id, chapters in records.items():
        index_books[book_id] = list(chapters.keys())
        book_dir = args.output / "books" / book_id
        book_dir.mkdir(parents=True, exist_ok=True)
        for chapter_number, verses in chapters.items():
            chapter_payload = OrderedDict(
                [
                    ("translationId", args.translation_id),
                    ("bookId", book_id),
                    ("chapter", chapter_number),
                    ("verses", OrderedDict((str(number), text) for number, text in verses.items())),
                ]
            )
            write_json(book_dir / f"{chapter_number:03d}.json", chapter_payload)

    write_json(args.output / "index.json", OrderedDict([("translationId", args.translation_id), ("books", index_books)]))


def write_json(path: Path, payload: OrderedDict) -> None:
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
