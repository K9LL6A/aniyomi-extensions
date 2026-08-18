Updated: added Fembed, Dood, Vidcloud extractors and wired the core parser to route embeds/host links to the appropriate extractor.

What I did now:
- Implemented FembedExtractor, DoodExtractor, VidcloudExtractor (best-effort: fetch page, regex scan for mp4/m3u8 or JSON keys)
- Updated Kurdcinama.kt to call specific extractors for known hosts and fall back to a general extractor for unknown embeds
- Added SAMPLE_TESTS.md earlier to help manual verification

Next: run light smoke-test (manual on-device) and then open PR. If you want I can prepare a PR title/body and the command list to create the PR locally or in the web UI.
