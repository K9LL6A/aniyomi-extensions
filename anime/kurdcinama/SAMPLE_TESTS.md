# Kurdcinama - Test URLs and Notes

This file lists sample URLs to test the Kurdcinama extension once the branch is updated. Use these to manually verify parsing and playback in Aniyomi.

Sample detail pages (movies):
- https://www.kurdcinama.com/moves-details.aspx?movieid=2581
- https://www.kurdcinama.com/moves-details.aspx?movieid=220
- https://www.kurdcinama.com/moves-details.aspx?movieid=8997

Search page:
- https://www.kurdcinama.com/Search.aspx

Testing checklist
1. Browse popular/latest on the extension home in Aniyomi. Verify thumbnails and titles appear and links open.
2. Use search to find a title. Verify results are present and open detail pages.
3. Open a detail page and verify title, thumbnail, and synopsis are parsed.
4. Open episode list (for series) or verify full movie fallback.
5. Open an episode and tap Play. If playback fails, capture the returned video URL shown by the extension and report it.

If a host fails (playback error) provide:
- Failing detail/episode URL
- The returned host/video URL (visible in the extension video selection)
- Player error if any

I will iterate on host extractors as you report failing hosts.
