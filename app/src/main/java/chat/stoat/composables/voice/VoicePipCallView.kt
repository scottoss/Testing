package chat.stoat.composables.voice

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.composables.chat.displayNameInChannel
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.User
import chat.stoat.voice.VoiceCallManager
import io.livekit.android.compose.local.RoomLocal
import io.livekit.android.compose.state.rememberParticipants
import io.livekit.android.compose.state.rememberTracks
import io.livekit.android.compose.ui.ScaleType
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.room.Room
import io.livekit.android.room.track.Track
import io.livekit.android.util.flow

/**
 * Rendered when MainActivity enters Android system Picture-in-Picture mode.
 */
@Composable
fun VoicePipCallView(
    onLeave: () -> Unit,
    onToggleMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val room = VoiceCallManager.room
    val channelId = VoiceCallManager.activeChannelId

    if (room == null || channelId == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Call Ended",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    CompositionLocalProvider(RoomLocal provides room) {
        val roomState by room::state.flow.collectAsState()
        val isMicOn by room.localParticipant::isMicrophoneEnabled.flow.collectAsState()
        val activeSpeakers by room::activeSpeakers.flow.collectAsState()
        val participants by rememberParticipants(room)
        val trackRefs by rememberTracks(passedRoom = room)

        val channel = StoatAPI.channelCache[channelId]
        val isDm = channel?.channelType == ChannelType.DirectMessage
        val partnerId = if (isDm) ChannelUtils.resolveDMPartner(channel) else null
        val partnerUser = partnerId?.let { StoatAPI.userCache[it] }

        val activeSpeaker = activeSpeakers.firstOrNull() ?: participants.firstOrNull { it != room.localParticipant } ?: room.localParticipant
        val activeSpeakerUser = StoatAPI.userCache[activeSpeaker.identity?.value]
        val activeSpeakerName = if (activeSpeaker.identity?.value != null) {
            displayNameInChannel(activeSpeaker.identity!!.value, channelId)
        } else {
            partnerUser?.let(User::resolveDefaultName) ?: channel?.let(ChannelUtils::resolveName) ?: "Voice Call"
        }

        // Active video track (screenshare or camera) - strictly verify unmuted and camera enabled
        val activeVideoTrack = trackRefs.firstOrNull { ref ->
            val pub = ref.publication
            val part = ref.participant
            ref.source == Track.Source.SCREEN_SHARE && pub != null && !pub.muted && pub.track != null && part.isScreenShareEnabled
        } ?: trackRefs.firstOrNull { ref ->
            val pub = ref.publication
            val part = ref.participant
            ref.source == Track.Source.CAMERA && part == activeSpeaker && pub != null && !pub.muted && pub.track != null && part.isCameraEnabled
        } ?: trackRefs.firstOrNull { ref ->
            val pub = ref.publication
            val part = ref.participant
            ref.source == Track.Source.CAMERA && pub != null && !pub.muted && pub.track != null && part.isCameraEnabled
        }

        val hasValidVideo = activeVideoTrack != null &&
            activeVideoTrack.publication?.muted == false &&
            activeVideoTrack.publication?.track != null &&
            when (activeVideoTrack.source) {
                Track.Source.CAMERA -> activeVideoTrack.participant.isCameraEnabled
                Track.Source.SCREEN_SHARE -> activeVideoTrack.participant.isScreenShareEnabled
                else -> true
            }

        val infiniteTransition = rememberInfiniteTransition(label = "pipPulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pipPulseScale"
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
        ) {
            if (hasValidVideo && activeVideoTrack != null) {
                VideoTrackView(
                    trackReference = activeVideoTrack,
                    room = room,
                    scaleType = ScaleType.FitInside,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Avatar view for audio-only
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val isSpeaking = activeSpeakers.contains(activeSpeaker)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        if (isSpeaking) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp * pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E).copy(alpha = 0.35f))
                            )
                        }
                        UserAvatar(
                            username = activeSpeakerName,
                            userId = activeSpeaker.identity?.value ?: "",
                            avatar = activeSpeakerUser?.avatar,
                            size = 46.dp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(
                                    width = if (isSpeaking) 2.dp else 1.dp,
                                    color = if (isSpeaking) Color(0xFF22C55E) else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )
                    }

                    Text(
                        text = activeSpeakerName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (roomState == Room.State.CONNECTED) Color(0xFF22C55E) else Color(0xFFF59E0B))
                        )
                        Text(
                            text = if (roomState == Room.State.CONNECTED) "Connected" else "Connecting...",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Compact control bar overlay in PiP
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic toggle
                IconButton(
                    onClick = onToggleMic,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isMicOn) Color.White.copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.8f))
                ) {
                    Icon(
                        painter = painterResource(
                            if (isMicOn) R.drawable.ic_mic_24dp else R.drawable.ic_mic_off_24dp
                        ),
                        contentDescription = "Toggle Mic",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Leave call
                IconButton(
                    onClick = onLeave,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_call_end_24dp__fill),
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
