package chat.stoat.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AndroidVoiceBridge {
    var onConnectRequested: ((channelId: String, channelName: String, livekitUrl: String, token: String) -> Unit)? = null
    var onDisconnectRequested: (() -> Unit)? = null
    var onToggleMuteRequested: ((Boolean) -> Unit)? = null
    var onToggleDeafenRequested: ((Boolean) -> Unit)? = null
}

actual class VoiceCallController actual constructor() {
    private val _connectionState = MutableStateFlow(VoiceConnectionState.DISCONNECTED)
    actual val connectionState: StateFlow<VoiceConnectionState> = _connectionState.asStateFlow()

    private val _activeChannelId = MutableStateFlow<String?>(null)
    actual val activeChannelId: StateFlow<String?> = _activeChannelId.asStateFlow()

    private val _activeChannelName = MutableStateFlow<String?>(null)
    actual val activeChannelName: StateFlow<String?> = _activeChannelName.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    actual val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isDeafened = MutableStateFlow(false)
    actual val isDeafened: StateFlow<Boolean> = _isDeafened.asStateFlow()

    private val _participants = MutableStateFlow<List<VoiceParticipantInfo>>(emptyList())
    actual val participants: StateFlow<List<VoiceParticipantInfo>> = _participants.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    actual val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    actual fun connect(
        channelId: String,
        channelName: String,
        livekitUrl: String,
        token: String
    ) {
        _activeChannelId.value = channelId
        _activeChannelName.value = channelName
        _connectionState.value = VoiceConnectionState.CONNECTED
        _statusMessage.value = "Connected via Android LiveKit Voice Engine"
        _participants.value = listOf(
            VoiceParticipantInfo(
                userId = "local_user",
                displayName = "You",
                isSpeaking = false,
                isMuted = _isMuted.value
            )
        )
        AndroidVoiceBridge.onConnectRequested?.invoke(channelId, channelName, livekitUrl, token)
    }

    actual fun disconnect() {
        AndroidVoiceBridge.onDisconnectRequested?.invoke()
        _connectionState.value = VoiceConnectionState.DISCONNECTED
        _activeChannelId.value = null
        _activeChannelName.value = null
        _participants.value = emptyList()
        _statusMessage.value = null
    }

    actual fun toggleMute() {
        val next = !_isMuted.value
        _isMuted.value = next
        _participants.value = _participants.value.map {
            if (it.userId == "local_user") it.copy(isMuted = next) else it
        }
        AndroidVoiceBridge.onToggleMuteRequested?.invoke(next)
    }

    actual fun toggleDeafen() {
        val next = !_isDeafened.value
        _isDeafened.value = next
        if (next && !_isMuted.value) {
            _isMuted.value = true
        }
        AndroidVoiceBridge.onToggleDeafenRequested?.invoke(next)
    }
}

@Composable
actual fun rememberVoiceCallController(): VoiceCallController {
    return remember { VoiceCallController() }
}
