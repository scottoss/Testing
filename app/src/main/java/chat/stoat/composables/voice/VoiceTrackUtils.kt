package chat.stoat.composables.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import chat.stoat.voice.VoiceCallManager
import io.livekit.android.compose.state.rememberTrackMuted
import io.livekit.android.compose.types.TrackReference
import io.livekit.android.room.track.Track
import io.livekit.android.util.flow

/**
 * Checks reactively whether a video track reference is currently active, unmuted,
 * and producing frames (e.g. camera is turned on).
 * If the user or anyone else stops their camera, this immediately returns false so that
 * the VideoTrackView is removed from the composition and does not keep showing a frozen image.
 */
@Composable
fun isVideoTrackActive(trackReference: TrackReference?): Boolean {
    if (trackReference == null) return false
    val pub = trackReference.publication ?: return false
    if (pub.track == null) return false

    // Reading tracksVersion ensures recomposition on any track mute/unmute or room event
    @Suppress("UNUSED_VARIABLE")
    val currentTracksVersion = VoiceCallManager.tracksVersion

    // Reactive check on track publication muted state via LiveKit's StateFlow
    val isMuted by rememberTrackMuted(trackReference)
    if (isMuted) return false

    val participant = trackReference.participant
    return when (trackReference.source) {
        Track.Source.CAMERA -> {
            val isCameraEnabled by participant::isCameraEnabled.flow.collectAsState(initial = participant.isCameraEnabled)
            isCameraEnabled
        }
        Track.Source.SCREEN_SHARE -> {
            val isScreenShareEnabled by participant::isScreenShareEnabled.flow.collectAsState(initial = participant.isScreenShareEnabled)
            isScreenShareEnabled
        }
        else -> true
    }
}
