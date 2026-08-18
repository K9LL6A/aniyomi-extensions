package eu.kanade.tachiyomi.animeextension.ku.kurdsubtitle

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class KurdSubtitle : ParsedAnimeHttpSource() {

    override val name = "KurdSubtitle"
    override val baseUrl = "https://kurdsubtitle.net"
    override val lang = "ku"
    override val supportsLatest = true

    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/page/$page")
    override fun popularAnimeSelector(): String = "div.post-item"
    override fun popularAnimeFromElement(element: Element): SAnime {
        val anime = SAnime.create()
        anime.title = element.select("h3.title").text()
        anime.thumbnail_url = element.select("img").attr("src")
        anime.setUrlWithoutDomain(element.select("a").attr("href"))
        return anime
    }
    override fun popularAnimeNextPageSelector(): String = "a.next"

    override fun latestUpdatesRequest(page: Int): Request = popularAnimeRequest(page)
    override fun latestUpdatesSelector(): String = popularAnimeSelector()
    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)
    override fun latestUpdatesNextPageSelector(): String = popularAnimeNextPageSelector()

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request = GET("$baseUrl/?s=$query")
    override fun searchAnimeSelector(): String = popularAnimeSelector()
    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)
    override fun searchAnimeNextPageSelector(): String = popularAnimeNextPageSelector()

    override fun animeDetailsParse(document: Document): SAnime {
        val anime = SAnime.create()
        anime.title = document.select("h1.entry-title").text()
        anime.description = document.select("div.description").text()
        return anime
    }

    override fun episodeListSelector(): String = "div.episode-item"
    override fun episodeFromElement(element: Element): SEpisode {
        val episode = SEpisode.create()
        episode.name = element.select("span.episode-num").text()
        episode.setUrlWithoutDomain(element.select("a").attr("href"))
        return episode
    }

    override fun videoListSelector(): String = "iframe"
    override fun videoFromElement(element: Element): Video {
        val videoUrl = element.attr("src")
        return Video(videoUrl, "Server", videoUrl)
    }
    override fun videoUrlParse(document: Document): String = ""
}
