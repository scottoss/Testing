package chat.stoat.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import chat.stoat.core.model.schemas.User
import chat.stoat.shared.network.SharedStoatClient
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
        val selfUser = SharedStoatClient.currentUser
        val selfId = selfUser?.id ?: SharedStoatClient.selfId ?: "self"
        val selfName = selfUser?.let { User.resolveDefaultName(it) } ?: "Me"

        _activeChannelId.value = channelId
        _activeChannelName.value = channelName
        _connectionState.value = VoiceConnectionState.CONNECTED
        _statusMessage.value = if (livekitUrl.isNotBlank()) {
            "Connected via LiveKit ($livekitUrl)"
        } else {
            "Connected to Voice Channel"
        }

        val remoteIds = SharedStoatClient.voiceStateCache[channelId]?.participants?.mapNotNull { it.id }.orEmpty()
        val list = buildList {
            add(
                VoiceParticipantInfo(
                    userId = selfId,
                    displayName = selfName,
                    isSpeaking = false,
                    isMuted = _isMuted.value
                )
            )
            remoteIds.filter { it != selfId }.forEach { uid ->
                val u = SharedStoatClient.userCache[uid]
                add(
                    VoiceParticipantInfo(
                        userId = uid,
                        displayName = u?.let { User.resolveDefaultName(it) } ?: uid,
                        isSpeaking = false,
                        isMuted = false
                    )
                )
            }
        }
        _participants.value = list
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
        val selfId = SharedStoatClient.selfId ?: "self"
        _participants.value = _participants.value.map {
            if (it.userId == selfId) it.copy(isMuted = next) else it
        }
        AndroidVoiceBridge.onToggleMuteRequested?.invoke(next)
    }

    actual fun toggleDeafen() {
        val next = !_isDeafened.value
        _isDeafened.value = next
        if (next && !_isMuted.value) {
            toggleMute()
        }
        AndroidVoiceBridge.onToggleDeafenRequested?.invoke(next)
    }
}

@Composable
actual fun rememberVoiceCallController(): VoiceCallController {
    return remember { VoiceCallController() }
}
