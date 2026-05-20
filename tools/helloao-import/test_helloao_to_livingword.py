import importlib.util
import tempfile
import unittest
from argparse import Namespace
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("helloao_to_livingword.py")
SPEC = importlib.util.spec_from_file_location("helloao_to_livingword", SCRIPT_PATH)
helloao_to_livingword = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(helloao_to_livingword)


class HelloAOImportTest(unittest.TestCase):
    def test_flattens_poetry_text_and_ignores_notes(self):
        payload = {
            "books": [
                {
                    "id": "PSA",
                    "chapters": [
                        {
                            "chapter": {
                                "number": 1,
                                "content": [
                                    {"type": "heading", "content": ["The Two Paths"]},
                                    {
                                        "type": "verse",
                                        "number": 1,
                                        "content": [
                                            {"text": "Blessed is the man", "poem": 1},
                                            {"text": "who does not walk in the counsel of the wicked,", "poem": 2},
                                            {"noteId": 0},
                                            {"text": "or sit in the seat of mockers.", "poem": 2},
                                        ],
                                    },
                                ],
                            }
                        }
                    ],
                }
            ]
        }

        records = helloao_to_livingword.parse_complete_translation(payload)

        self.assertEqual(
            "Blessed is the man who does not walk in the counsel of the wicked, or sit in the seat of mockers.",
            records["psalms"][1][1],
        )

    def test_writes_living_word_translation_resources(self):
        payload = {
            "books": [
                {
                    "id": "PRO",
                    "chapters": [
                        {
                            "chapter": {
                                "number": 1,
                                "content": [
                                    {
                                        "type": "verse",
                                        "number": 1,
                                        "content": [
                                            {"text": "These are the proverbs of Solomon son of David,", "poem": 1},
                                            {"text": "king of Israel,", "poem": 2},
                                        ],
                                    }
                                ],
                            }
                        }
                    ],
                }
            ]
        }

        with tempfile.TemporaryDirectory() as tmp:
            output = Path(tmp)
            args = Namespace(
                output=output,
                translation_id="bsb",
                display_name="Berean Standard Bible",
                language="en_us",
                license="Public Domain",
                attribution="HelloAO",
                audio_manifest_id="default",
            )

            helloao_to_livingword.write_translation(args, helloao_to_livingword.parse_complete_translation(payload))

            chapter = (output / "books" / "proverbs" / "001.json").read_text(encoding="utf-8")
            self.assertIn('"translationId": "bsb"', chapter)
            self.assertIn('"1": "These are the proverbs of Solomon son of David, king of Israel,"', chapter)


if __name__ == "__main__":
    unittest.main()
