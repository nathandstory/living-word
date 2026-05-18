#!/usr/bin/env python3
"""Convert verse-marked public-domain KJV text into Living Word resources."""

from __future__ import annotations

import argparse
import json
import re
from collections import OrderedDict
from pathlib import Path


BOOKS = OrderedDict(
    [
        ("genesis", ("Genesis", ["genesis"])),
        ("exodus", ("Exodus", ["exodus"])),
        ("leviticus", ("Leviticus", ["leviticus"])),
        ("numbers", ("Numbers", ["numbers"])),
        ("deuteronomy", ("Deuteronomy", ["deuteronomy"])),
        ("joshua", ("Joshua", ["joshua"])),
        ("judges", ("Judges", ["judges"])),
        ("ruth", ("Ruth", ["ruth"])),
        ("1_samuel", ("1 Samuel", ["1 samuel", "first samuel", "i samuel"])),
        ("2_samuel", ("2 Samuel", ["2 samuel", "second samuel", "ii samuel"])),
        ("1_kings", ("1 Kings", ["1 kings", "first kings", "i kings"])),
        ("2_kings", ("2 Kings", ["2 kings", "second kings", "ii kings"])),
        ("1_chronicles", ("1 Chronicles", ["1 chronicles", "first chronicles", "i chronicles"])),
        ("2_chronicles", ("2 Chronicles", ["2 chronicles", "second chronicles", "ii chronicles"])),
        ("ezra", ("Ezra", ["ezra"])),
        ("nehemiah", ("Nehemiah", ["nehemiah"])),
        ("esther", ("Esther", ["esther"])),
        ("job", ("Job", ["job"])),
        ("psalms", ("Psalms", ["psalms", "psalm"])),
        ("proverbs", ("Proverbs", ["proverbs"])),
        ("ecclesiastes", ("Ecclesiastes", ["ecclesiastes"])),
        ("song_of_solomon", ("Song of Solomon", ["song of solomon", "song of songs", "canticles"])),
        ("isaiah", ("Isaiah", ["isaiah"])),
        ("jeremiah", ("Jeremiah", ["jeremiah"])),
        ("lamentations", ("Lamentations", ["lamentations"])),
        ("ezekiel", ("Ezekiel", ["ezekiel"])),
        ("daniel", ("Daniel", ["daniel"])),
        ("hosea", ("Hosea", ["hosea"])),
        ("joel", ("Joel", ["joel"])),
        ("amos", ("Amos", ["amos"])),
        ("obadiah", ("Obadiah", ["obadiah"])),
        ("jonah", ("Jonah", ["jonah"])),
        ("micah", ("Micah", ["micah"])),
        ("nahum", ("Nahum", ["nahum"])),
        ("habakkuk", ("Habakkuk", ["habakkuk"])),
        ("zephaniah", ("Zephaniah", ["zephaniah"])),
        ("haggai", ("Haggai", ["haggai"])),
        ("zechariah", ("Zechariah", ["zechariah"])),
        ("malachi", ("Malachi", ["malachi"])),
        ("matthew", ("Matthew", ["matthew", "saint matthew"])),
        ("mark", ("Mark", ["mark", "saint mark"])),
        ("luke", ("Luke", ["luke", "saint luke"])),
        ("john", ("John", ["john", "saint john"])),
        ("acts", ("Acts", ["acts", "acts of the apostles"])),
        ("romans", ("Romans", ["romans"])),
        ("1_corinthians", ("1 Corinthians", ["1 corinthians", "first corinthians", "i corinthians"])),
        ("2_corinthians", ("2 Corinthians", ["2 corinthians", "second corinthians", "ii corinthians"])),
        ("galatians", ("Galatians", ["galatians"])),
        ("ephesians", ("Ephesians", ["ephesians"])),
        ("philippians", ("Philippians", ["philippians"])),
        ("colossians", ("Colossians", ["colossians"])),
        ("1_thessalonians", ("1 Thessalonians", ["1 thessalonians", "first thessalonians", "i thessalonians"])),
        ("2_thessalonians", ("2 Thessalonians", ["2 thessalonians", "second thessalonians", "ii thessalonians"])),
        ("1_timothy", ("1 Timothy", ["1 timothy", "first timothy", "i timothy"])),
        ("2_timothy", ("2 Timothy", ["2 timothy", "second timothy", "ii timothy"])),
        ("titus", ("Titus", ["titus"])),
        ("philemon", ("Philemon", ["philemon"])),
        ("hebrews", ("Hebrews", ["hebrews"])),
        ("james", ("James", ["james"])),
        ("1_peter", ("1 Peter", ["1 peter", "first peter", "i peter"])),
        ("2_peter", ("2 Peter", ["2 peter", "second peter", "ii peter"])),
        ("1_john", ("1 John", ["1 john", "first john", "i john"])),
        ("2_john", ("2 John", ["2 john", "second john", "ii john"])),
        ("3_john", ("3 John", ["3 john", "third john", "iii john"])),
        ("jude", ("Jude", ["jude"])),
        ("revelation", ("Revelation", ["revelation", "revelation of john"])),
    ]
)

ALIASES = {}
for book_id, (display_name, aliases) in BOOKS.items():
    ALIASES[display_name.lower()] = book_id
    for alias in aliases:
        ALIASES[alias] = book_id

VERSE_LINE = re.compile(r"^\s*(\d{1,3}):(\d{1,3})\s+(.*)$")
GUTENBERG_NUMERIC_VERSE_LINE = re.compile(r"^\s*(\d{2}):(\d{3}):(\d{3})\s+(.*)$")
PG_START = "*** START OF"
PG_END = "*** END OF"
GUTENBERG_BOOK_IDS = dict(enumerate(BOOKS.keys(), start=1))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--translation-id", default="kjv")
    parser.add_argument("--display-name", default="King James Version")
    parser.add_argument("--language", default="en_us")
    parser.add_argument("--license", default="Public Domain")
    parser.add_argument("--attribution", default="King James Version, public-domain source text")
    args = parser.parse_args()

    records = parse_source(args.source)
    write_translation(args, records)
    print(f"Wrote {sum(len(chapters) for chapters in records.values())} chapters to {args.output}")
    return 0


def parse_source(source: Path) -> OrderedDict[str, OrderedDict[int, OrderedDict[int, str]]]:
    return parse_text(source.read_text(encoding="utf-8"))


def parse_text(source: str) -> OrderedDict[str, OrderedDict[int, OrderedDict[int, str]]]:
    data: OrderedDict[str, OrderedDict[int, OrderedDict[int, str]]] = OrderedDict((book_id, OrderedDict()) for book_id in BOOKS)
    current_book = None
    current_chapter = None
    current_verse = None
    in_body = False

    for raw_line in source.splitlines():
        line = raw_line.strip()
        if not in_body:
            in_body = PG_START in line or bool(looks_like_book_heading(line)) or bool(GUTENBERG_NUMERIC_VERSE_LINE.match(line))
            if not in_body:
                continue
        if PG_END in line:
            break
        if not line:
            continue

        heading_book = looks_like_book_heading(line)
        if heading_book is not None:
            current_book = heading_book
            current_chapter = None
            current_verse = None
            continue

        numeric_verse_match = GUTENBERG_NUMERIC_VERSE_LINE.match(line)
        if numeric_verse_match:
            current_book = GUTENBERG_BOOK_IDS.get(int(numeric_verse_match.group(1)))
            if current_book is None:
                current_chapter = None
                current_verse = None
                continue
            current_chapter = int(numeric_verse_match.group(2))
            current_verse = int(numeric_verse_match.group(3))
            text = clean_text(numeric_verse_match.group(4))
            chapter = data[current_book].setdefault(current_chapter, OrderedDict())
            chapter[current_verse] = text
            continue

        verse_match = VERSE_LINE.match(line)
        if verse_match and current_book is not None:
            current_chapter = int(verse_match.group(1))
            current_verse = int(verse_match.group(2))
            text = clean_text(verse_match.group(3))
            chapter = data[current_book].setdefault(current_chapter, OrderedDict())
            chapter[current_verse] = text
            continue

        if current_book is not None and current_chapter is not None and current_verse is not None:
            chapter = data[current_book][current_chapter]
            chapter[current_verse] = clean_text(chapter[current_verse] + " " + line)

    return OrderedDict((book_id, chapters) for book_id, chapters in data.items() if chapters)


def looks_like_book_heading(line: str) -> str | None:
    normalized = normalize_heading(line)
    return ALIASES.get(normalized)


def normalize_heading(line: str) -> str:
    value = re.sub(r"[^A-Za-z0-9 ]+", " ", line).lower()
    value = re.sub(r"\s+", " ", value).strip()
    prefixes = [
        "the book of ",
        "the first book of ",
        "the second book of ",
        "the gospel according to ",
        "the epistle of paul the apostle to the ",
        "the first epistle general of ",
        "the second epistle general of ",
        "the third epistle general of ",
        "the general epistle of ",
        "the revelation of saint ",
    ]
    for prefix in prefixes:
        if value.startswith(prefix):
            value = value[len(prefix):]
    value = value.replace("st ", "saint ")
    return value


def clean_text(value: str) -> str:
    replacements = {
        "\u2014": "--",
        "\u2013": "-",
        "\u00e2\u20ac\u201d": "--",
    }
    text = re.sub(r"\s+", " ", value).strip()
    for old, new in replacements.items():
        text = text.replace(old, new)
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
            ("audioManifestId", f"{args.translation_id}-default"),
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
