# KJV Import Utility

`kjv_to_livingword.py` converts verse-marked KJV text into Living Word translation resources.

Expected source shape:

```text
The Gospel According to Saint John
3:16 For God so loved the world...
3:17 For God sent not his Son...
```

The parser recognizes common KJV book headings, keeps the 66-book Protestant canon, and ignores unrecognized books. Inspect the generated files after every import.

Usage:

```powershell
python tools\kjv-import\kjv_to_livingword.py path\to\kjv.txt src\main\resources\data\livingword\bible\kjv
```
