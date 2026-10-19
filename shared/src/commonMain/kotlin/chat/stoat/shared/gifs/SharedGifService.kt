package chat.stoat.shared.gifs

import chat.stoat.core.model.data.DAWNCHAT_GIFBOX_URL
import chat.stoat.core.model.data.GIFBOX_WEB_BASE
import chat.stoat.core.model.data.KLIPY_API_BASE
import chat.stoat.core.model.data.KLIPY_PUBLIC_API_KEY
import chat.stoat.core.model.data.KLIPY_WEB_BASE
import chat.stoat.core.model.schemas.Embed
import chat.stoat.shared.network.SharedHttpClient
import chat.stoat.shared.network.SharedJson
import chat.stoat.shared.network.SharedStoatApi
import chat.stoat.shared.platform.encodeUrlQueryComponent
import io.ktor.client.request.get
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

enum class SharedGifProvider(val id: String, val displayName: String) {
    GIFBOX("gifbox", "Gifbox"),
    KLIPY("klipy", "KLIPY")
}

data class SharedGifCategory(
    val title: String,
    val query: String,
    val previewUrl: String? = null,
    val isTrendingTile: Boolean = false
)

data class SharedGifItem(
    val id: String,
    val slug: String? = null,
    val title: String,
    val previewUrl: String,
    val previewIsVideo: Boolean,
    val sendUrl: String,
    val width: Int,
    val height: Int,
    val provider: SharedGifProvider,
    val videoUrl: String? = null,
    val gifUrl: String? = null
)

data class SharedResolvedGifMedia(
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

object SharedGifService {
    private val mediaCache = mutableMapOf<String, SharedResolvedGifMedia>()

    private val gifHostSuffixes = listOf(
        "gifbox.me",
        "gifbox.dawn-chat.com",
        "klipy.com",
        "tenor.com",
        "giphy.com"
    )

    private fun extractHost(rawUrl: String): String? {
        val withoutScheme = rawUrl.trim().substringAfter("://", "")
        if (withoutScheme.isEmpty()) return null
        return withoutScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore(':')
            .lowercase()
            .takeIf { it.isNotBlank() }
    }

    private fun extractPath(rawUrl: String): String {
        val withoutScheme = rawUrl.trim().substringAfter("://", "")
        if (withoutScheme.isEmpty()) return ""
        val path = withoutScheme.substringAfter('/', "").substringBefore('?').substringBefore('#')
        return if (path.isEmpty()) "" else "/$path"
    }

    fun isGifProviderUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        val cleaned = rawUrl.trim()
        val host = extractHost(cleaned) ?: return false
        if (gifHostSuffixes.any { host == it || host.endsWith(".$it") }) {
            return true
        }
        return extractPath(cleaned).lowercase().endsWith(".gif")
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

    fun resolveGifMediaFromEmbed(embed: Embed): SharedResolvedGifMedia? {
        if (!isGifEmbed(embed)) return null

        val originalLink = embed.originalURL ?: embed.url
        originalLink?.let { cached ->
            mediaCache[cached]?.let { return it }
        }

        val video = embed.video
        val image = embed.image

        if (image?.url != null && isAnimatedImageMediaUrl(image.url)) {
            return SharedResolvedGifMedia(
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
            return SharedResolvedGifMedia(
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
            return SharedResolvedGifMedia(
                mediaUrl = asProxyIfNeeded(imageUrl),
                fallbackImageUrl = asProxyIfNeeded(imageUrl),
                originalUrl = originalLink,
                width = (image.width ?: embed.width ?: 498L).toInt().coerceAtLeast(1),
                height = (image.height ?: embed.height ?: 280L).toInt().coerceAtLeast(1),
                isVideo = isVideoMediaUrl(imageUrl)
            )
        }

        return null
    }

    private fun asProxyIfNeeded(url: String): String {
        val januaryBase = SharedStoatApi.januaryBaseUrl
        if (url.startsWith(januaryBase)) return url
        val host = extractHost(url).orEmpty()
        if (host.endsWith("klipy.com") || host.endsWith("gifbox.me") || host.endsWith("dawn-chat.com")) {
            return url
        }
        return SharedStoatApi.asJanuaryProxyUrl(url)
    }

    suspend fun fetchCategories(provider: SharedGifProvider): List<SharedGifCategory> {
        return when (provider) {
            SharedGifProvider.GIFBOX -> fetchGifboxCategories()
            SharedGifProvider.KLIPY -> fetchKlipyCategories()
        }
    }

    suspend fun fetchGifs(provider: SharedGifProvider, query: String): List<SharedGifItem> {
        val normalizedQuery = query.trim()
        return when (provider) {
            SharedGifProvider.GIFBOX -> fetchGifboxGifs(normalizedQuery.ifBlank { "trending" })
            SharedGifProvider.KLIPY -> fetchKlipyGifs(normalizedQuery.ifBlank { "trending" })
        }
    }

    suspend fun fetchGifboxCategories(): List<SharedGifCategory> {
        val response = SharedHttpClient.get("$DAWNCHAT_GIFBOX_URL/categories?locale=en_US")
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Gifbox categories failed: ${response.status.value}")
        }
        val element = SharedJson.parseToJsonElement(response.bodyAsText())
        val categories = mutableListOf(
            SharedGifCategory(
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
                SharedGifCategory(
                    title = title,
                    query = query,
                    previewUrl = image,
                    isTrendingTile = false
                )
            )
        }
        return categories
    }

    suspend fun fetchGifboxGifs(query: String): List<SharedGifItem> {
        val endpoint = if (query.equals("trending", ignoreCase = true) || query.isBlank()) {
            "$DAWNCHAT_GIFBOX_URL/trending?locale=en_US"
        } else {
            val encoded = encodeUrlQueryComponent(query)
            "$DAWNCHAT_GIFBOX_URL/search?locale=en_US&query=$encoded"
        }

        val response = SharedHttpClient.get(endpoint)
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Gifbox search failed: ${response.status.value}")
        }

        val root = SharedJson.parseToJsonElement(response.bodyAsText())
        val resultsArray: JsonArray = when (root) {
            is JsonObject -> (root["results"] ?: root["data"] ?: root["items"]) as? JsonArray ?: JsonArray(emptyList())
            is JsonArray -> root
            else -> JsonArray(emptyList())
        }

        return resultsArray.mapNotNull { parseGifboxGifItem(it) }
    }

    private fun parseGifboxGifItem(element: JsonElement): SharedGifItem? {
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

        val resolvedMediaUrl = gifFormat?.first ?: videoFormat?.first ?: primaryPreview.first
        mediaCache[sendUrl] = SharedResolvedGifMedia(
            mediaUrl = resolvedMediaUrl,
            fallbackImageUrl = gifFormat?.first,
            originalUrl = sendUrl,
            width = primaryPreview.second,
            height = primaryPreview.third,
            isVideo = isVideoMediaUrl(resolvedMediaUrl)
        )

        return SharedGifItem(
            id = id,
            slug = id,
            title = title,
            previewUrl = primaryPreview.first,
            previewIsVideo = previewIsVideo,
            sendUrl = sendUrl,
            width = primaryPreview.second,
            height = primaryPreview.third,
            provider = SharedGifProvider.GIFBOX,
            videoUrl = videoFormat?.first,
            gifUrl = gifFormat?.first
        )
    }

    suspend fun fetchKlipyCategories(): List<SharedGifCategory> {
        val encodedKey = encodeUrlQueryComponent(KLIPY_PUBLIC_API_KEY)
        val response = SharedHttpClient.get("$KLIPY_API_BASE/$encodedKey/gifs/categories?locale=en_US")
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("KLIPY categories failed: ${response.status.value}")
        }
        val root = SharedJson.parseToJsonElement(response.bodyAsText()).jsonObject
        val categoriesArray = root["data"]?.jsonObject?.get("categories") as? JsonArray

        val result = mutableListOf(
            SharedGifCategory(
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
                SharedGifCategory(
                    title = category,
                    query = query,
                    previewUrl = previewUrl,
                    isTrendingTile = false
                )
            )
        }
        return result
    }

    suspend fun fetchKlipyGifs(query: String): List<SharedGifItem> {
        val encodedKey = encodeUrlQueryComponent(KLIPY_PUBLIC_API_KEY)
        val isTrending = query.equals("trending", ignoreCase = true) || query.isBlank()
        val path = if (isTrending) "trending" else "search"
        val queryParam = if (isTrending) "" else "&q=${encodeUrlQueryComponent(query)}"
        val url = "$KLIPY_API_BASE/$encodedKey/gifs/$path?page=1&per_page=24&locale=us&content_filter=high$queryParam"

        val response = SharedHttpClient.get(url)
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("KLIPY search failed: ${response.status.value}")
        }

        val root = SharedJson.parseToJsonElement(response.bodyAsText()).jsonObject
        val itemsArray = root["data"]?.jsonObject?.get("data") as? JsonArray ?: JsonArray(emptyList())
        return itemsArray.mapNotNull { elem ->
            (elem as? JsonObject)?.let { parseKlipyGifItem(it) }
        }
    }

    private fun parseKlipyGifItem(obj: JsonObject): SharedGifItem? {
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
            "$KLIPY_WEB_BASE/gifs/${encodeUrlQueryComponent(slug)}"
        } else {
            mdGif?.first ?: videoFmt?.first ?: preview.first
        }

        val chatDimensions = mdGif ?: videoFmt ?: preview
        val resolvedMediaUrl = (mdGif ?: smGif)?.first ?: videoFmt?.first ?: preview.first
        mediaCache[sendUrl] = SharedResolvedGifMedia(
            mediaUrl = resolvedMediaUrl,
            fallbackImageUrl = (mdGif ?: smGif)?.first,
            originalUrl = sendUrl,
            width = chatDimensions.second,
            height = chatDimensions.third,
            isVideo = isVideoMediaUrl(resolvedMediaUrl)
        )

        return SharedGifItem(
            id = id,
            slug = slug,
            title = title,
            previewUrl = preview.first,
            previewIsVideo = previewIsVideo,
            sendUrl = sendUrl,
            width = chatDimensions.second,
            height = chatDimensions.third,
            provider = SharedGifProvider.KLIPY,
            videoUrl = videoFmt?.first,
            gifUrl = (mdGif ?: smGif)?.first
        )
    }

    suspend fun registerKlipyShare(slug: String?, query: String) {
        if (slug.isNullOrBlank()) return
        runCatching {
            val encodedKey = encodeUrlQueryComponent(KLIPY_PUBLIC_API_KEY)
            val encodedSlug = encodeUrlQueryComponent(slug)
            val shareQuery = if (query.equals("trending", ignoreCase = true)) "" else query
            SharedHttpClient.post("$KLIPY_API_BASE/$encodedKey/gifs/share/$encodedSlug") {
                contentType(ContentType.Application.Json)
                setBody(
                    SharedJson.encodeToString(
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
