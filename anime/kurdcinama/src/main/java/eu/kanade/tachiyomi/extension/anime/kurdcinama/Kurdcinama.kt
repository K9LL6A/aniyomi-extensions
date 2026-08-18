package eu.kanade.tachiyomi.extension.anime.kurdcinama

import eu.kanade.tachiyomi.animeextension.extractors.ExtractorLink
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import uy.kohesive.injekt.injectLazy

/**
 * Kurdcinama extension implementation (best-effort selectors and extractors).
 *
 * Notes:
 * - This is a best-effort implementation based on the site's common patterns.
 * - The site uses ASP.NET pages like moves-details.aspx?movieid=... for details.
 * - Video extraction returns embedded iframe srcs as playable sources when possible.
 * - If some hosts require additional extraction (e.g., StreamTape, OK.ru), separate extractors can be added later.
 */
class Kurdcinama : ConfigurableAnimeSource, ParsedAnimeHttpSource() {
    override val name = "KurdCinama"
    override val baseUrl = "https://www.kurdcinama.com"
    override val lang = "ku"
    override val supportsLatest = true

    override val client: OkHttpClient = network.client
    private val json: Json by injectLazy()

    // Headers used for video requests
    private fun defaultHeaders(): Headers = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        .add("Referer", baseUrl)
        .build()

    // --- Popular / Latest ---
    override fun getPopularAnimeRequest(page: Int) = GET("$baseUrl/", defaultHeaders())

    override fun popularAnimeSelector() = "a[href*=moves-details.aspx]"

    override fun popularAnimeFromElement(element: Element): SAnime {
        val anime = SAnime.create()
        val url = element.attr("abs:href")
        val title = element.attr("title").ifEmpty { element.text() }
        val img = element.selectFirst("img")?.attr("abs:src")

        anime.setUrlWithoutDomain(url)
        anime.title = title
        anime.thumbnail_url = img
        return anime
    }

    override fun popularAnimeNextPageSelector() = null

    // Latest (reuse popular)
    override fun getLatestUpdatesRequest(page: Int) = GET(baseUrl, defaultHeaders())
    override fun latestUpdatesSelector() = popularAnimeSelector()

    override fun latestUpdatesFromElement(element: Element) = popularAnimeFromElement(element)
    override fun latestUpdatesNextPageSelector() = popularAnimeNextPageSelector()

    // --- Search ---
    override fun getSearchAnimeRequest(page: Int, query: String, filters: Array<AnimeFilter>) : Request {
        // The site exposes a Search.aspx page. Try common query param names.
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        val urlAttempts = listOf(
            "$baseUrl/Search.aspx?keyword=$q",
            "$baseUrl/Search.aspx?search=$q",
            "$baseUrl/Search.aspx?q=$q",
            "$baseUrl/?s=$q"
        )
        // Use the first form (Search.aspx?keyword=) by default
        return GET(urlAttempts.first(), defaultHeaders())
    }

    override fun searchAnimeSelector() = popularAnimeSelector()
    override fun searchAnimeFromElement(element: Element) = popularAnimeFromElement(element)
    override fun searchAnimeNextPageSelector() = popularAnimeNextPageSelector()

    // --- Details ---
    override fun animeDetailsRequest(anime: SAnime) = GET(anime.url, defaultHeaders())

    override fun animeDetailsParse(document: Document): SAnime {
        val anime = SAnime.create()
        // Title
        val title = document.selectFirst("h1, .post-title, .movie-title")?.text() ?: document.title()
        // Thumbnail
        val img = document.selectFirst("img[src*=uploads], img[src*=movies], .movie img, .post-thumb img")?.attr("abs:src")
        // Synopsis
        val desc = document.selectFirst(".summary, .description, #longdesc, .movie-desc, .post-content")?.text() ?: ""

        anime.title = title
        anime.thumbnail_url = img
        anime.description = desc
        // Keep the original URL
        anime.setUrlWithoutDomain(document.location())
        return anime
    }

    // --- Episodes ---
    override fun episodeListRequest(anime: SAnime) = GET(anime.url, defaultHeaders())

    override fun episodeListParse(document: Document): List<SEpisode> {
        // Two modes: series (multiple episode links) or movie (single "episode" pointing to the page)
        val episodes = mutableListOf<SEpisode>()

        // Find episode/part links on page
        // Common selectors: a[href*=episode], a[href*=part], a.play-episode, .episodes a
        val epLinks = document.select("a[href*=/episode], a[href*=/episode-details], a[href*=/part], a[href*=Episode], a[href*=episodeid], .episodes a, a[href*=play]")

        if (epLinks.isNotEmpty()) {
            // Build episodes list
            epLinks.forEachIndexed { idx, el ->
                val ep = SEpisode.create()
                val epUrl = el.attr("abs:href")
                val epTitle = el.text().ifEmpty { "Episode ${idx + 1}" }
                ep.name = epTitle
                ep.setUrlWithoutDomain(epUrl)
                ep.episode_number = (idx + 1).toFloat()
                episodes.add(ep)
            }
        } else {
            // Treat the current page as a single movie episode
            val ep = SEpisode.create()
            ep.name = "Full Movie"
            ep.setUrlWithoutDomain(document.location())
            ep.episode_number = 1F
            episodes.add(ep)
        }

        return episodes
    }

    // --- Videos ---
    override fun videoListRequest(episode: SEpisode) = GET(episode.url, defaultHeaders())

    override fun videoListParse(document: Document): List<Video> {
        val videos = mutableListOf<Video>()

        // Look for iframe embeds
        document.select("iframe[src]").forEach { iframe ->
            val src = iframe.attr("abs:src")
            if (src.isNotEmpty()) {
                videos.add(Video(src, "Embed", src))
            }
        }

        // Look for video source tags
        document.select("video source[src]").forEach { source ->
            val src = source.attr("abs:src")
            val quality = source.attr("res") .ifEmpty { source.attr("label") }.ifEmpty { "Video" }
            videos.add(Video(src, quality, src))
        }

        // Some pages include direct links inside anchors to hosts
        document.select("a[href]").forEach { a ->
            val href = a.attr("abs:href")
            if (href.contains("streamtape") || href.contains("ok.ru") || href.contains("openload") || href.contains("vidstream") || href.contains("membed")) {
                videos.add(Video(href, a.text().ifEmpty { "Host" }, href))
            }
        }

        // Deduplicate by url
        return videos.distinctBy { it.url }
    }

    // --- Unused parsed-source methods ---
    override fun popularAnimeFromElement(element: Element, isFeatured: Boolean): SAnime = popularAnimeFromElement(element)

    // Parser helpers (not all are used but required overrides)
    override fun getFilterList() = AnimeFilterList()

    // Not needed for Parsed source but required: provide stubs
    override fun animeDetailsSelector() = ""
    override fun episodeListSelector() = ""
    override fun videoListSelector() = ""
}
