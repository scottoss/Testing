package chat.stoat.api.routes.account

import android.os.Build
import android.util.Log
import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import chat.stoat.api.apiError
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class LoginNegotiation(
    val email: String,
    val password: String,

    @SerialName("friendly_name")
    val friendlyName: String,
    val captcha: String? = null
)

@Serializable
data class LoginMfaAmendmentTotpCode(
    @SerialName("mfa_ticket")
    val mfaTicket: String,

    @SerialName("mfa_response")
    val mfaResponse: MfaResponseTotpCode,

    @SerialName("friendly_name")
    val friendlyName: String
)

@Serializable
data class LoginMfaAmendmentRecoveryCode(
    @SerialName("mfa_ticket")
    val mfaTicket: String,

    @SerialName("mfa_response")
    val mfaResponse: MfaResponseRecoveryCode,

    @SerialName("friendly_name")
    val friendlyName: String
)

/**
 * To add a new MFA method, add a variant here plus its branch in [MfaResponseSerializer]
 * then describe the UI in [chat.stoat.composables.mfa.MfaMethod].
 */
@Serializable(with = MfaResponseSerializer::class)
sealed interface MfaResponse

@Serializable
data class MfaResponsePassword(
    val password: String
) : MfaResponse

@Serializable
data class MfaResponseRecoveryCode(
    @SerialName("recovery_code")
    val recoveryCode: String
) : MfaResponse

@Serializable
data class MfaResponseTotpCode(
    @SerialName("totp_code")
    val totpCode: String
) : MfaResponse

object MfaResponseSerializer : KSerializer<MfaResponse> {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("MfaResponse", PolymorphicKind.SEALED)

    override fun serialize(encoder: Encoder, value: MfaResponse) = when (value) {
        is MfaResponsePassword ->
            encoder.encodeSerializableValue(MfaResponsePassword.serializer(), value)

        is MfaResponseRecoveryCode ->
            encoder.encodeSerializableValue(MfaResponseRecoveryCode.serializer(), value)

        is MfaResponseTotpCode ->
            encoder.encodeSerializableValue(MfaResponseTotpCode.serializer(), value)
    }

    override fun deserialize(decoder: Decoder): MfaResponse =
        throw UnsupportedOperationException("MfaResponse is only ever sent to the API")
}

@Serializable
data class MfaLoginSpec(
    val result: String,
    val ticket: String,

    @SerialName("allowed_methods")
    val allowedMethods: List<String>
)

@Serializable
data class MfaCheck(
    val result: String
)

@Serializable
data class WebPushData(
    val endpoint: String,

    @SerialName("p256dh")
    val p256diffieHellman: String,
    val auth: String
)

@Serializable
data class UserHints(
    val result: String,

    @SerialName("_id")
    val id: String,

    @SerialName("user_id")
    val userId: String,
    val token: String,
    val name: String,
    val subscription: WebPushData? = null
)

data class EmailPasswordAssessment(
    val proceedMfa: Boolean = false,
    val mfaSpec: MfaLoginSpec? = null,
    val firstUserHints: UserHints? = null,
    val error: StoatAPIError? = null
)

private fun parseLoginResponse(response: HttpResponse, responseContent: String): EmailPasswordAssessment {
    // 1. Check HTTP status code
    if (!response.status.isSuccess()) {
        val errorType = runCatching {
            Json.parseToJsonElement(responseContent).jsonObject["type"]?.jsonPrimitive?.contentOrNull
        }.getOrNull() ?: runCatching {
            Json.parseToJsonElement(responseContent).jsonObject["error"]?.jsonPrimitive?.contentOrNull
        }.getOrNull() ?: apiError(responseContent, response.status.value)

        return EmailPasswordAssessment(error = StoatAPIError(errorType))
    }

    // 2. Parse JSON body
    val jsonElement = runCatching {
        Json.parseToJsonElement(responseContent)
    }.getOrNull()

    val jsonObject = jsonElement as? JsonObject
        ?: return EmailPasswordAssessment(error = StoatAPIError("InvalidResponse"))

    // Handle any error returned with 2xx status
    jsonObject["type"]?.jsonPrimitive?.contentOrNull?.let { errorType ->
        return EmailPasswordAssessment(error = StoatAPIError(errorType))
    }

    val result = jsonObject["result"]?.jsonPrimitive?.contentOrNull
        ?: return EmailPasswordAssessment(error = StoatAPIError("MissingResult"))

    return when (result) {
        "Success" -> {
            val userHints = runCatching {
                StoatJson.decodeFromJsonElement(UserHints.serializer(), jsonObject)
            }.getOrNull()

            if (userHints != null) {
                EmailPasswordAssessment(firstUserHints = userHints)
            } else {
                EmailPasswordAssessment(error = StoatAPIError("FailedToParseUserSession"))
            }
        }

        "MFA" -> {
            val mfaSpec = runCatching {
                StoatJson.decodeFromJsonElement(MfaLoginSpec.serializer(), jsonObject)
            }.getOrNull()

            if (mfaSpec != null) {
                EmailPasswordAssessment(proceedMfa = true, mfaSpec = mfaSpec)
            } else {
                EmailPasswordAssessment(error = StoatAPIError("FailedToParseMfaSpec"))
            }
        }

        "Disabled" -> EmailPasswordAssessment(error = StoatAPIError("DisabledAccount"))
        else -> EmailPasswordAssessment(error = StoatAPIError("UnknownResult_$result"))
    }
}

suspend fun negotiateAuthentication(email: String, password: String): EmailPasswordAssessment {
    val sessionName = friendlySessionName()

    val response: HttpResponse = StoatHttp.post("/auth/session/login".api()) {
        contentType(ContentType.Application.Json)
        setBody(LoginNegotiation(email, password, sessionName, null))
    }

    val responseContent = response.bodyAsText()
    Log.d("Stoat", "negotiateAuthentication: status=${response.status.value}, body=$responseContent")

    return parseLoginResponse(response, responseContent)
}

suspend fun authenticateWithMfaTotpCode(
    mfaTicket: String,
    mfaResponse: MfaResponseTotpCode
): EmailPasswordAssessment {
    val response: HttpResponse = StoatHttp.post("/auth/session/login".api()) {
        contentType(ContentType.Application.Json)
        setBody(LoginMfaAmendmentTotpCode(mfaTicket, mfaResponse, friendlySessionName()))
    }

    val responseContent = response.bodyAsText()
    Log.d("Stoat", "authenticateWithMfaTotpCode: status=${response.status.value}, body=$responseContent")

    return parseLoginResponse(response, responseContent)
}

suspend fun authenticateWithMfaRecoveryCode(
    mfaTicket: String,
    mfaResponse: MfaResponseRecoveryCode
): EmailPasswordAssessment {
    val response: HttpResponse = StoatHttp.post("/auth/session/login".api()) {
        contentType(ContentType.Application.Json)
        setBody(LoginMfaAmendmentRecoveryCode(mfaTicket, mfaResponse, friendlySessionName()))
    }

    val responseContent = response.bodyAsText()
    Log.d("Stoat", "authenticateWithMfaRecoveryCode: status=${response.status.value}, body=$responseContent")

    return parseLoginResponse(response, responseContent)
}

fun friendlySessionName(): String {
    return "DawnChat for Android on ${Build.MANUFACTURER} ${Build.MODEL}"
}
