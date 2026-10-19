package chat.stoat.composables.voice

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
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

@Composable
fun PipCallView() {
    val room = VoiceCallManager.room
    val channelId = VoiceCallManager.activeChannelId

    if (room == null || channelId == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Call Ended",
                color = Color.White,
                fontSize = 14.sp
            )
        }
        return
    }

    CompositionLocalProvider(RoomLocal provides room) {
        val isMicOn by room.localParticipant::isMicrophoneEnabled.flow.collectAsState()
        val isDeafened = VoiceCallManager.isDeafened
        val activeSpeakers by room::activeSpeakers.flow.collectAsState()
        val participants by rememberParticipants(room)
        val trackRefs by rememberTracks(passedRoom = room)

        val channel = StoatAPI.channelCache[channelId]
        val isDm = channel?.channelType == ChannelType.DirectMessage
        val partnerId = if (isDm) ChannelUtils.resolveDMPartner(channel) else null
        val partnerUser = partnerId?.let { StoatAPI.userCache[it] }

        // Primary speaker or partner
        val mainSpeakerId = activeSpeakers.firstOrNull()?.identity?.value ?: partnerId ?: participants.firstOrNull()?.identity?.value
        val mainSpeakerUser = mainSpeakerId?.let { StoatAPI.userCache[it] }
        val mainSpeakerName = if (mainSpeakerId != null) displayNameInChannel(mainSpeakerId, channelId) else (channel?.let(ChannelUtils::resolveName) ?: "Voice Call")

        val isSpeaking = activeSpeakers.isNotEmpty()

        // Check if there is an active unmuted video track
        val activeVideoTrack = trackRefs.firstOrNull { ref ->
            val pub = ref.publication
            val part = ref.participant
            pub != null && !pub.muted && pub.track != null &&
            when (ref.source) {
                Track.Source.CAMERA -> part.isCameraEnabled
                Track.Source.SCREEN_SHARE -> part.isScreenShareEnabled
                else -> true
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
        ) {
            if (activeVideoTrack != null) {
                VideoTrackView(
                    trackReference = activeVideoTrack,
                    room = room,
                    scaleType = ScaleType.FitInside,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Speaker avatar view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val speakingBorderColour by animateColorAsState(
                        targetValue = if (isSpeaking) Color(0xFF22C55E) else Color.Transparent,
                        animationSpec = spring()
                    )
                    val speakingBorderWidth by animateDpAsState(
                        targetValue = if (isSpeaking) 3.dp else 0.dp,
                        animationSpec = spring()
                    )

                    UserAvatar(
                        username = mainSpeakerName,
                        userId = mainSpeakerId ?: "",
                        avatar = mainSpeakerUser?.avatar,
                        size = 56.dp,
                        allowAnimation = isSpeaking,
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(speakingBorderWidth, speakingBorderColour, CircleShape)
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = mainSpeakerName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Top overlay bar with status & controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Button
                IconButton(
                    onClick = { VoiceCallManager.toggleMicrophone() },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isMicOn) Color(0xFF2563EB) else Color(0xFF334155))
                ) {
                    Icon(
                        painter = if (isMicOn) painterResource(R.drawable.ic_mic_24dp) else painterResource(R.drawable.ic_mic_off_24dp),
                        contentDescription = stringResource(if (isMicOn) R.string.voice_action_mute else R.string.voice_action_unmute),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Deafen Button
                IconButton(
                    onClick = { VoiceCallManager.toggleDeafen() },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (!isDeafened) Color(0xFF2563EB) else Color(0xFF334155))
                ) {
                    Icon(
                        painter = if (isDeafened) painterResource(R.drawable.ic_headset_off_24dp) else painterResource(R.drawable.ic_headset_24dp),
                        contentDescription = stringResource(if (isDeafened) R.string.voice_action_undeafen else R.string.voice_action_deafen),
                        tint = if (isDeafened) MaterialTheme.colorScheme.error else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // End Call Button
                IconButton(
                    onClick = { VoiceCallManager.leave() },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_call_end_24dp__fill),
                        contentDescription = stringResource(R.string.voice_action_disconnect),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
