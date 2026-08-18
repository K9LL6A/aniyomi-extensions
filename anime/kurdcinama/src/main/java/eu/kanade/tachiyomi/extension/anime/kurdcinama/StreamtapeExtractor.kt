package eu.kanade.tachiyomi.extension.anime.kurdcinama

import eu.kanade.tachiyomi.animesource.model.Video
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Best-effort Streamtape extractor.
 *
 * Streamtape sometimes exposes direct mp4 links or m3u8 in the embed/player HTML. This class
 * implements a pragmatic approach:
 * - Fetch the embed URL
 * - Search the HTML and script contents for mp4/m3u8 links or common "file":"..." JSON keys
 * - Return found links, or fallback to the original embed URL
 *
 * Note: Fully decoding Streamtape's obfuscated JS is outside this simple extractor. If playback
 * fails for some Streamtape URLs, we can iterate and add a JS-unpacking routine.
 */
object StreamtapeExtractor {
    private val urlRegex = Regex("(https?://[^"]+?\\.(?:mp4|m3u8)(?:\\?[^\"'\\s<>]*)?)")
    private val fileKeyRegex = Regex("[\"']file[\"']\s*[:=]\s*[\"'](https?://[^\"']+?\\.(?:mp4|m3u8)[^\"']*)[\"']")

    fun extract(client: OkHttpClient, url: String, referer: String? = null): List<Video> {
        try {
            val reqBuilder = Request.Builder().url(url)
            if (referer != null) reqBuilder.header("Referer", referer)
            reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            val res = client.newCall(reqBuilder.build()).execute()
            val body = res.body?.string() ?: return listOf(Video(url, "Streamtape Embed", url))

            val videos = mutableListOf<Video>()

            urlRegex.findAll(body).forEach { m ->
                val found = m.groupValues[1]
                videos.add(Video(found, guessQuality(found), found))
            }

            fileKeyRegex.findAll(body).forEach { m ->
                val found = m.groupValues[1]
                videos.add(Video(found, guessQuality(found), found))
            }

            val unique = videos.distinctBy { it.url }
            if (unique.isNotEmpty()) return unique
        } catch (e: Exception) {
            // ignore and fallback
        }
        return listOf(Video(url, "Streamtape Embed", url))
    }

    private fun guessQuality(url: String): String {
        return when {
            url.contains("1080") -> "1080p"
            url.contains("720") -> "720p"
            url.contains("480") -> "480p"
            url.contains("360") -> "360p"
            url.endsWith(".m3u8") -> "HLS"
            else -> "Stream"
        }
    }
}
