package chat.stoat.api.routes.channel

import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class WebhookAvatar(
    @SerialName("_id") val id: String
)

@Serializable
data class Webhook(
    val id: String,
    val name: String,
    val avatar: WebhookAvatar? = null,
    @SerialName("channel_id") val channelId: String,
    val token: String? = null,
)

@Serializable
data class CreateWebhookBody(
    val name: String,
    val avatar: String? = null,
)

@Serializable
data class EditWebhookBody(
    val name: String? = null,
    val avatar: String? = null,
    val remove: List<String>? = null,
)

private suspend fun HttpResponse.requireSuccess(): String {
    val text = bodyAsText()
    if (!status.isSuccess()) throw Exception(text.ifBlank { status.toString() })
    return text
}

suspend fun fetchChannelWebhooks(channelId: String): List<Webhook> {
    val text = StoatHttp.get("/channels/$channelId/webhooks".api()).requireSuccess()
    return StoatJson.decodeFromString(ListSerializer(Webhook.serializer()), text)
}

suspend fun createChannelWebhook(channelId: String, name: String, avatar: String? = null): Webhook {
    val text = StoatHttp.post("/channels/$channelId/webhooks".api()) {
        contentType(ContentType.Application.Json)
        setBody(StoatJson.encodeToString(CreateWebhookBody.serializer(), CreateWebhookBody(name, avatar)))
    }.requireSuccess()
    return StoatJson.decodeFromString(Webhook.serializer(), text)
}

suspend fun editWebhook(webhookId: String, body: EditWebhookBody): Webhook {
    val text = StoatHttp.patch("/webhooks/$webhookId".api()) {
        contentType(ContentType.Application.Json)
        setBody(StoatJson.encodeToString(EditWebhookBody.serializer(), body))
    }.requireSuccess()
    return StoatJson.decodeFromString(Webhook.serializer(), text)
}

suspend fun deleteWebhook(webhookId: String) {
    StoatHttp.delete("/webhooks/$webhookId".api()).requireSuccess()
}
