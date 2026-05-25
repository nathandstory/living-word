import argparse
import json
import os
import re
import shutil
import site
import subprocess
import sys
import time
import urllib.request
from concurrent.futures import FIRST_COMPLETED, ThreadPoolExecutor, wait
from dataclasses import dataclass
from difflib import SequenceMatcher
from pathlib import Path


BOOK_CODES = {
    "genesis": "GEN",
    "exodus": "EXO",
    "leviticus": "LEV",
    "numbers": "NUM",
    "deuteronomy": "DEU",
    "joshua": "JOS",
    "judges": "JDG",
    "ruth": "RUT",
    "1_samuel": "1SA",
    "2_samuel": "2SA",
    "1_kings": "1KI",
    "2_kings": "2KI",
    "1_chronicles": "1CH",
    "2_chronicles": "2CH",
    "ezra": "EZR",
    "nehemiah": "NEH",
    "esther": "EST",
    "job": "JOB",
    "psalms": "PSA",
    "proverbs": "PRO",
    "ecclesiastes": "ECC",
    "song_of_solomon": "SNG",
    "isaiah": "ISA",
    "jeremiah": "JER",
    "lamentations": "LAM",
    "ezekiel": "EZK",
    "daniel": "DAN",
    "hosea": "HOS",
    "joel": "JOL",
    "amos": "AMO",
    "obadiah": "OBA",
    "jonah": "JON",
    "micah": "MIC",
    "nahum": "NAM",
    "habakkuk": "HAB",
    "zephaniah": "ZEP",
    "haggai": "HAG",
    "zechariah": "ZEC",
    "malachi": "MAL",
    "matthew": "MAT",
    "mark": "MRK",
    "luke": "LUK",
    "john": "JHN",
    "acts": "ACT",
    "romans": "ROM",
    "1_corinthians": "1CO",
    "2_corinthians": "2CO",
    "galatians": "GAL",
    "ephesians": "EPH",
    "philippians": "PHP",
    "colossians": "COL",
    "1_thessalonians": "1TH",
    "2_thessalonians": "2TH",
    "1_timothy": "1TI",
    "2_timothy": "2TI",
    "titus": "TIT",
    "philemon": "PHM",
    "hebrews": "HEB",
    "james": "JAS",
    "1_peter": "1PE",
    "2_peter": "2PE",
    "1_john": "1JN",
    "2_john": "2JN",
    "3_john": "3JN",
    "jude": "JUD",
    "revelation": "REV",
}

KJV_AUDIO_TREASURE_PATHS = {
    "genesis": ("01_Genesis", False),
    "exodus": ("02_Exodus", False),
    "leviticus": ("03_Leviticus", False),
    "numbers": ("04_Numbers", False),
    "deuteronomy": ("05_Deuteronomy", False),
    "joshua": ("06_Joshua", False),
    "judges": ("07_Judges", False),
    "ruth": ("08_Ruth", False),
    "1_samuel": ("09_1Samuel", False),
    "2_samuel": ("10_2Samuel", False),
    "1_kings": ("11_1Kings", False),
    "2_kings": ("12_2Kings", False),
    "1_chronicles": ("13_1Chronicles", False),
    "2_chronicles": ("14_2Chronicles", False),
    "ezra": ("15_Ezra", False),
    "nehemiah": ("16_Nehemiah", False),
    "esther": ("17_Esther", False),
    "job": ("18_Job", False),
    "psalms": ("19_Psalms", False),
    "proverbs": ("20_Proverbs", False),
    "ecclesiastes": ("21_Ecclesiastes", False),
    "song_of_solomon": ("22_Song_of_Soloman", False),
    "isaiah": ("23_Isaiah", False),
    "jeremiah": ("24_Jeremiah", False),
    "lamentations": ("25_Lamentations", False),
    "ezekiel": ("26_Ezekiel", False),
    "daniel": ("27_Daniel", False),
    "hosea": ("28_Hosea", False),
    "joel": ("29_Joel", False),
    "amos": ("30_Amos", False),
    "obadiah": ("31_Obadiah", False),
    "jonah": ("32_Jonah", False),
    "micah": ("33_Micah", False),
    "nahum": ("34_Nahum", False),
    "habakkuk": ("35_Habakkuk", False),
    "zephaniah": ("36_Zephaniah", False),
    "haggai": ("37_Haggai", False),
    "zechariah": ("38_Zechariah", False),
    "malachi": ("39_Malachi", False),
    "matthew": ("40_Matthew", False),
    "mark": ("41_Mark", False),
    "luke": ("42_Luke", False),
    "john": ("43_John", False),
    "acts": ("44_Acts", False),
    "romans": ("45_Romans", False),
    "1_corinthians": ("46_1Corinthians", False),
    "2_corinthians": ("47_2Corinthians", False),
    "galatians": ("48_Galatians", False),
    "ephesians": ("49_Ephesians", False),
    "philippians": ("50_Philippians", False),
    "colossians": ("51_Colossians", False),
    "1_thessalonians": ("52_1Thessalonians", False),
    "2_thessalonians": ("53_2Thessalonians", False),
    "1_timothy": ("54_1Timothy", False),
    "2_timothy": ("55_2Timothy", False),
    "titus": ("56_Titus", False),
    "philemon": ("57_Philemon", True),
    "hebrews": ("58_Hebrews", False),
    "james": ("59_James", False),
    "1_peter": ("60_1Peter", False),
    "2_peter": ("61_2Peter", False),
    "1_john": ("62_1John", False),
    "2_john": ("63_2John", True),
    "3_john": ("64_3John", True),
    "jude": ("65_Jude", True),
    "revelation": ("66_Revelation", False),
}

WORD_RE = re.compile(r"[A-Za-z0-9]+(?:[''][A-Za-z0-9]+)?")


@dataclass(frozen=True)
class ChapterTask:
    book_id: str
    code: str
    chapter: int
    bible_json: Path
    output_json: Path
    mp3_path: Path
    wav_path: Path


def add_cuda_dll_directories() -> None:
    for site_dir in [Path(path) for path in site.getsitepackages()]:
        for rel in (
            "nvidia/cublas/bin",
            "nvidia/cudnn/bin",
            "nvidia/cuda_runtime/bin",
            "nvidia/cuda_nvrtc/bin",
        ):
            directory = site_dir / rel
            if directory.exists():
                os.add_dll_directory(str(directory))
                os.environ["PATH"] = str(directory) + os.pathsep + os.environ.get("PATH", "")


def fix_text(text: str) -> str:
    return (
        text.replace("â€™", "'")
        .replace("â€˜", "'")
        .replace("â€œ", '"')
        .replace("â€", '"')
        .replace("â€”", " ")
        .replace("â€“", " ")
        .replace("Â", "")
    )


def normalize_word(text: str) -> str:
    text = fix_text(text).lower().replace("'", "'")
    return re.sub(r"[^a-z0-9']+", "", text)


def bible_words(chapter_json: Path) -> list[dict]:
    chapter = json.loads(chapter_json.read_text(encoding="utf-8"))
    words = []
    for verse_key in sorted(chapter["verses"], key=lambda key: int(key)):
        verse = int(verse_key)
        for match in WORD_RE.finditer(fix_text(chapter["verses"][verse_key])):
            raw = match.group(0)
            normalized = normalize_word(raw)
            if normalized:
                words.append({"verse": verse, "text": raw, "norm": normalized})
    return words


def audio_words(segments: list) -> list[dict]:
    words = []
    for segment in segments:
        for word in segment.get("words", []):
            raw = word.get("word", "").strip()
            normalized = normalize_word(raw)
            if normalized:
                words.append(
                    {
                        "text": raw,
                        "norm": normalized,
                        "start": float(word["start"]),
                        "end": float(word["end"]),
                    }
                )
    return words


def align_words(bible: list[dict], audio: list[dict]) -> dict[int, int]:
    bible_norm = [word["norm"] for word in bible]
    audio_norm = [word["norm"] for word in audio]
    matcher = SequenceMatcher(a=bible_norm, b=audio_norm, autojunk=False)
    assignments = {}
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == "equal":
            for offset in range(i2 - i1):
                assignments[i1 + offset] = j1 + offset
            continue
        if tag != "replace":
            continue
        used = set()
        for bible_index in range(i1, i2):
            best_audio_index = None
            best_score = 0.0
            for audio_index in range(j1, j2):
                if audio_index in used:
                    continue
                score = SequenceMatcher(None, bible_norm[bible_index], audio_norm[audio_index]).ratio()
                if bible_norm[bible_index] == audio_norm[audio_index]:
                    score = 1.0
                if score > best_score:
                    best_score = score
                    best_audio_index = audio_index
            if best_audio_index is not None and best_score >= 0.82:
                assignments[bible_index] = best_audio_index
                used.add(best_audio_index)
    return assignments


def build_sidecar(task: ChapterTask, transcript: dict, args: argparse.Namespace) -> tuple[dict, dict]:
    bible = bible_words(task.bible_json)
    audio = audio_words(transcript["segments"])
    assignments = align_words(bible, audio)
    match_ratio = len(assignments) / max(1, len(bible))
    if match_ratio < args.minimum_match_ratio:
        return {}, {
            "book": task.book_id,
            "chapter": task.chapter,
            "status": "low_match",
            "matchRatio": round(match_ratio, 4),
            "matchedWords": len(assignments),
            "bibleWords": len(bible),
            "audioWords": len(audio),
        }

    max_verse = max((word["verse"] for word in bible), default=0)
    verses = {}
    words_by_verse = {str(verse): [] for verse in range(1, max_verse + 1)}
    last_time = 0.0
    for verse in range(1, max_verse + 1):
        verse_indices = [index for index, word in enumerate(bible) if word["verse"] == verse]
        mapped_indices = [index for index in verse_indices if index in assignments]
        if mapped_indices:
            start = audio[assignments[mapped_indices[0]]]["start"]
            start = max(start, last_time)
            verses[str(verse)] = round(start, 3)
            last_time = start
        else:
            verses[str(verse)] = round(last_time, 3)
        for index in mapped_indices:
            bible_word = bible[index]
            audio_word = audio[assignments[index]]
            words_by_verse[str(verse)].append(
                {
                    "text": bible_word["text"],
                    "start": round(audio_word["start"], 3),
                    "end": round(max(audio_word["end"], audio_word["start"] + 0.05), 3),
                }
            )

    payload = {
        "source": source_description(args),
        "quality": "machine-generated",
        "audioDuration": round(float(transcript.get("duration", 0.0)), 3),
        "matchRatio": round(match_ratio, 4),
        "verses": verses,
        "words": {verse: entries for verse, entries in words_by_verse.items() if entries},
    }
    return payload, {
        "book": task.book_id,
        "chapter": task.chapter,
        "status": "written",
        "matchRatio": round(match_ratio, 4),
        "matchedWords": len(assignments),
        "bibleWords": len(bible),
        "audioWords": len(audio),
    }


def discover_tasks(args: argparse.Namespace) -> list[ChapterTask]:
    bible_root = Path(args.bible_root)
    output_root = Path(args.output_root)
    cache_root = Path(args.cache_root)
    only = set(args.only or [])
    only_chapters = set(args.chapter or [])
    only_pairs = parse_only_chapters(args.only_chapter or [])
    tasks = []
    for book_id, code in BOOK_CODES.items():
        if only and book_id not in only:
            continue
        book_root = bible_root / book_id
        if not book_root.exists():
            continue
        for chapter_file in sorted(book_root.glob("*.json")):
            chapter = int(chapter_file.stem)
            if only_pairs and (book_id, chapter) not in only_pairs:
                continue
            if only_chapters and chapter not in only_chapters:
                continue
            output_json = output_root / book_id / f"{book_id}_{chapter:03}.timestamps.json"
            mp3_path = cache_root / "mp3" / book_id / f"{book_id}_{chapter:03}.mp3"
            wav_path = cache_root / "wav" / book_id / f"{book_id}_{chapter:03}.wav"
            if output_json.exists() and not args.force:
                continue
            tasks.append(ChapterTask(book_id, code, chapter, chapter_file, output_json, mp3_path, wav_path))
    return tasks


def parse_only_chapters(values: list[str]) -> set[tuple[str, int]]:
    pairs = set()
    for value in values:
        book_id, separator, chapter = value.partition(":")
        if not separator:
            raise ValueError(f"--only-chapter must use book_id:chapter, got {value!r}")
        pairs.add((book_id, int(chapter)))
    return pairs


def download(url: str, destination: Path, retries: int) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".tmp")
    for attempt in range(1, retries + 1):
        try:
            with urllib.request.urlopen(url, timeout=60) as response:
                temporary.write_bytes(response.read())
            temporary.replace(destination)
            return
        except Exception:
            if attempt == retries:
                raise
            time.sleep(min(10, attempt * 2))


def audio_url(task: ChapterTask, args: argparse.Namespace) -> str:
    base_uri = args.base_uri.rstrip("/")
    if args.audio_strategy == "helloao-bsb":
        return f"{base_uri}/{task.code}/{task.chapter}/audio/{args.narrator}.mp3"
    if args.audio_strategy == "audiotreasure-kjv":
        prefix, chapterless_single = KJV_AUDIO_TREASURE_PATHS[task.book_id]
        file_name = f"{prefix}.mp3" if chapterless_single else f"{prefix}{task.chapter:03}.mp3"
        return f"{base_uri}/{file_name}"
    raise ValueError(f"Unsupported audio strategy: {args.audio_strategy}")


def prepare_audio(task: ChapterTask, args: argparse.Namespace) -> ChapterTask:
    url = audio_url(task, args)
    if not task.mp3_path.exists() or task.mp3_path.stat().st_size == 0:
        download(url, task.mp3_path, args.retries)
    if not task.wav_path.exists() or task.wav_path.stat().st_size == 0:
        task.wav_path.parent.mkdir(parents=True, exist_ok=True)
        temporary = task.wav_path.with_suffix(".wav.tmp")
        command = [
            "ffmpeg",
            "-y",
            "-v",
            "error",
            "-err_detect",
            "ignore_err",
            "-i",
            str(task.mp3_path),
            "-ac",
            "1",
            "-ar",
            "16000",
            "-f",
            "wav",
            str(temporary),
        ]
        subprocess.run(command, check=True, stderr=subprocess.DEVNULL)
        temporary.replace(task.wav_path)
    return task


def transcribe(model, task: ChapterTask, args: argparse.Namespace) -> dict:
    segments, info = model.transcribe(
        str(task.wav_path),
        language="en",
        beam_size=args.beam_size,
        best_of=args.best_of,
        word_timestamps=True,
        vad_filter=False,
    )
    transcript = {"duration": info.duration, "segments": []}
    for segment in segments:
        transcript["segments"].append(
            {
                "start": segment.start,
                "end": segment.end,
                "text": segment.text,
                "words": [
                    {
                        "word": word.word,
                        "start": word.start,
                        "end": word.end,
                        "probability": word.probability,
                    }
                    for word in (segment.words or [])
                ],
            }
        )
    return transcript


def write_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def run_batch(args: argparse.Namespace) -> int:
    add_cuda_dll_directories()
    from faster_whisper import WhisperModel

    tasks = discover_tasks(args)
    if args.limit:
        tasks = tasks[: args.limit]
    report_path = Path(args.report)
    report_path.parent.mkdir(parents=True, exist_ok=True)
    results = []

    print(f"tasks={len(tasks)} device={args.device} model={args.model} prepare_workers={args.prepare_workers}")
    if not tasks:
        write_json(report_path, {"results": results})
        return 0

    model = WhisperModel(args.model, device=args.device, compute_type=args.compute_type)
    start_time = time.perf_counter()
    completed = 0
    failed = 0
    task_iter = iter(tasks)
    pending = {}

    with ThreadPoolExecutor(max_workers=args.prepare_workers) as executor:
        def submit_next() -> bool:
            try:
                task = next(task_iter)
            except StopIteration:
                return False
            pending[executor.submit(prepare_audio, task, args)] = task
            return True

        for _ in range(min(args.prefetch, len(tasks))):
            submit_next()

        while pending:
            done, _ = wait(pending, return_when=FIRST_COMPLETED)
            for future in done:
                task = pending.pop(future)
                try:
                    prepared_task = future.result()
                    transcript = transcribe(model, prepared_task, args)
                    sidecar, result = build_sidecar(prepared_task, transcript, args)
                    if sidecar:
                        write_json(prepared_task.output_json, sidecar)
                    results.append(result)
                    completed += 1 if result["status"] == "written" else 0
                    failed += 1 if result["status"] != "written" else 0
                    elapsed = time.perf_counter() - start_time
                    print(
                        f"[{completed + failed}/{len(tasks)}] {result['status']} "
                        f"{task.book_id} {task.chapter} match={result.get('matchRatio')} "
                        f"elapsed={elapsed:.1f}s"
                    )
                except Exception as exception:
                    failed += 1
                    result = {
                        "book": task.book_id,
                        "chapter": task.chapter,
                        "status": "error",
                        "error": repr(exception),
                    }
                    results.append(result)
                    print(f"[{completed + failed}/{len(tasks)}] error {task.book_id} {task.chapter}: {exception}")
                finally:
                    if not args.keep_wav and task.wav_path.exists():
                        task.wav_path.unlink()
                    write_json(report_path, {"results": results})
                    submit_next()

    write_json(
        report_path,
        {
            "summary": {
                "tasks": len(tasks),
                "written": completed,
                "failed": failed,
                "elapsedSeconds": round(time.perf_counter() - start_time, 3),
            },
            "results": results,
        },
    )
    return 1 if failed else 0


def source_description(args: argparse.Namespace) -> str:
    if args.source_description:
        return args.source_description
    if args.audio_strategy == "helloao-bsb":
        return f"machine-generated alignment from HelloAO BSB {args.narrator.title()} audio using faster-whisper {args.model}"
    if args.audio_strategy == "audiotreasure-kjv":
        return f"machine-generated alignment from AudioTreasure KJV voice audio using faster-whisper {args.model}"
    return f"machine-generated alignment using faster-whisper {args.model}"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate audio timing sidecars.")
    parser.add_argument("--bible-root", default="src/main/resources/data/livingword/bible/bsb/books")
    parser.add_argument("--output-root", default="src/main/resources/data/livingword/audio/bsb/default")
    parser.add_argument("--cache-root", default=str(Path(os.environ.get("TEMP", ".")) / "livingword-bsb-timing-cache"))
    parser.add_argument("--report", default="build/timing-generation/bsb-default-report.json")
    parser.add_argument("--base-uri", default="https://audio.bible.helloao.org/api/BSB")
    parser.add_argument("--audio-strategy", choices=["helloao-bsb", "audiotreasure-kjv"], default="helloao-bsb")
    parser.add_argument("--narrator", default="david")
    parser.add_argument("--source-description", default="")
    parser.add_argument("--model", default="small.en")
    parser.add_argument("--device", default="cuda")
    parser.add_argument("--compute-type", default="float16")
    parser.add_argument("--beam-size", type=int, default=1)
    parser.add_argument("--best-of", type=int, default=1)
    parser.add_argument("--prepare-workers", type=int, default=8)
    parser.add_argument("--prefetch", type=int, default=16)
    parser.add_argument("--retries", type=int, default=3)
    parser.add_argument("--minimum-match-ratio", type=float, default=0.85)
    parser.add_argument("--limit", type=int)
    parser.add_argument("--only", action="append")
    parser.add_argument("--chapter", type=int, action="append")
    parser.add_argument("--only-chapter", action="append")
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--keep-wav", action="store_true")
    return parser.parse_args()


if __name__ == "__main__":
    if not shutil.which("ffmpeg"):
        print("ffmpeg is required on PATH", file=sys.stderr)
        raise SystemExit(2)
    raise SystemExit(run_batch(parse_args()))
