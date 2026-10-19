package chat.stoat.api.routes.account

import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmailCaptchaBody(
    val email: String,
    val captcha: String
)

@Serializable
data class ConfirmPasswordResetBody(
    val password: String,
    val token: String,
    @SerialName("remove_sessions") val removeSessions: Boolean
)

@Serializable
data class ConfirmDeleteBody(val token: String)

private suspend fun HttpResponse.throwIfFailed() {
    if (status.isSuccess()) return
    val text = bodyAsText()
    runCatching { StoatJson.decodeFromString(StoatAPIError.serializer(), text) }
        .onSuccess { throw Exception(it.type) }
    throw Exception(text.ifBlank { "HTTP ${status.value}" })
}

suspend fun requestPasswordReset(email: String, captcha: String) {
    StoatHttp.post("/auth/account/reset_password".api()) {
        setBody(EmailCaptchaBody(email, captcha))
        contentType(ContentType.Application.Json)
    }.throwIfFailed()
}

suspend fun resendVerification(email: String, captcha: String) {
    StoatHttp.post("/auth/account/reverify".api()) {
        setBody(EmailCaptchaBody(email, captcha))
        contentType(ContentType.Application.Json)
    }.throwIfFailed()
}

suspend fun verifyEmailToken(token: String) {
    StoatHttp.post("/auth/account/verify/$token".api()).throwIfFailed()
}

suspend fun confirmAccountDeletion(token: String) {
    StoatHttp.put("/auth/account/delete".api()) {
        setBody(ConfirmDeleteBody(token))
        contentType(ContentType.Application.Json)
    }.throwIfFailed()
}

suspend fun confirmPasswordReset(token: String, password: String, removeSessions: Boolean) {
    StoatHttp.patch("/auth/account/reset_password".api()) {
        setBody(ConfirmPasswordResetBody(password, token, removeSessions))
        contentType(ContentType.Application.Json)
    }.throwIfFailed()
}
