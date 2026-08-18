# Kurdcinama (implementation)

This branch includes an initial implementation of a Kurdcinama Aniyomi anime extension.

Status
- Basic ParsedAnimeHttpSource implementation that:
  - Scans the homepage for links to movies/series (moves-details.aspx) for popular/latest
  - Uses Search.aspx with a few query patterns
  - Parses detail pages for title, thumbnail, description
  - Builds an episode list by scanning the detail page for episode/part links or fallback to single "Full Movie" episode
  - Extracts embedded iframes and video <source> tags as playable Video entries

Limitations & next steps
- Some video hosts (StreamTape, OK.ru, etc.) may require specialized extractor logic to get direct MP4 links. The current implementation returns embed/host URLs which Aniyomi players or installed host extractors may handle; if not, we can add specific extractors.
- Search query parameter names may need adjustment depending on how the site expects the form data. If search returns no results, we can adapt the request to POST with form data matching the site's search input name.
- Selectors are written defensively with multiple fallbacks; they may still need fine-tuning after testing in-app.

How I tested
- I implemented a best-effort parser; I have not executed runtime tests in this environment. Please run the extension in Aniyomi (build the project) and report any selector mismatches or hosts that need specialized extraction.

If you want, I can now open a PR against the repo default branch with these changes and add more host extractors (StreamTape, OK.ru, Vidstream, etc.) — say "Open PR" and I will create the pull request.