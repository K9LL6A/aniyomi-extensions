Updated implementation notes: added Streamtape and OK.ru best-effort extractors, and wired them into video parsing.

What I changed:
- Added StreamtapeExtractor and OkRuExtractor. These attempt to resolve direct mp4/m3u8 links from embed pages and fall back to the embed URL when none are found.
- Added SAMPLE_TESTS.md with sample detail pages and a manual testing checklist for Aniyomi.

Next extractor candidates (if failures reported):
- Fembed / Memembed / Vidstream
- Dood / Streamlare / Vidcloud

After you test, send any failing host URLs and I will implement more detailed extractors as needed.
