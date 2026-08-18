# Kurdcinama (scaffold)

This module scaffolds an Aniyomi/Tachiyomi anime extension for kurdcinama.com.

Defaults used:
- Domain: https://kurdcinama.com
- Extension name: KurdCinama
- Language: ku
- Package id: eu.kanade.tachiyomi.extension.anime.kurdcinama
- Content: movies + series (episodes when present)

What’s included in this scaffold:
- A Kotlin placeholder file where the real source implementation will go.
- This README with sample URLs and next steps.

Next steps I will take after you confirm: implement the HttpSource subclass, parsing for listings/search/details/episodes, and video extractors using live pages from the site.
