import importlib.util
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("usfm_to_livingword.py")
SPEC = importlib.util.spec_from_file_location("usfm_to_livingword", SCRIPT_PATH)
usfm_to_livingword = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(usfm_to_livingword)


class USFMImportTest(unittest.TestCase):
    def test_parses_chapters_and_verses(self):
        source = r"""
\id JHN World English Bible
\c 3
\p
\v 16 For God so loved the world, that he gave his only born Son,
\v 17 For God didn’t send his Son into the world to judge the world,
"""

        book_id, chapters = usfm_to_livingword.parse_usfm_text(source)

        self.assertEqual("john", book_id)
        self.assertEqual("For God so loved the world, that he gave his only born Son,", chapters[3][16])
        self.assertEqual("For God didn’t send his Son into the world to judge the world,", chapters[3][17])

    def test_strips_usfm_study_markup_without_losing_text(self):
        source = r"""
\id JHN World English Bible
\c 1
\p
\v 14 \w The|strong="G1722"\w* Word became flesh\f + \fr 1:14 \ft footnote text\f* and lived among us.
\v 23 \wj “Make straight the way of the Lord,”\wj* \x + \xo 1:23 \xt Isaiah 40:3\x*
"""

        book_id, chapters = usfm_to_livingword.parse_usfm_text(source)

        self.assertEqual("john", book_id)
        self.assertEqual("The Word became flesh and lived among us.", chapters[1][14])
        self.assertEqual("“Make straight the way of the Lord,”", chapters[1][23])


if __name__ == "__main__":
    unittest.main()
