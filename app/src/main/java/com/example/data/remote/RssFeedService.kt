package com.example.data.remote

import android.util.Xml
import com.example.data.db.NewsArticleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.TimeUnit

class RssFeedService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchFeed(
        sourceId: Long,
        sourceName: String,
        feedUrl: String
    ): List<NewsArticleEntity> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(feedUrl)
            .header("User-Agent", "HWidget-Android/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            parseXml(body, sourceId, sourceName)
        }
    }

    private fun parseXml(
        xml: String,
        sourceId: Long,
        sourceName: String
    ): List<NewsArticleEntity> {
        if (xml.isBlank()) return emptyList()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val articles = mutableListOf<NewsArticleEntity>()
        var insideItem = false
        var title = ""
        var link = ""
        var summary = ""
        var pubDate = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && articles.size < 10) {
            val tagName = parser.name?.lowercase().orEmpty()
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName == "item" || tagName == "entry") {
                        insideItem = true
                        title = ""
                        link = ""
                        summary = ""
                        pubDate = ""
                    } else if (insideItem) {
                        when (tagName) {
                            "title" -> title = safeNextText(parser)
                            "link" -> {
                                val href = parser.getAttributeValue(null, "href")
                                link = if (!href.isNullOrBlank()) href else safeNextText(parser)
                            }
                            "description", "summary", "content" -> {
                                if (summary.isBlank()) {
                                    summary = stripHtml(safeNextText(parser))
                                }
                            }
                            "pubdate", "published", "updated" -> {
                                if (pubDate.isBlank()) pubDate = safeNextText(parser)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if ((tagName == "item" || tagName == "entry") && insideItem) {
                        insideItem = false
                        if (title.isNotBlank()) {
                            articles.add(
                                NewsArticleEntity(
                                    sourceId = sourceId,
                                    sourceName = sourceName,
                                    title = title.trim(),
                                    summary = summary.ifBlank { "來自 $sourceName 的即時訂閱消息" }.take(180),
                                    link = link.trim(),
                                    publishedAtText = pubDate.take(22).ifBlank { "剛剛更新" }
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return articles
    }

    private fun safeNextText(parser: XmlPullParser): String {
        return runCatching {
            if (parser.next() == XmlPullParser.TEXT) parser.text.orEmpty() else ""
        }.getOrDefault("")
    }

    private fun stripHtml(input: String): String =
        input.replace(Regex("<[^>]*>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
}
