package chat.stoat.shared.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

enum class VoiceConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class VoiceParticipantInfo(
    val userId: String,
    val displayName: String,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val isPublishingVideo: Boolean = false
)

expect class VoiceCallController() {
    val connectionState: StateFlow<VoiceConnectionState>
    val activeChannelId: StateFlow<String?>
    val activeChannelName: StateFlow<String?>
    val isMuted: StateFlow<Boolean>
    val isDeafened: StateFlow<Boolean>
    val participants: StateFlow<List<VoiceParticipantInfo>>
    val statusMessage: StateFlow<String?>

    fun connect(
        channelId: String,
        channelName: String,
        livekitUrl: String = "",
        token: String = ""
    )

    fun disconnect()
    fun toggleMute()
    fun toggleDeafen()
}

@Composable
expect fun rememberVoiceCallController(): VoiceCallController
