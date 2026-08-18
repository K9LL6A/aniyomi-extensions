package eu.kanade.tachiyomi.extension.anime.kurdcinama

import eu.kanade.tachiyomi.animeextension.extractors.ExtractorLink
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy

class Kurdcinama : ConfigurableAnimeSource, ParsedAnimeHttpSource() {
    override val name = "KurdCinama"
    override val baseUrl = "https://www.kurdcinama.com"
    override val lang = "ku"
    override val supportsLatest = true

    override val client: OkHttpClient = network.client
    private val json: Json by injectLazy()

    private fun defaultHeaders(): Headers = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        .add("Referer", baseUrl)
        .build()

    // --- Popular / Latest ---
    override fun getPopularAnimeRequest(page: Int) = GET("$baseUrl/", defaultHeaders())

    override fun popularAnimeSelector() = "div.movie-item a[href*=moves-details.aspx], a[href*=moves-details.aspx]"

    override fun popularAnimeFromElement(element: Element): SAnime {
        val anime = SAnime.create()
        val url = element.attr("abs:href")
        val title = element.attr("title").ifEmpty { element.selectFirst("img")?.attr("alt") ?: element.text() }
        val img = element.selectFirst("img")?.attr("abs:src")

        anime.setUrlWithoutDomain(url)
        anime.title = title
        anime.thumbnail_url = img
        return anime
    }

    override fun popularAnimeNextPageSelector() = "a.next, .pagination a:contains(>)"

    // Latest (reuse popular)
    override fun getLatestUpdatesRequest(page: Int) = GET(baseUrl, defaultHeaders())
    override fun latestUpdatesSelector() = popularAnimeSelector()
    override fun latestUpdatesFromElement(element: Element) = popularAnimeFromElement(element)
    override fun latestUpdatesNextPageSelector() = popularAnimeNextPageSelector()

    // --- Search ---
    override fun getSearchAnimeRequest(page: Int, query: String, filters: Array<AnimeFilter>): Request {
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/Search.aspx?keyword=$q"
        return GET(url, defaultHeaders())
    }

    override fun searchAnimeSelector() = popularAnimeSelector()
    override fun searchAnimeFromElement(element: Element) = popularAnimeFromElement(element)
    override fun searchAnimeNextPageSelector() = popularAnimeNextPageSelector()

    // --- Details ---
    override fun animeDetailsRequest(anime: SAnime) = GET(anime.url, defaultHeaders())

    override fun animeDetailsParse(document: Document): SAnime {
        val anime = SAnime.create()
        val title = document.selectFirst("h1, .movie-title, .post-title")?.text() ?: document.title()
        val img = document.selectFirst(".movie-thumb img, .post-thumb img, img[src*=uploads], img[src*=movies]")?.attr("abs:src")
        val descElem = document.selectFirst(".description, .movie-desc, .post-content, .summary, #longdesc")
        val desc = descElem?.text()?.trim() ?: ""

        anime.title = title
        anime.thumbnail_url = img
        anime.description = desc
        anime.setUrlWithoutDomain(document.location())
        return anime
    }

    // --- Episodes ---
    override fun episodeListRequest(anime: SAnime) = GET(anime.url, defaultHeaders())

    override fun episodeListParse(document: Document): List<SEpisode> {
        val episodes = mutableListOf<SEpisode>()

        val epSelectors = listOf(
            "div.episodes a[href*='episode'], .episodes a",
            "a[href*='part'], a[href*='episodeid']",
            "ul.episode-list li a, .season-list a",
            "a.play, a.btn-play"
        )

        var epLinks = listOf<Element>()
        for (sel in epSelectors) {
            val found = document.select(sel)
            if (found.isNotEmpty()) {
                epLinks = found
                break
            }
        }

        if (epLinks.isNotEmpty()) {
            epLinks.forEachIndexed { idx, el ->
                val ep = SEpisode.create()
                val epUrl = el.attr("abs:href")
                val text = el.text().trim()
                ep.name = text.ifEmpty { "Episode ${idx + 1}" }
                ep.setUrlWithoutDomain(epUrl)
                ep.episode_number = (idx + 1).toFloat()
                episodes.add(ep)
            }
        } else {
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

        // 1) video > source
        document.select("video source[src]").forEach { source ->
            val src = source.attr("abs:src")
            val quality = source.attr("res").ifEmpty { source.attr("label") }.ifEmpty { "Video" }
            videos.add(Video(src, quality, src))
        }

        // 2) iframes - try to resolve
        document.select("iframe[src]").forEach { iframe ->
            val src = iframe.attr("abs:src")
            if (src.isNotEmpty()) {
                val resolved = KurdcinamaExtractors.extract(client, src, document.location())
                videos.addAll(resolved)
            }
        }

        // 3) direct host links - try to resolve
        document.select("a[href]").forEach { a ->
            val href = a.attr("abs:href")
            if (href.contains("streamtape") || href.contains("streamlare") || href.contains("ok.ru") || href.contains("fembed") || href.contains("membed") || href.contains("vidstream") || href.contains("dood")) {
                val resolved = KurdcinamaExtractors.extract(client, href, document.location())
                videos.addAll(resolved)
            }
        }

        // 4) look in scripts for direct media urls
        val scripts = document.select("script").map { it.data() }.joinToString("\n")
        val urlRegex = Regex("(https?://[^\"'\\s<>]+?\\.(?:mp4|m3u8)(?:\\?[^\"'\\s<>]*)?)")
        urlRegex.findAll(scripts).forEach { m ->
            val url = m.groupValues[1]
            videos.add(Video(url, "Video", url))
        }

        return videos.distinctBy { it.url }
    }

    override fun getFilterList() = AnimeFilterList()
    override fun animeDetailsSelector() = ""
    override fun episodeListSelector() = ""
    override fun videoListSelector() = ""
}
