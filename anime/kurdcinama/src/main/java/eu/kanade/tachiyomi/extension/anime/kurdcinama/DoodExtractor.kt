package eu.kanade.tachiyomi.extension.anime.kurdcinama

import eu.kanade.tachiyomi.animesource.model.Video
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Best-effort Dood extractor.
 *
 * Dood often provides a short player page that contains a direct video link or an HLS manifest.
 * We try to fetch and extract those links. Falls back to the embed URL if nothing is found.
 */
object DoodExtractor {
    private val urlRegex = Regex("(https?://[^"]+?\\.(?:mp4|m3u8)(?:\\?[^\"'\\s<>]*)?)")
    private val fileKeyRegex = Regex("[\"']file[\"']\s*[:=]\s*[\"'](https?://[^\"']+?\\.(?:mp4|m3u8)[^\"']*)[\"']")

    fun extract(client: OkHttpClient, url: String, referer: String? = null): List<Video> {
        try {
            val reqBuilder = Request.Builder().url(url)
            if (referer != null) reqBuilder.header("Referer", referer)
            reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            val res = client.newCall(reqBuilder.build()).execute()
            val body = res.body?.string() ?: return listOf(Video(url, "Dood Embed", url))

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
            // ignore
        }
        return listOf(Video(url, "Dood Embed", url))
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
