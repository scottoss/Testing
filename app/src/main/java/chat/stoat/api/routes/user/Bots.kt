package chat.stoat.api.routes.user

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

@Serializable
data class OwnedBot(
    @SerialName("_id") val id: String,
    val token: String,
    @SerialName("public") val isPublic: Boolean = false,
)

@Serializable
data class OwnedBotUser(
    @SerialName("_id") val id: String,
    val username: String,
    val discriminator: String = "0000",
)

@Serializable
data class OwnedBotsResponse(
    val bots: List<OwnedBot> = emptyList(),
    val users: List<OwnedBotUser> = emptyList(),
)

@Serializable
private data class CreateBotBody(val name: String)

@Serializable
private data class EditBotBody(
    @SerialName("public") val isPublic: Boolean? = null,
    val remove: List<String>? = null,
)

@Serializable
private data class EditBotUsernameBody(val username: String)

private suspend fun HttpResponse.requireSuccess(): String {
    val text = bodyAsText()
    if (!status.isSuccess()) throw Exception(text.ifBlank { status.toString() })
    return text
}

suspend fun fetchOwnedBots(): OwnedBotsResponse {
    val text = StoatHttp.get("/bots/@me".api()).requireSuccess()
    return StoatJson.decodeFromString(OwnedBotsResponse.serializer(), text)
}

suspend fun createBot(name: String): OwnedBot {
    val text = StoatHttp.post("/bots/create".api()) {
        contentType(ContentType.Application.Json)
        setBody(StoatJson.encodeToString(CreateBotBody.serializer(), CreateBotBody(name)))
    }.requireSuccess()
    return StoatJson.decodeFromString(OwnedBot.serializer(), text)
}

suspend fun setBotPublic(botId: String, public: Boolean): OwnedBot {
    val text = StoatHttp.patch("/bots/$botId".api()) {
        contentType(ContentType.Application.Json)
        setBody(StoatJson.encodeToString(EditBotBody.serializer(), EditBotBody(isPublic = public)))
    }.requireSuccess()
    return StoatJson.decodeFromString(OwnedBot.serializer(), text)
}

suspend fun resetBotToken(botId: String): OwnedBot {
    val text = StoatHttp.patch("/bots/$botId".api()) {
        contentType(ContentType.Application.Json)
        setBody(StoatJson.encodeToString(EditBotBody.serializer(), EditBotBody(remove = listOf("Token"))))
    }.requireSuccess()
    return StoatJson.decodeFromString(OwnedBot.serializer(), text)
}

suspend fun renameBot(botId: String, username: String) {
    StoatHttp.patch("/users/$botId".api()) {
        contentType(ContentType.Application.Json)
        setBody(StoatJson.encodeToString(EditBotUsernameBody.serializer(), EditBotUsernameBody(username)))
    }.requireSuccess()
}

suspend fun deleteBot(botId: String) {
    StoatHttp.delete("/bots/$botId".api()).requireSuccess()
}
