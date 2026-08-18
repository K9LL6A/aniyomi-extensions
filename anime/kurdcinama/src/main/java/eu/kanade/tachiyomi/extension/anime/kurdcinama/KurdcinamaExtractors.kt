package eu.kanade.tachiyomi.extension.anime.kurdcinama

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Headers
import eu.kanade.tachiyomi.animesource.model.Video

/**
 * Small collection of best-effort extractors that try to resolve MP4/M3U8 URLs from embed pages.
 *
 * These are not full, host-specific decoders (some hosts obfuscate JS heavily). Instead they:
 * - Fetch the embed/player page
 * - Search inline script blocks and HTML for direct mp4/m3u8 links
 * - Parse simple JSON player objects ("file": "<url>") and sources arrays
 * - Fall back to returning the original embed URL as a playable entry (if no direct streams found)
 *
 * This approach works for many embeds that include direct file URLs or player config JSON in the page.
 */
object KurdcinamaExtractors {

    private val urlRegex = Regex("(https?://[^"]+?\\.(?:mp4|m3u8)(?:\\?[^\"'\\s<>]*)?)")
    private val fileKeyRegex = Regex("[\"']file[\"']\s*[:=]\s*[\"'](https?://[^\"']+?\\.(?:mp4|m3u8)[^\"']*)[\"']")
    private val srcKeyRegex = Regex("[\"']src[\"']\s*[:=]\s*[\"'](https?://[^\"']+?\\.(?:mp4|m3u8)[^\"']*)[\"']")
    private val sourcesArrayRegex = Regex("sources\\s*:\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL)

    fun extract(client: OkHttpClient, url: String, referer: String? = null): List<Video> {
        try {
            val reqBuilder = Request.Builder().url(url)
            if (referer != null) reqBuilder.header("Referer", referer)
            reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            val req = reqBuilder.build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return listOf(Video(url, "Embed", url))

            val videos = mutableListOf<Video>()

            // 1) direct mp4/m3u8 urls
            urlRegex.findAll(body).forEach { m ->
                val found = m.groupValues[1]
                videos.add(Video(found, guessQuality(found), found))
            }

            // 2) file: "..." patterns
            fileKeyRegex.findAll(body).forEach { m ->
                val found = m.groupValues[1]
                videos.add(Video(found, guessQuality(found), found))
            }

            // 3) src: "..." patterns
            srcKeyRegex.findAll(body).forEach { m ->
                val found = m.groupValues[1]
                videos.add(Video(found, guessQuality(found), found))
            }

            // 4) sources: [...] arrays - try to extract file entries inside
            sourcesArrayRegex.findAll(body).forEach { m ->
                val inside = m.groupValues[1]
                urlRegex.findAll(inside).forEach { sub ->
                    val found = sub.groupValues[1]
                    videos.add(Video(found, guessQuality(found), found))
                }
            }

            // Dedupe and return
            val unique = videos.distinctBy { it.url }
            if (unique.isNotEmpty()) return unique
        } catch (e: Exception) {
            // ignore and fallback
        }

        // Fallback: return the embed page as a playable source (some players can handle embed URLs)
        return listOf(Video(url, "Embed", url))
    }

    private fun guessQuality(url: String): String {
        return when {
            url.contains("1080") -> "1080p"
            url.contains("720") -> "720p"
            url.contains("480") -> "480p"
            url.contains("360") -> "360p"
            url.endsWith(".m3u8") -> "HLS"
            else -> "Video"
        }
    }
}
