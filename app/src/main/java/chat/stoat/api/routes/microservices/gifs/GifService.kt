package chat.stoat.api.routes.microservices.gifs

import android.net.Uri
import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.routes.microservices.january.asJanuaryProxyUrl
import chat.stoat.core.model.data.DAWNCHAT_GIFBOX_URL
import chat.stoat.core.model.data.GIFBOX_WEB_BASE
import chat.stoat.core.model.data.KLIPY_API_BASE
import chat.stoat.core.model.data.KLIPY_PUBLIC_API_KEY
import chat.stoat.core.model.data.KLIPY_WEB_BASE
import chat.stoat.core.model.schemas.Embed
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

enum class GifProvider(val id: String, val displayName: String) {
    GIFBOX("gifbox", "Gifbox"),
    KLIPY("klipy", "KLIPY")
}

data class GifCategory(
    val title: String,
    val query: String,
    val previewUrl: String? = null,
    val isTrendingTile: Boolean = false
)

data class GifItem(
    val id: String,
    val slug: String? = null,
    val title: String,
    val previewUrl: String,
    val previewIsVideo: Boolean,
    val sendUrl: String,
    val width: Int,
    val height: Int,
    val provider: GifProvider,
    val videoUrl: String? = null,
    val gifUrl: String? = null
)

data class ResolvedGifMedia(
    val mediaUrl: String,
    val fallbackImageUrl: String? = null,
    val originalUrl: String? = null,
    val width: Int = 498,
    val height: Int = 280,
    val isVideo: Boolean = false
) {
    val aspectRatio: Float
        get() {
            if (width <= 0 || height <= 0) return 16f / 9f
            val ratio = width.toFloat() / height.toFloat()
            return if (ratio.isNaN() || ratio.isInfinite() || ratio <= 0.1f) 1f else ratio.coerceIn(0.35f, 3.5f)
        }
}

object GifService {
    private val mediaCache = ConcurrentHashMap<String, ResolvedGifMedia>()

    private val gifHostSuffixes = listOf(
        "gifbox.me",
        "gifbox.dawn-chat.com",
        "klipy.com",
        "tenor.com",
        "giphy.com"
    )

    fun cacheResolvedMedia(url: String, media: ResolvedGifMedia) {
        mediaCache[url.trim()] = media
    }

    fun getCachedMedia(url: String): ResolvedGifMedia? = mediaCache[url.trim()]

    fun isGifProviderUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        val cleaned = rawUrl.trim()
        val host = runCatching { Uri.parse(cleaned).host?.lowercase() }.getOrNull() ?: return false
        if (gifHostSuffixes.any { host == it || host.endsWith(".$it") }) {
            return true
        }
        val path = runCatching { Uri.parse(cleaned).path?.lowercase() }.getOrNull().orEmpty()
        return path.endsWith(".gif")
    }

    fun isVideoMediaUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        val path = rawUrl.substringBefore('?').substringBefore('#').lowercase()
        return path.endsWith(".mp4") || path.endsWith(".webm")
    }

    fun isAnimatedImageMediaUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        val path = rawUrl.substringBefore('?').substringBefore('#').lowercase()
        return path.endsWith(".gif") || path.endsWith(".webp")
    }

    /**
     * If a message's content consists solely of a single GIF link (or angle-bracketed `<https://...>` link)
     * from Gifbox, Klipy, Tenor, Giphy, or a direct `.gif` URL, returns that URL so the message renderer
     * can hide the raw URL text and render the borderless GIF embed.
     */
    fun extractStandaloneGifUrl(content: String?): String? {
        if (content.isNullOrBlank()) return null
        val trimmed = content.trim()
        if (trimmed.contains('\n') || trimmed.contains(' ') || trimmed.contains('\t')) {
            return null
        }
        val unwrapped = trimmed.removePrefix("<").removeSuffix(">").trim()
        if (!unwrapped.startsWith("http://", ignoreCase = true) &&
            !unwrapped.startsWith("https://", ignoreCase = true)
        ) {
            return null
        }
        return if (isGifProviderUrl(unwrapped)) unwrapped else null
    }

    fun isGifEmbed(embed: Embed): Boolean {
        if (embed.special?.type?.equals("GIF", ignoreCase = true) == true) {
            return true
        }
        val candidateUrl = embed.originalURL ?: embed.url
        return when (embed.type) {
            "Website" -> {
                isGifProviderUrl(candidateUrl) ||
                    isGifProviderUrl(embed.video?.url) ||
                    isGifProviderUrl(embed.image?.url)
            }
            "Image", "Video" -> {
                isGifProviderUrl(candidateUrl) ||
                    isAnimatedImageMediaUrl(embed.url)
            }
            else -> false
        }
    }

    fun resolveGifMediaFromEmbed(embed: Embed): ResolvedGifMedia? {
        if (!isGifEmbed(embed)) return null

        val originalLink = embed.originalURL ?: embed.url
        originalLink?.let { cached ->
            mediaCache[cached]?.let { return it }
        }

        val video = embed.video
        val image = embed.image

        // Prefer animated image (.gif / .webp) if available alongside or instead of video,
        // or use video (.mp4 / .webm) when provided by January (e.g., Klipy / Gifbox / Tenor).
        if (image?.url != null && isAnimatedImageMediaUrl(image.url)) {
            return ResolvedGifMedia(
                mediaUrl = asProxyIfNeeded(image.url!!),
                fallbackImageUrl = asProxyIfNeeded(image.url!!),
                originalUrl = originalLink,
                width = (image.width ?: embed.width ?: 498L).toInt().coerceAtLeast(1),
                height = (image.height ?: embed.height ?: 280L).toInt().coerceAtLeast(1),
                isVideo = false
            )
        }

        if (video?.url != null) {
            val videoUrl = video.url!!
            val fallbackImg = image?.url?.let { asProxyIfNeeded(it) }
            return ResolvedGifMedia(
                mediaUrl = videoUrl,
                fallbackImageUrl = fallbackImg,
                originalUrl = originalLink,
                width = (video.width ?: image?.width ?: embed.width ?: 498L).toInt().coerceAtLeast(1),
                height = (video.height ?: image?.height ?: embed.height ?: 280L).toInt().coerceAtLeast(1),
                isVideo = isVideoMediaUrl(videoUrl) || !isAnimatedImageMediaUrl(videoUrl)
            )
        }

        if (image?.url != null) {
            val imageUrl = image.url!!
            return ResolvedGifMedia(
                mediaUrl = asProxyIfNeeded(imageUrl),
                fallbackImageUrl = asProxyIfNeeded(imageUrl),
                originalUrl = originalLink,
                width = (image.width ?: embed.width ?: 498L).toInt().coerceAtLeast(1),
                height = (image.height ?: embed.height ?: 280L).toInt().coerceAtLeast(1),
                isVideo = isVideoMediaUrl(imageUrl)
            )
        }

        if (embed.url != null && (embed.type == "Image" || embed.type == "Video" ||
                isAnimatedImageMediaUrl(embed.url) || isVideoMediaUrl(embed.url))
        ) {
            val directUrl = embed.url!!
            val isVid = embed.type == "Video" || isVideoMediaUrl(directUrl)
            return ResolvedGifMedia(
                mediaUrl = if (isVid) directUrl else asProxyIfNeeded(directUrl),
                fallbackImageUrl = if (!isVid) asProxyIfNeeded(directUrl) else null,
                originalUrl = originalLink,
                width = (embed.width ?: 498L).toInt().coerceAtLeast(1),
                height = (embed.height ?: 280L).toInt().coerceAtLeast(1),
                isVideo = isVid
            )
        }

        return null
    }

    private fun asProxyIfNeeded(url: String): String {
        val januaryBase = StoatAPI.getJanuaryUrl()
        if (url.startsWith(januaryBase)) return url
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull().orEmpty()
        // Direct CDN URLs for Klipy and Gifbox can be loaded directly by Glide/ExoPlayer
        if (host.endsWith("klipy.com") || host.endsWith("gifbox.me") || host.endsWith("dawn-chat.com")) {
            return url
        }
        return asJanuaryProxyUrl(url)
    }

    /**
     * Resolves a standalone GIF URL (e.g., `https://klipy.com/gifs/...` or `https://gifbox.me/view/...`
     * or direct media URL) when `message.embeds` has not yet been populated by the server or lacks media.
     */
    suspend fun resolveStandaloneGifUrl(rawUrl: String): ResolvedGifMedia? {
        val url = rawUrl.trim()
        mediaCache[url]?.let { return it }

        if (isAnimatedImageMediaUrl(url)) {
            return ResolvedGifMedia(
                mediaUrl = asProxyIfNeeded(url),
                fallbackImageUrl = asProxyIfNeeded(url),
                originalUrl = url,
                isVideo = false
            ).also { mediaCache[url] = it }
        }

        if (isVideoMediaUrl(url)) {
            return ResolvedGifMedia(
                mediaUrl = url,
                originalUrl = url,
                isVideo = true
            ).also { mediaCache[url] = it }
        }

        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val host = uri.host?.lowercase().orEmpty()
        val pathSegments = uri.pathSegments.orEmpty()

        // 1. Klipy share URL: https://klipy.com/gifs/{slug}
        if ((host == "klipy.com" || host.endsWith(".klipy.com")) &&
            pathSegments.size >= 2 && pathSegments[0].equals("gifs", ignoreCase = true)
        ) {
            val slug = pathSegments[1]
            resolveKlipySlug(slug, url)?.let {
                mediaCache[url] = it
                return it
            }
        }

        // 2. Try January /embed endpoint
        runCatching {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val response = StoatHttp.get("${StoatAPI.getJanuaryUrl()}/embed?url=$encoded")
            if (response.status == HttpStatusCode.OK) {
                val embed = StoatJson.decodeFromString<Embed>(response.bodyAsText())
                resolveGifMediaFromEmbed(embed)?.let { resolved ->
                    mediaCache[url] = resolved
                    return resolved
                }
            }
        }

        // 3. Gifbox view URL fallback: https://gifbox.me/view/{slug}
        if (host == "gifbox.me" || host.endsWith(".gifbox.me") || host == "gifbox.dawn-chat.com") {
            val slug = pathSegments.lastOrNull().orEmpty()
            if (slug.isNotBlank()) {
                resolveGifboxSlug(slug, url)?.let {
                    mediaCache[url] = it
                    return it
                }
            }
        }

        return null
    }

    private suspend fun resolveKlipySlug(slug: String, originalUrl: String): ResolvedGifMedia? {
        return runCatching {
            val encodedSlug = URLEncoder.encode(slug, "UTF-8")
            val response = StoatHttp.get(
                "$KLIPY_API_BASE/$KLIPY_PUBLIC_API_KEY/gifs/items?slugs=$encodedSlug"
            )
            if (response.status != HttpStatusCode.OK) return null
            val root = StoatJson.parseToJsonElement(response.bodyAsText()).jsonObject
            val items = root["data"]?.jsonObject?.get("data")?.jsonArray ?: return null
            val first = items.firstOrNull()?.jsonObject ?: return null
            val parsed = parseKlipyGifItem(first) ?: return null
            val mediaUrl = parsed.gifUrl ?: parsed.videoUrl ?: parsed.previewUrl
            ResolvedGifMedia(
                mediaUrl = mediaUrl,
                fallbackImageUrl = parsed.gifUrl,
                originalUrl = originalUrl,
                width = parsed.width,
                height = parsed.height,
                isVideo = isVideoMediaUrl(mediaUrl)
            )
        }.getOrNull()
    }

    private suspend fun resolveGifboxSlug(slug: String, originalUrl: String): ResolvedGifMedia? {
        return runCatching {
            val searchWords = slug.substringBeforeLast('-', slug).replace('-', ' ').ifBlank { slug }
            val results = fetchGifboxGifs(searchWords)
            val match = results.firstOrNull {
                it.sendUrl.equals(originalUrl, ignoreCase = true) ||
                    it.sendUrl.endsWith(slug, ignoreCase = true) ||
                    slug.endsWith(it.id, ignoreCase = true)
            } ?: results.firstOrNull() ?: return null

            val mediaUrl = match.gifUrl ?: match.videoUrl ?: match.previewUrl
            ResolvedGifMedia(
                mediaUrl = mediaUrl,
                fallbackImageUrl = match.gifUrl,
                originalUrl = originalUrl,
                width = match.width,
                height = match.height,
                isVideo = isVideoMediaUrl(mediaUrl)
            )
        }.getOrNull()
    }

    suspend fun fetchCategories(provider: GifProvider): List<GifCategory> {
        return when (provider) {
            GifProvider.GIFBOX -> fetchGifboxCategories()
            GifProvider.KLIPY -> fetchKlipyCategories()
        }
    }

    suspend fun fetchGifs(provider: GifProvider, query: String): List<GifItem> {
        val normalizedQuery = query.trim()
        return when (provider) {
            GifProvider.GIFBOX -> fetchGifboxGifs(normalizedQuery.ifBlank { "trending" })
            GifProvider.KLIPY -> fetchKlipyGifs(normalizedQuery.ifBlank { "trending" })
        }
    }

    suspend fun fetchGifboxCategories(): List<GifCategory> {
        val response = StoatHttp.get("$DAWNCHAT_GIFBOX_URL/categories?locale=en_US")
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Gifbox categories failed: ${response.status.value}")
        }
        val body = response.bodyAsText()
        val element = StoatJson.parseToJsonElement(body)
        val categories = mutableListOf(
            GifCategory(
                title = "Trending GIFs",
                query = "trending",
                previewUrl = null,
                isTrendingTile = true
            )
        )

        val array: JsonArray? = when (element) {
            is JsonArray -> element
            is JsonObject -> (element["tags"] ?: element["categories"] ?: element["results"]) as? JsonArray
            else -> null
        }

        array?.forEach { item ->
            val obj = item as? JsonObject ?: return@forEach
            val title = obj["title"]?.jsonPrimitive?.contentOrNull
                ?: obj["name"]?.jsonPrimitive?.contentOrNull
                ?: obj["searchterm"]?.jsonPrimitive?.contentOrNull
                ?: return@forEach
            val query = obj["searchterm"]?.jsonPrimitive?.contentOrNull
                ?: obj["query"]?.jsonPrimitive?.contentOrNull
                ?: title
            val image = obj["image"]?.jsonPrimitive?.contentOrNull
                ?: obj["preview_url"]?.jsonPrimitive?.contentOrNull
                ?: obj["previewUrl"]?.jsonPrimitive?.contentOrNull

            categories.add(
                GifCategory(
                    title = title,
                    query = query,
                    previewUrl = image,
                    isTrendingTile = false
                )
            )
        }
        return categories
    }

    suspend fun fetchGifboxGifs(query: String): List<GifItem> {
        val endpoint = if (query.equals("trending", ignoreCase = true) || query.isBlank()) {
            "$DAWNCHAT_GIFBOX_URL/trending?locale=en_US"
        } else {
            val encoded = URLEncoder.encode(query, "UTF-8")
            "$DAWNCHAT_GIFBOX_URL/search?locale=en_US&query=$encoded"
        }

        val response = StoatHttp.get(endpoint)
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Gifbox search failed: ${response.status.value}")
        }

        val root = StoatJson.parseToJsonElement(response.bodyAsText())
        val resultsArray: JsonArray = when (root) {
            is JsonObject -> (root["results"] ?: root["data"] ?: root["items"]) as? JsonArray ?: JsonArray(emptyList())
            is JsonArray -> root
            else -> JsonArray(emptyList())
        }

        return resultsArray.mapNotNull { parseGifboxGifItem(it) }
    }

    private fun parseGifboxGifItem(element: JsonElement): GifItem? {
        val obj = element as? JsonObject ?: return null
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val title = obj["title"]?.jsonPrimitive?.contentOrNull
            ?: obj["content_description"]?.jsonPrimitive?.contentOrNull
            ?: "Gifbox GIF"
        val itemUrl = obj["url"]?.jsonPrimitive?.contentOrNull
            ?: obj["itemurl"]?.jsonPrimitive?.contentOrNull
        val formats = obj["media_formats"] as? JsonObject

        fun extractFormat(name: String): Triple<String, Int, Int>? {
            val fmt = formats?.get(name) as? JsonObject ?: return null
            val fmtUrl = fmt["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
            val dims = fmt["dims"] as? JsonArray
            val w = dims?.getOrNull(0)?.jsonPrimitive?.intOrNull ?: fmt["width"]?.jsonPrimitive?.intOrNull ?: 498
            val h = dims?.getOrNull(1)?.jsonPrimitive?.intOrNull ?: fmt["height"]?.jsonPrimitive?.intOrNull ?: 280
            return Triple(fmtUrl, w.coerceAtLeast(1), h.coerceAtLeast(1))
        }

        val gifFormat = extractFormat("tinygif")
            ?: extractFormat("gif")
            ?: extractFormat("mediumgif")
            ?: extractFormat("webp")
            ?: extractFormat("tinywebp")

        val videoFormat = extractFormat("tinywebm")
            ?: extractFormat("webm")
            ?: extractFormat("tinymp4")
            ?: extractFormat("mp4")

        val primaryPreview = gifFormat ?: videoFormat ?: return null
        val previewIsVideo = gifFormat == null && isVideoMediaUrl(primaryPreview.first)
        val sendUrl = itemUrl?.takeIf { it.isNotBlank() }
            ?: gifFormat?.first
            ?: videoFormat?.first
            ?: "$GIFBOX_WEB_BASE/view/$id"

        val item = GifItem(
            id = id,
            slug = id,
            title = title,
            previewUrl = primaryPreview.first,
            previewIsVideo = previewIsVideo,
            sendUrl = sendUrl,
            width = primaryPreview.second,
            height = primaryPreview.third,
            provider = GifProvider.GIFBOX,
            videoUrl = videoFormat?.first,
            gifUrl = gifFormat?.first
        )

        val resolvedMediaUrl = gifFormat?.first ?: videoFormat?.first ?: primaryPreview.first
        cacheResolvedMedia(
            sendUrl,
            ResolvedGifMedia(
                mediaUrl = resolvedMediaUrl,
                fallbackImageUrl = gifFormat?.first,
                originalUrl = sendUrl,
                width = primaryPreview.second,
                height = primaryPreview.third,
                isVideo = isVideoMediaUrl(resolvedMediaUrl)
            )
        )

        return item
    }

    suspend fun fetchKlipyCategories(): List<GifCategory> {
        val encodedKey = URLEncoder.encode(KLIPY_PUBLIC_API_KEY, "UTF-8")
        val response = StoatHttp.get("$KLIPY_API_BASE/$encodedKey/gifs/categories?locale=en_US")
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("KLIPY categories failed: ${response.status.value}")
        }
        val root = StoatJson.parseToJsonElement(response.bodyAsText()).jsonObject
        val categoriesArray = root["data"]?.jsonObject?.get("categories") as? JsonArray

        val result = mutableListOf(
            GifCategory(
                title = "Trending GIFs",
                query = "trending",
                previewUrl = null,
                isTrendingTile = true
            )
        )

        categoriesArray?.forEach { elem ->
            val obj = elem as? JsonObject ?: return@forEach
            val category = obj["category"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val query = obj["query"]?.jsonPrimitive?.contentOrNull ?: category
            val previewUrl = obj["preview_url"]?.jsonPrimitive?.contentOrNull
            result.add(
                GifCategory(
                    title = category,
                    query = query,
                    previewUrl = previewUrl,
                    isTrendingTile = false
                )
            )
        }
        return result
    }

    suspend fun fetchKlipyGifs(query: String): List<GifItem> {
        val encodedKey = URLEncoder.encode(KLIPY_PUBLIC_API_KEY, "UTF-8")
        val isTrending = query.equals("trending", ignoreCase = true) || query.isBlank()
        val path = if (isTrending) "trending" else "search"
        val queryParam = if (isTrending) "" else "&q=${URLEncoder.encode(query, "UTF-8")}"
        val url = "$KLIPY_API_BASE/$encodedKey/gifs/$path?page=1&per_page=24&locale=us&content_filter=high$queryParam"

        val response = StoatHttp.get(url)
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("KLIPY search failed: ${response.status.value}")
        }

        val root = StoatJson.parseToJsonElement(response.bodyAsText()).jsonObject
        if (root["result"]?.jsonPrimitive?.contentOrNull == "false") {
            throw IllegalStateException("KLIPY search returned result=false")
        }
        val itemsArray = root["data"]?.jsonObject?.get("data") as? JsonArray ?: JsonArray(emptyList())
        return itemsArray.mapNotNull { elem ->
            (elem as? JsonObject)?.let { parseKlipyGifItem(it) }
        }
    }

    private fun parseKlipyGifItem(obj: JsonObject): GifItem? {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull
            ?: obj["slug"]?.jsonPrimitive?.contentOrNull
            ?: return null
        val slug = obj["slug"]?.jsonPrimitive?.contentOrNull
        val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "KLIPY GIF"
        val fileObj = obj["file"] as? JsonObject ?: return null

        fun extractTierFormat(tier: String, format: String): Triple<String, Int, Int>? {
            val tierObj = fileObj[tier] as? JsonObject ?: return null
            val fmtObj = tierObj[format] as? JsonObject ?: return null
            val url = fmtObj["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
            val w = fmtObj["width"]?.jsonPrimitive?.intOrNull ?: 498
            val h = fmtObj["height"]?.jsonPrimitive?.intOrNull ?: 280
            return Triple(url, w.coerceAtLeast(1), h.coerceAtLeast(1))
        }

        val smGif = extractTierFormat("sm", "gif")
            ?: extractTierFormat("sm", "webp")
            ?: extractTierFormat("md", "gif")
            ?: extractTierFormat("md", "webp")
            ?: extractTierFormat("hd", "gif")
            ?: extractTierFormat("hd", "webp")

        val mdGif = extractTierFormat("md", "gif")
            ?: extractTierFormat("hd", "gif")
            ?: extractTierFormat("md", "webp")
            ?: smGif

        val videoFmt = extractTierFormat("sm", "webm")
            ?: extractTierFormat("md", "webm")
            ?: extractTierFormat("hd", "webm")
            ?: extractTierFormat("md", "mp4")
            ?: extractTierFormat("hd", "mp4")

        val preview = smGif ?: videoFmt ?: return null
        val previewIsVideo = smGif == null && isVideoMediaUrl(preview.first)
        val sendUrl = if (!slug.isNullOrBlank()) {
            "$KLIPY_WEB_BASE/gifs/${URLEncoder.encode(slug, "UTF-8")}"
        } else {
            mdGif?.first ?: videoFmt?.first ?: preview.first
        }

        val chatDimensions = mdGif ?: videoFmt ?: preview
        val item = GifItem(
            id = id,
            slug = slug,
            title = title,
            previewUrl = preview.first,
            previewIsVideo = previewIsVideo,
            sendUrl = sendUrl,
            width = chatDimensions.second,
            height = chatDimensions.third,
            provider = GifProvider.KLIPY,
            videoUrl = videoFmt?.first,
            gifUrl = (mdGif ?: smGif)?.first
        )

        val resolvedMediaUrl = (mdGif ?: smGif)?.first ?: videoFmt?.first ?: preview.first
        cacheResolvedMedia(
            sendUrl,
            ResolvedGifMedia(
                mediaUrl = resolvedMediaUrl,
                fallbackImageUrl = (mdGif ?: smGif)?.first,
                originalUrl = sendUrl,
                width = chatDimensions.second,
                height = chatDimensions.third,
                isVideo = isVideoMediaUrl(resolvedMediaUrl)
            )
        )

        return item
    }

    suspend fun registerKlipyShare(slug: String?, query: String) {
        if (slug.isNullOrBlank()) return
        runCatching {
            val encodedKey = URLEncoder.encode(KLIPY_PUBLIC_API_KEY, "UTF-8")
            val encodedSlug = URLEncoder.encode(slug, "UTF-8")
            val shareQuery = if (query.equals("trending", ignoreCase = true)) "" else query
            StoatHttp.post("$KLIPY_API_BASE/$encodedKey/gifs/share/$encodedSlug") {
                contentType(ContentType.Application.Json)
                setBody(
                    StoatJson.encodeToString(
                        JsonObject.serializer(),
                        buildJsonObject {
                            put("q", shareQuery)
                        }
                    )
                )
            }
        }
    }
}
