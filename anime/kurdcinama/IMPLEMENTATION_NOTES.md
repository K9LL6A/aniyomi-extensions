# Kurdcinama (implementation notes)

I've updated the implementation on branch feat/kurdcinama-extension with improved selectors and search handling. What I changed and why:

- Improved CSS selectors for popular/latest and episode lists to capture more of the site's common markup patterns.
- Search now uses Search.aspx?keyword= as a GET by default; if the site uses POST-only search, we can fall back to a POST later after testing.
- videoListParse now:
  - Collects <video> sources, iframes, and common host links
  - Scans inline scripts for direct mp4/m3u8 URLs and returns them as Video entries
- Added comments noting that host-specific extractors (StreamTape, OK.ru, Fembed/membed) may be needed for best playback. Those can be implemented as separate classes inside the module if certain hosts fail to play in Aniyomi.

Next steps (I will do these unless you tell me otherwise):
1. Add host extractors for the most common hosts encountered on kurdcinama (StreamTape, OK.ru, Fembed/Membed/Vidstream, Dood).
2. Add unit/sample tests and verified sample URLs to README.
3. Build the extension locally and smoke test in Aniyomi; fix any parsing or playback issues found.
4. Open a PR to merge feat/kurdcinama-extension into the repo default branch with a summary and testing notes.

If you want me to proceed, tell me "Proceed"; if you prefer I open the PR automatically after implementing extractors and tests, tell me "Open PR when ready".
