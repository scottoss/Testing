package chat.stoat.shared.network

import chat.stoat.core.model.data.STOAT_BASE
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.data.STOAT_PROXY
import chat.stoat.core.model.schemas.Message
import chat.stoat.shared.platform.encodeUrlQueryComponent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val SharedJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
}

@Serializable
data class SharedSendMessageRequest(
    val content: String
)

expect fun createPlatformHttpClient(): HttpClient

val SharedHttpClient: HttpClient by lazy {
    createPlatformHttpClient()
}

object SharedStoatApi {
    var apiBaseUrl: String = STOAT_BASE
    var autumnBaseUrl: String = STOAT_FILES
    var januaryBaseUrl: String = STOAT_PROXY

    fun asJanuaryProxyUrl(url: String): String {
        return "$januaryBaseUrl/proxy?url=${encodeUrlQueryComponent(url)}"
    }

    suspend fun checkNodeAvailable(): Boolean {
        return runCatching {
            val response = SharedHttpClient.get(apiBaseUrl)
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }

    suspend fun fetchMessages(
        channelId: String,
        sessionToken: String
    ): List<Message> {
        val response = SharedHttpClient.get("$apiBaseUrl/channels/$channelId/messages?limit=50") {
            header("x-session-token", sessionToken)
        }
        if (response.status != HttpStatusCode.OK) {
            return emptyList()
        }
        return SharedJson.decodeFromString(response.bodyAsText())
    }

    suspend fun sendChannelMessage(
        channelId: String,
        content: String,
        sessionToken: String
    ): Message? {
        val response = SharedHttpClient.post("$apiBaseUrl/channels/$channelId/messages") {
            header("x-session-token", sessionToken)
            contentType(ContentType.Application.Json)
            setBody(SharedSendMessageRequest(content = content))
        }
        if (response.status != HttpStatusCode.OK) {
            return null
        }
        return response.body()
    }
}
