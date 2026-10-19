package chat.stoat.shared.network

import androidx.compose.runtime.mutableStateListOf
import kotlinx.datetime.Clock

data class NetworkLogEntry(
    val id: String,
    val timestampMs: Long,
    val method: String,
    val url: String,
    val statusCode: Int?,
    val durationMs: Long,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val responseBody: String? = null,
    val isWebSocket: Boolean = false,
    val error: String? = null
)

/**
 * Multiplatform HTTP & WebSocket Inspector (Chucker equivalent for iOS and shared UI).
 * Captures live REST requests, responses, status codes, durations, and WebSocket frames.
 */
object SharedNetworkInspector {
    private const val MAX_ENTRIES = 250
    private var counter = 0L

    val entries = mutableStateListOf<NetworkLogEntry>()

    fun recordHttp(
        method: String,
        url: String,
        statusCode: Int?,
        durationMs: Long,
        requestHeaders: Map<String, String> = emptyMap(),
        requestBody: String? = null,
        responseBody: String? = null,
        error: String? = null
    ) {
        val redactedHeaders = requestHeaders.mapValues { (k, v) ->
            if (k.equals("x-session-token", ignoreCase = true) ||
                k.equals("x-mfa-ticket", ignoreCase = true) ||
                k.equals("authorization", ignoreCase = true)
            ) {
                if (v.length > 6) "${v.take(3)}***${v.takeLast(3)}" else "***"
            } else {
                v
            }
        }
        val entry = NetworkLogEntry(
            id = "http_${++counter}_${Clock.System.now().toEpochMilliseconds()}",
            timestampMs = Clock.System.now().toEpochMilliseconds(),
            method = method,
            url = url,
            statusCode = statusCode,
            durationMs = durationMs,
            requestHeaders = redactedHeaders,
            requestBody = requestBody?.take(20_000),
            responseBody = responseBody?.take(30_000),
            isWebSocket = false,
            error = error
        )
        entries.add(0, entry)
        if (entries.size > MAX_ENTRIES) {
            entries.removeRange(MAX_ENTRIES, entries.size)
        }
    }

    fun recordWebSocketFrame(
        direction: String,
        url: String,
        frameType: String,
        payload: String
    ) {
        val redactedPayload = if (frameType == "Authenticate") {
            "{\"type\":\"Authenticate\",\"token\":\"***REDACTED***\"}"
        } else {
            payload.take(15_000)
        }
        val entry = NetworkLogEntry(
            id = "ws_${++counter}_${Clock.System.now().toEpochMilliseconds()}",
            timestampMs = Clock.System.now().toEpochMilliseconds(),
            method = "WS $direction ($frameType)",
            url = url,
            statusCode = 101,
            durationMs = 0L,
            requestBody = if (direction == "SEND") redactedPayload else null,
            responseBody = if (direction == "RECV") redactedPayload else null,
            isWebSocket = true
        )
        entries.add(0, entry)
        if (entries.size > MAX_ENTRIES) {
            entries.removeRange(MAX_ENTRIES, entries.size)
        }
    }

    fun clear() {
        entries.clear()
    }
}
