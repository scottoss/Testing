package chat.stoat.composables.voice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.composables.chat.displayNameInChannel
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.voice.VoiceCallManager
import io.livekit.android.compose.state.rememberTracks
import io.livekit.android.compose.ui.ScaleType
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.room.Room
import io.livekit.android.room.track.Track
import io.livekit.android.util.flow
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ScreenshareFullScreenDialog(
    initialTrackIndex: Int = 0,
    room: Room,
    channelId: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        ScreenshareFullScreenContent(
            initialTrackIndex = initialTrackIndex,
            room = room,
            channelId = channelId,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun ScreenshareFullScreenContent(
    initialTrackIndex: Int = 0,
    room: Room,
    channelId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onDismiss()
    }

    val trackRefs by rememberTracks(passedRoom = room)
    val videoTracks = trackRefs.filter { ref ->
        val pub = ref.publication
        pub != null && !pub.muted
    }

    var selectedIndex by remember(videoTracks.size) {
        mutableStateOf(if (initialTrackIndex in videoTracks.indices) initialTrackIndex else 0)
    }

    val currentTrackRef = videoTracks.getOrNull(selectedIndex) ?: videoTracks.firstOrNull()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var scaleType by remember { mutableStateOf(ScaleType.FitInside) }
    var showControls by remember { mutableStateOf(true) }
    var streamMenuExpanded by remember { mutableStateOf(false) }

    val participantId = currentTrackRef?.participant?.identity?.value
    val participantUser = participantId?.let { StoatAPI.userCache[it] }
    val participantName = participantId?.let { displayNameInChannel(it, channelId) }
        ?: stringResource(R.string.unknown)

    val isScreenshareMuted = VoiceCallManager.isScreenshareMutedFor(participantId)

    // Reset zoom when switching tracks
    LaunchedEffect(selectedIndex) {
        scale = 1f
        offset = Offset.Zero
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("screenshare_fullscreen_container")
    ) {
        if (currentTrackRef != null) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                            },
                            onDoubleTap = { tapOffset ->
                                if (scale > 1.05f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                    // Focus zoom around the tap position
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    offset = Offset(
                                        x = (centerX - tapOffset.x) * 1.5f,
                                        y = (centerY - tapOffset.y) * 1.5f
                                    )
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            if (newScale <= 1.01f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                val maxPanX = (size.width * (newScale - 1f)) / 2f
                                val maxPanY = (size.height * (newScale - 1f)) / 2f

                                val newOffsetX = (offset.x + pan.x).coerceIn(-maxPanX, maxPanX)
                                val newOffsetY = (offset.y + pan.y).coerceIn(-maxPanY, maxPanY)

                                scale = newScale
                                offset = Offset(newOffsetX, newOffsetY)
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentAlignment = Alignment.Center
                ) {
                    VideoTrackView(
                        trackReference = currentTrackRef,
                        room = room,
                        scaleType = scaleType,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.voice_status_disconnected),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Top Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -it },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("screenshare_fullscreen_close_button")
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close_24dp),
                                contentDescription = stringResource(R.string.voice_action_exit_fullscreen),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (participantId != null) {
                            UserAvatar(
                                username = participantName,
                                userId = participantId,
                                avatar = participantUser?.avatar,
                                size = 36.dp,
                                modifier = Modifier.clip(CircleShape)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = participantName,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (currentTrackRef?.source == Track.Source.SCREEN_SHARE) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.voice_action_screen_share),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (isScreenshareMuted) {
                                Text(
                                    text = stringResource(R.string.voice_screenshare_muted),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute / Unmute screenshare audio toggle
                        IconButton(
                            onClick = {
                                if (participantId != null) {
                                    VoiceCallManager.toggleScreenshareMuteForParticipant(participantId)
                                } else {
                                    VoiceCallManager.toggleScreenshareMute()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isScreenshareMuted) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                                    else Color.White.copy(alpha = 0.15f)
                                )
                                .testTag("screenshare_fullscreen_mute_button")
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isScreenshareMuted) R.drawable.ic_volume_off_24dp
                                    else R.drawable.ic_volume_up_24dp
                                ),
                                contentDescription = stringResource(
                                    if (isScreenshareMuted) R.string.voice_action_unmute_screenshare
                                    else R.string.voice_action_mute_screenshare
                                ),
                                tint = if (isScreenshareMuted) MaterialTheme.colorScheme.onErrorContainer else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Stream switcher dropdown if multiple streams
                        if (videoTracks.size > 1) {
                            Box {
                                IconButton(
                                    onClick = { streamMenuExpanded = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_devices_24dp),
                                        contentDescription = "Switch stream",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = streamMenuExpanded,
                                    onDismissRequest = { streamMenuExpanded = false }
                                ) {
                                    videoTracks.forEachIndexed { index, trackRef ->
                                        val pId = trackRef.participant.identity?.value
                                        val pName = pId?.let { displayNameInChannel(it, channelId) } ?: "Stream ${index + 1}"
                                        val isScreen = trackRef.source == Track.Source.SCREEN_SHARE
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (isScreen) "$pName (Screen)" else "$pName (Camera)",
                                                    fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            trailingIcon = {
                                                if (index == selectedIndex) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_check_24dp),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedIndex = index
                                                streamMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Zoom & Display Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zoom In / Zoom Out / Percentage Bar
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val nextScale = (scale - 0.5f).coerceIn(1f, 5f)
                                    scale = nextScale
                                    if (nextScale <= 1.01f) offset = Offset.Zero
                                },
                                enabled = scale > 1.01f,
                                modifier = Modifier.size(32.dp).testTag("screenshare_zoom_out_button")
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_remove_24dp),
                                    contentDescription = stringResource(R.string.voice_screenshare_zoom_out),
                                    tint = if (scale > 1.01f) Color.White else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "${(scale * 100).roundToInt()}%",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .testTag("screenshare_zoom_percent")
                            )

                            IconButton(
                                onClick = {
                                    val nextScale = (scale + 0.5f).coerceIn(1f, 5f)
                                    scale = nextScale
                                },
                                enabled = scale < 5f,
                                modifier = Modifier.size(32.dp).testTag("screenshare_zoom_in_button")
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add_24dp),
                                    contentDescription = stringResource(R.string.voice_screenshare_zoom_in),
                                    tint = if (scale < 5f) Color.White else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset Zoom Button (visible when zoomed in)
                        if (scale > 1.05f) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        scale = 1f
                                        offset = Offset.Zero
                                    }
                                    .testTag("screenshare_zoom_reset_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_undo_24dp),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.voice_screenshare_zoom_reset),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
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
