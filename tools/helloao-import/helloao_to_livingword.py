#!/usr/bin/env python3
"""Convert a HelloAO complete Bible JSON payload into Living Word resources."""

from __future__ import annotations

import argparse
import json
import re
from collections import OrderedDict
from pathlib import Path
from typing import Any


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

HELLOAO_BOOK_IDS = {
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

    payload = json.loads(args.source.read_text(encoding="utf-8"))
    records = parse_complete_translation(payload)
    write_translation(args, records)
    print(f"Wrote {sum(len(chapters) for chapters in records.values())} chapters to {args.output}")
    return 0


def parse_complete_translation(payload: dict[str, Any]) -> OrderedDict[str, OrderedDict[int, OrderedDict[int, str]]]:
    parsed = {}
    for book in payload.get("books", []):
        book_id = HELLOAO_BOOK_IDS.get(book.get("id"))
        if book_id is None:
            continue
        chapters: OrderedDict[int, OrderedDict[int, str]] = OrderedDict()
        for chapter_payload in book.get("chapters", []):
            chapter = chapter_payload.get("chapter", {})
            chapter_number = chapter.get("number")
            if not isinstance(chapter_number, int):
                continue
            verses = parse_chapter_content(chapter.get("content", []))
            if verses:
                chapters[chapter_number] = verses
        parsed[book_id] = chapters
    return OrderedDict((book_id, parsed[book_id]) for book_id in BOOKS if book_id in parsed)


def parse_chapter_content(content: list[Any]) -> OrderedDict[int, str]:
    verses: OrderedDict[int, str] = OrderedDict()
    for entry in content:
        if not isinstance(entry, dict) or entry.get("type") != "verse":
            continue
        number = entry.get("number")
        if not isinstance(number, int):
            continue
        text = flatten_content(entry.get("content", []))
        if text:
            verses[number] = text
    return verses


def flatten_content(content: list[Any]) -> str:
    parts: list[str] = []
    for item in content:
        if isinstance(item, str):
            parts.append(item)
        elif isinstance(item, dict) and isinstance(item.get("text"), str):
            parts.append(item["text"])
    return clean_text(" ".join(parts))


def clean_text(value: str) -> str:
    text = re.sub(r"\s+", " ", value).strip()
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
