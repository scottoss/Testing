package chat.stoat.composables.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

/**
 * In-app floating Picture-in-Picture card displayed when user navigates around the app
 * while in an active call and the full voice sheet is minimized.
 * Supports dragging anywhere, and dragging to the bottom dismiss zone to remove from screen.
 */
@Composable
fun VoiceFloatingPip(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val room = VoiceCallManager.room
    val channelId = VoiceCallManager.activeChannelId
    val isSheetVisible = VoiceCallManager.isSheetVisible
    val isFloatingPipVisible = VoiceCallManager.isFloatingPipVisible

    AnimatedVisibility(
        visible = room != null && channelId != null && !isSheetVisible && isFloatingPipVisible,
        enter = fadeIn(tween(200)) + scaleIn(tween(200)),
        exit = fadeOut(tween(200)) + scaleOut(tween(200)),
        modifier = modifier
    ) {
        if (room == null || channelId == null) return@AnimatedVisibility

        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        val dismissThreshold = 160f
        val isNearDismiss = isDragging && offsetY > 130f

        CompositionLocalProvider(RoomLocal provides room) {
            val roomState by room::state.flow.collectAsState()
            val isMicOn by room.localParticipant::isMicrophoneEnabled.flow.collectAsState()
            val isLocalCameraOn by room.localParticipant::isCameraEnabled.flow.collectAsState()
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

            // Select active video track - strictly ensure the track is unmuted and camera is actually ON
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

            val hasValidVideoFeed = activeVideoTrack != null &&
                activeVideoTrack.publication?.muted == false &&
                activeVideoTrack.publication?.track != null &&
                when (activeVideoTrack.source) {
                    Track.Source.CAMERA -> activeVideoTrack.participant.isCameraEnabled
                    Track.Source.SCREEN_SHARE -> activeVideoTrack.participant.isScreenShareEnabled
                    else -> true
                }

            val infiniteTransition = rememberInfiniteTransition(label = "floatingPulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "floatingPulseAlpha"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                // Bottom Dismiss Zone (visible while user is dragging PiP)
                AnimatedVisibility(
                    visible = isDragging,
                    enter = fadeIn(tween(150)) + scaleIn(tween(150)),
                    exit = fadeOut(tween(150)) + scaleOut(tween(150)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isNearDismiss) Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isNearDismiss) 2.dp else 1.dp,
                                color = if (isNearDismiss) Color.White else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(if (isNearDismiss) 56.dp else 48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close_24dp),
                                    contentDescription = "Remove from screen",
                                    tint = if (isNearDismiss) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isNearDismiss) "Release to remove" else "Drag to bottom to remove",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isNearDismiss) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Floating PiP Card
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .safeDrawingPadding()
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                },
                                onDragEnd = {
                                    isDragging = false
                                    if (offsetY > dismissThreshold) {
                                        VoiceCallManager.dismissFloatingPip()
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        offsetY = offsetY.coerceIn(-650f, 40f)
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    offsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                    offsetY += dragAmount.y
                                }
                            )
                        }
                        .padding(16.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isNearDismiss) 0.7f else 0.95f),
                        shape = RoundedCornerShape(22.dp),
                        tonalElevation = 8.dp,
                        shadowElevation = 14.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            color = if (isNearDismiss) Color(0xFFEF4444).copy(alpha = 0.85f) else Color(0xFF22C55E).copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .width(220.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onExpand() }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Video preview or Avatar row
                            if (hasValidVideoFeed && activeVideoTrack != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black)
                                ) {
                                    VideoTrackView(
                                        trackReference = activeVideoTrack,
                                        room = room,
                                        scaleType = ScaleType.FitInside,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = activeSpeakerName,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val isSpeaking = activeSpeakers.contains(activeSpeaker)
                                    Box(contentAlignment = Alignment.Center) {
                                        UserAvatar(
                                            username = activeSpeakerName,
                                            userId = activeSpeaker.identity?.value ?: "",
                                            avatar = activeSpeakerUser?.avatar,
                                            size = 38.dp,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (isSpeaking) 2.dp else 1.dp,
                                                    color = if (isSpeaking) Color(0xFF22C55E) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                    shape = CircleShape
                                                )
                                        )
                                        if (isSpeaking) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF22C55E))
                                                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = activeSpeakerName,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF22C55E).copy(alpha = pulseAlpha))
                                            )
                                            Text(
                                                text = "In Call",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF22C55E),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }

                                    // Fullscreen expand icon
                                    IconButton(
                                        onClick = onExpand,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_fullscreen_24dp),
                                            contentDescription = "Expand Call",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Compact Quick Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mic Mute Button
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isMicOn) MaterialTheme.colorScheme.surfaceContainerHighest else Color(0xFFEF4444).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isMicOn) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { VoiceCallManager.toggleMicrophone() }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (isMicOn) R.drawable.ic_mic_24dp else R.drawable.ic_mic_off_24dp
                                            ),
                                            contentDescription = "Mic Toggle",
                                            tint = if (isMicOn) MaterialTheme.colorScheme.onSurface else Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Disconnect Button
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { VoiceCallManager.leave() }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 6.dp)
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
                }
            }
        }
    }
}
