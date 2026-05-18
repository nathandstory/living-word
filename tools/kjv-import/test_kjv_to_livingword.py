import importlib.util
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("kjv_to_livingword.py")
SPEC = importlib.util.spec_from_file_location("kjv_to_livingword", SCRIPT_PATH)
kjv_to_livingword = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(kjv_to_livingword)


class KJVImportTest(unittest.TestCase):
    def test_parses_project_gutenberg_numeric_references(self):
        source = """
        *** START OF THE PROJECT GUTENBERG EBOOK THE BIBLE, KING JAMES ***
        01:001:001 In the beginning God created the heaven and the earth.
        01:001:002 And the earth was without form, and void;
        43:003:016 For God so loved the world,
        43:003:017 For God sent not his Son into the world to condemn the world;
        *** END OF THE PROJECT GUTENBERG EBOOK THE BIBLE, KING JAMES ***
        """

        records = kjv_to_livingword.parse_text(source)

        self.assertEqual("In the beginning God created the heaven and the earth.", records["genesis"][1][1])
        self.assertEqual("And the earth was without form, and void;", records["genesis"][1][2])
        self.assertEqual("For God so loved the world,", records["john"][3][16])
        self.assertEqual("For God sent not his Son into the world to condemn the world;", records["john"][3][17])

    def test_normalizes_typographic_dashes_to_ascii(self):
        text = "Yet now, if thou wilt forgive their sin\u2014; and if not, blot me"
        mojibake_text = "Yet now, if thou wilt forgive their sin\u00e2\u20ac\u201d; and if not, blot me"

        self.assertEqual("Yet now, if thou wilt forgive their sin--; and if not, blot me", kjv_to_livingword.clean_text(text))
        self.assertEqual("Yet now, if thou wilt forgive their sin--; and if not, blot me", kjv_to_livingword.clean_text(mojibake_text))


if __name__ == "__main__":
    unittest.main()
