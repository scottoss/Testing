package chat.stoat.composables.voice

import android.app.Activity
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.composables.chat.displayNameInChannel
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.User
import chat.stoat.core.model.util.UserVoiceState
import chat.stoat.voice.VoiceCallManager
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioDeviceChangeListener
import io.livekit.android.compose.local.RoomLocal
import io.livekit.android.compose.state.rememberParticipants
import io.livekit.android.compose.state.rememberTracks
import io.livekit.android.compose.ui.ScaleType
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import io.livekit.android.util.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VoiceSheet(
    onDisconnect: () -> Unit,
    onMinimize: (() -> Unit)? = null,
    onEnterPip: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = onMinimize != null) {
        onMinimize?.invoke()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val room = VoiceCallManager.room
    val channelId = VoiceCallManager.activeChannelId
    if (room == null || channelId == null) return

    CompositionLocalProvider(RoomLocal provides room) {
        val roomState by room::state.flow.collectAsState()
        val isMicOn by room.localParticipant::isMicrophoneEnabled.flow.collectAsState()
        val isCameraOn by room.localParticipant::isCameraEnabled.flow.collectAsState()
        val isScreenShared by room.localParticipant::isScreenShareEnabled.flow.collectAsState()
        val activeSpeakers by room::activeSpeakers.flow.collectAsState()
        val participants by rememberParticipants(room)
        val trackRefs by rememberTracks(passedRoom = room)
        val isDeafened = VoiceCallManager.isDeafened
        val isScreenshareMuted = VoiceCallManager.isScreenshareMuted
        var fullScreenTrackIndex by remember { mutableStateOf<Int?>(null) }

        if (fullScreenTrackIndex != null) {
            ScreenshareFullScreenDialog(
                initialTrackIndex = fullScreenTrackIndex!!,
                room = room,
                channelId = channelId,
                onDismiss = { fullScreenTrackIndex = null }
            )
        }

        val channel = StoatAPI.channelCache[channelId]
        val isDm = channel?.channelType == ChannelType.DirectMessage
        val partnerId = if (isDm) ChannelUtils.resolveDMPartner(channel) else null
        val partnerUser = partnerId?.let { StoatAPI.userCache[it] }
        val headerTitle = if (isDm && partnerUser != null) {
            User.resolveDefaultName(partnerUser)
        } else {
            channel?.let(ChannelUtils::resolveName) ?: stringResource(R.string.voice_notification_fallback_channel_name)
        }

        val audioHandler = room.audioSwitchHandler
        var audioDevices by remember {
            mutableStateOf(audioHandler?.availableAudioDevices ?: emptyList())
        }
        var selectedAudioDevice by remember { mutableStateOf(audioHandler?.selectedAudioDevice) }
        DisposableEffect(audioHandler) {
            val listener: AudioDeviceChangeListener = { devices, selected ->
                audioDevices = devices
                selectedAudioDevice = selected
            }
            audioHandler?.registerAudioDeviceChangeListener(listener)
            onDispose {
                audioHandler?.unregisterAudioDeviceChangeListener(listener)
            }
        }

        // Screen share launcher
        val screenCaptureLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                scope.launch {
                    try {
                        val published = room.localParticipant.setScreenShareEnabled(
                            true,
                            ScreenCaptureParams(data)
                        )
                        logcat("VoiceSheet") { "Screen share enable result: $published" }
                    } catch (e: Exception) {
                        logcat("VoiceSheet", LogPriority.ERROR) {
                            "Could not start screen share\n" + e.asLog()
                        }
                    }
                }
            } else {
                logcat("VoiceSheet", LogPriority.WARN) {
                    "Screen capture consent denied or empty (resultCode ${result.resultCode})"
                }
            }
        }

        var toolbarExpanded by remember { mutableStateOf(false) }
        var outputMenuOpen by remember { mutableStateOf(false) }
        val chevronRotation by animateFloatAsState(
            targetValue = if (toolbarExpanded) 270f else 90f,
            label = "voice toolbar chevron"
        )

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Call Info Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (onMinimize != null) {
                        IconButton(
                            onClick = onMinimize,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_forward_24dp),
                                contentDescription = stringResource(R.string.cancel),
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer { rotationZ = 90f },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (partnerUser != null) {
                        UserAvatar(
                            username = headerTitle,
                            userId = partnerUser.id ?: "",
                            avatar = partnerUser.avatar,
                            size = 42.dp,
                            modifier = Modifier.clip(CircleShape)
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_voice_chat_24dp),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Pulsing green dot if connected
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dotAlpha"
                            )
                            val dotColor = when (roomState) {
                                Room.State.CONNECTED -> Color(0xFF22C55E)
                                Room.State.CONNECTING, Room.State.RECONNECTING -> Color(0xFFF59E0B)
                                Room.State.DISCONNECTED -> MaterialTheme.colorScheme.error
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor.copy(alpha = if (roomState == Room.State.CONNECTED) pulseAlpha else 1f))
                            )
                            Text(
                                text = when (roomState) {
                                    Room.State.CONNECTING -> stringResource(R.string.voice_status_connecting)
                                    Room.State.CONNECTED -> stringResource(R.string.voice_status_connected)
                                    Room.State.DISCONNECTED -> stringResource(R.string.voice_status_disconnected)
                                    Room.State.RECONNECTING -> stringResource(R.string.voice_status_reconnecting)
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (roomState == Room.State.CONNECTED) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (audioHandler != null) {
                        Box {
                            IconButton(
                                onClick = { outputMenuOpen = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_volume_up_24dp),
                                    contentDescription = stringResource(R.string.voice_audio_output),
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = outputMenuOpen,
                                onDismissRequest = { outputMenuOpen = false }
                            ) {
                                audioDevices.forEach { device ->
                                    DropdownMenuItem(
                                        text = { Text(audioDeviceLabel(device)) },
                                        trailingIcon = {
                                            if (device == selectedAudioDevice) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_check_24dp),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            audioHandler.selectDevice(device)
                                            outputMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (onEnterPip != null) {
                        IconButton(
                            onClick = onEnterPip,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_fullscreen_exit_24dp),
                                contentDescription = "Picture in Picture",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Main stage for video / screenshare or audio visualizer
            val tracksVersion = VoiceCallManager.tracksVersion
            val activeTrackIndices = remember(trackRefs, tracksVersion, isCameraOn, isScreenShared) {
                trackRefs.indices.filter { idx ->
                    val ref = trackRefs[idx]
                    val pub = ref.publication
                    val part = ref.participant
                    pub != null && !pub.muted && pub.track != null &&
                    when (ref.source) {
                        Track.Source.CAMERA -> part.isCameraEnabled
                        Track.Source.SCREEN_SHARE -> part.isScreenShareEnabled
                        else -> true
                    }
                }
            }
            var focusedTrackIndex by remember { mutableStateOf<Int?>(null) }
            val effectiveFocusedIndex = remember(activeTrackIndices, focusedTrackIndex) {
                if (activeTrackIndices.isEmpty()) null
                else if (focusedTrackIndex != null && focusedTrackIndex in activeTrackIndices) focusedTrackIndex
                else {
                    activeTrackIndices.firstOrNull { trackRefs[it].source == Track.Source.SCREEN_SHARE }
                        ?: activeTrackIndices.firstOrNull()
                }
            }
            var stageScale by remember { mutableFloatStateOf(1f) }
            var stageOffset by remember { mutableStateOf(Offset.Zero) }

            LaunchedEffect(effectiveFocusedIndex) {
                stageScale = 1f
                stageOffset = Offset.Zero
            }

            if (effectiveFocusedIndex != null && effectiveFocusedIndex in trackRefs.indices) {
                val featuredTrack = trackRefs[effectiveFocusedIndex]
                val featuredParticipantId = featuredTrack.participant.identity?.value
                val isFeaturedScreenshare = featuredTrack.source == Track.Source.SCREEN_SHARE
                val isThisTrackMuted = VoiceCallManager.isScreenshareMutedFor(featuredParticipantId)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Featured Video Stage (takes full central stage)
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0F172A))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        val maxPanX = (constraints.maxWidth * (stageScale - 1f) / 2f).coerceAtLeast(0f)
                        val maxPanY = (constraints.maxHeight * (stageScale - 1f) / 2f).coerceAtLeast(0f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(effectiveFocusedIndex) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            stageScale = if (stageScale > 1.2f) 1f else 2.5f
                                            stageOffset = Offset.Zero
                                        }
                                    )
                                }
                                .pointerInput(effectiveFocusedIndex) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (stageScale * zoom).coerceIn(1f, 5f)
                                        stageScale = newScale
                                        if (newScale <= 1f) {
                                            stageOffset = Offset.Zero
                                        } else {
                                            val currentMaxX = (constraints.maxWidth * (newScale - 1f) / 2f).coerceAtLeast(0f)
                                            val currentMaxY = (constraints.maxHeight * (newScale - 1f) / 2f).coerceAtLeast(0f)
                                            stageOffset = Offset(
                                                x = (stageOffset.x + pan.x).coerceIn(-currentMaxX, currentMaxX),
                                                y = (stageOffset.y + pan.y).coerceIn(-currentMaxY, currentMaxY)
                                            )
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    scaleX = stageScale
                                    scaleY = stageScale
                                    translationX = stageOffset.x
                                    translationY = stageOffset.y
                                }
                        ) {
                            if (isVideoTrackActive(featuredTrack)) {
                                VideoTrackView(
                                    trackReference = featuredTrack,
                                    room = room,
                                    scaleType = ScaleType.FitInside,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Top Controls Overlay on Stage: Reset Zoom, Mute Screenshare, Fullscreen
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (stageScale > 1.05f) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            stageScale = 1f
                                            stageOffset = Offset.Zero
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_close_24dp),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${(stageScale * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                            }

                            if (isFeaturedScreenshare) {
                                Surface(
                                    color = if (isThisTrackMuted) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (featuredParticipantId != null) {
                                                VoiceCallManager.toggleScreenshareMuteForParticipant(featuredParticipantId)
                                            } else {
                                                VoiceCallManager.toggleScreenshareMute()
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(
                                                if (isThisTrackMuted) R.drawable.ic_volume_off_24dp
                                                else R.drawable.ic_volume_up_24dp
                                            ),
                                            contentDescription = stringResource(
                                                if (isThisTrackMuted) R.string.voice_action_unmute_screenshare
                                                else R.string.voice_action_mute_screenshare
                                            ),
                                            tint = if (isThisTrackMuted) MaterialTheme.colorScheme.onErrorContainer else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { fullScreenTrackIndex = effectiveFocusedIndex }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_fullscreen_24dp),
                                        contentDescription = stringResource(R.string.voice_action_fullscreen),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Bottom-left info chip on Stage
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isFeaturedScreenshare) R.drawable.ic_screen_share_24dp
                                        else R.drawable.ic_videocam_24dp
                                    ),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = featuredParticipantId?.let { displayNameInChannel(it, channelId) }
                                        ?: stringResource(R.string.unknown),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                if (isFeaturedScreenshare && isThisTrackMuted) {
                                    Text(
                                        text = "• " + stringResource(R.string.voice_muted),
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                                    )
                                }
                            }
                        }
                    }

                    // Horizontal Thumbnails Strip (other video tracks & voice participants)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(activeTrackIndices.size) { i ->
                            val trackIdx = activeTrackIndices[i]
                            val track = trackRefs[trackIdx]
                            val isFocused = trackIdx == effectiveFocusedIndex
                            val trackParticipantId = track.participant.identity?.value

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .width(105.dp)
                                    .height(65.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { focusedTrackIndex = trackIdx }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (isVideoTrackActive(track)) {
                                        VideoTrackView(
                                            trackReference = track,
                                            room = room,
                                            scaleType = ScaleType.FitInside,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = trackParticipantId?.let { displayNameInChannel(it, channelId) } ?: "",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Voice participants in strip
                        items(participants.size) { pIdx ->
                            val participant = participants[pIdx]
                            val pUserId = participant.identity?.value
                            if (pUserId != null && activeTrackIndices.none { trackRefs[it].participant.identity?.value == pUserId }) {
                                val isSpeaking = activeSpeakers.any { it.identity == participant.identity }
                                val pUser = StoatAPI.userCache[pUserId]
                                val pName = displayNameInChannel(pUserId, channelId)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSpeaking) 2.dp else 1.dp,
                                        color = if (isSpeaking) Color(0xFF22C55E) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .height(65.dp)
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        UserAvatar(
                                            username = pName,
                                            userId = pUserId,
                                            avatar = pUser?.avatar,
                                            size = 32.dp,
                                            allowAnimation = isSpeaking,
                                            modifier = Modifier.clip(CircleShape)
                                        )
                                        Column(verticalArrangement = Arrangement.Center) {
                                            Text(
                                                text = pName,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isSpeaking) "Speaking" else "Connected",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    color = if (isSpeaking) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Pure Audio Call View - expands across the full page
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val mainSpeaker = activeSpeakers.firstOrNull() ?: participants.firstOrNull { it != room.localParticipant } ?: room.localParticipant
                        val isSpeaking = activeSpeakers.contains(mainSpeaker)
                        val mainSpeakerUser = StoatAPI.userCache[mainSpeaker.identity?.value]
                        val mainSpeakerName = if (mainSpeaker.identity?.value != null) {
                            displayNameInChannel(mainSpeaker.identity!!.value, channelId)
                        } else headerTitle

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSpeaking) {
                                    Box(
                                        modifier = Modifier
                                            .size(116.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E).copy(alpha = 0.25f))
                                    )
                                }
                                UserAvatar(
                                    username = mainSpeakerName,
                                    userId = mainSpeaker.identity?.value ?: "",
                                    avatar = mainSpeakerUser?.avatar ?: partnerUser?.avatar,
                                    size = 96.dp,
                                    allowAnimation = isSpeaking,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSpeaking) 3.dp else 1.5.dp,
                                            color = if (isSpeaking) Color(0xFF22C55E) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = mainSpeakerName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isSpeaking) "Speaking…" else stringResource(R.string.voice_notification_ongoing_call),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isSpeaking) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Voice Participants
                    val voiceStates = StoatAPI.voiceStateCache[channelId]
                    items(participants.size) { index ->
                        val participant = participants[index]
                        val userId = participant.identity?.value
                        if (userId != null) {
                            val micEnabled by participant::isMicrophoneEnabled.flow.collectAsState()
                            val cameraEnabled by participant::isCameraEnabled.flow.collectAsState()
                            val screenShareEnabled by participant::isScreenShareEnabled.flow.collectAsState()
                            val cachedState = voiceStates?.participants?.find { it.id == userId }
                            VoiceParticipant(
                                state = UserVoiceState(
                                    id = userId,
                                    isReceiving = cachedState?.isReceiving ?: true,
                                    isPublishing = micEnabled,
                                    screensharing = screenShareEnabled,
                                    camera = cameraEnabled,
                                    joinedAt = cachedState?.joinedAt
                                ),
                                channelId = channelId,
                                speaking = activeSpeakers.any { it.identity == participant.identity }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(VoiceCallManager.errorResource != null) {
                VoiceCallManager.errorResource?.let { resId ->
                    Text(
                        text = stringResource(resId),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }

            // Expanded Controls Drawer
            AnimatedVisibility(
                visible = toolbarExpanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Video Camera
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = if (isCameraOn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                        ),
                        headlineContent = { Text(stringResource(R.string.voice_action_video)) },
                        leadingContent = {
                            Icon(
                                painter = if (isCameraOn) painterResource(R.drawable.ic_videocam_24dp) else painterResource(R.drawable.ic_videocam_off_24dp),
                                contentDescription = null,
                                tint = if (isCameraOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                scope.launch {
                                    room.localParticipant.setCameraEnabled(!isCameraOn)
                                }
                            }
                    )

                    // Flip camera
                    AnimatedVisibility(visible = isCameraOn) {
                        ListItem(
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                            ),
                            headlineContent = { Text(stringResource(R.string.voice_action_flip_camera)) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_cameraswitch_24dp),
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    (room.localParticipant
                                        .getTrackPublication(Track.Source.CAMERA)
                                        ?.track as? LocalVideoTrack)
                                        ?.switchCamera()
                                }
                        )
                    }

                    // Screen Share
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = if (isScreenShared) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                        ),
                        headlineContent = { Text(stringResource(R.string.voice_action_screen_share)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_mobile_share_24px),
                                contentDescription = null,
                                tint = if (isScreenShared) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (isScreenShared) {
                                    scope.launch {
                                        room.localParticipant.setScreenShareEnabled(false)
                                    }
                                } else {
                                    context.getSystemService(MediaProjectionManager::class.java)
                                        ?.let { manager ->
                                            screenCaptureLauncher.launch(
                                                manager.createScreenCaptureIntent()
                                            )
                                        }
                                }
                            }
                    )

                    // Audio Output
                    if (audioHandler != null) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ListItem(
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                                ),
                                headlineContent = { Text(stringResource(R.string.voice_audio_output)) },
                                supportingContent = {
                                    selectedAudioDevice?.let { Text(audioDeviceLabel(it)) }
                                },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_volume_up_24dp),
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { outputMenuOpen = true }
                            )
                            DropdownMenu(
                                expanded = outputMenuOpen,
                                onDismissRequest = { outputMenuOpen = false }
                            ) {
                                audioDevices.forEach { device ->
                                    DropdownMenuItem(
                                        text = { Text(audioDeviceLabel(device)) },
                                        trailingIcon = {
                                            if (device == selectedAudioDevice) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_check_24dp),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            audioHandler.selectDevice(device)
                                            outputMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Mute Screenshare Audio Toggle
                    ListItem(
                        colors = ListItemDefaults.colors(
                            containerColor = if (isScreenshareMuted) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                        ),
                        headlineContent = { Text(stringResource(R.string.voice_action_mute_all_screenshares)) },
                        supportingContent = {
                            Text(
                                if (isScreenshareMuted) stringResource(R.string.voice_screenshare_muted)
                                else stringResource(R.string.voice_action_unmute_screenshare)
                            )
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(
                                    if (isScreenshareMuted) R.drawable.ic_volume_off_24dp
                                    else R.drawable.ic_volume_up_24dp
                                ),
                                contentDescription = null,
                                tint = if (isScreenshareMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                VoiceCallManager.toggleScreenshareMute()
                            }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Action Toolbar (Mic, Video, Screen Share, Deafen, More, End Call)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (roomState == Room.State.DISCONNECTED) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                VoiceCallManager.openCall(channelId)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF22C55E),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_call_24dp__fill),
                                contentDescription = stringResource(R.string.voice_action_join_call),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.voice_action_join_call),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Microphone Button
                    val micBgColor = if (isMicOn) Color(0xFF2563EB) else MaterialTheme.colorScheme.surfaceContainer
                    val micFgColor = if (isMicOn) Color.White else MaterialTheme.colorScheme.onSurface
                    Button(
                        onClick = { VoiceCallManager.toggleMicrophone() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = micBgColor,
                            contentColor = micFgColor
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(
                            painter = if (isMicOn) painterResource(R.drawable.ic_mic_24dp) else painterResource(R.drawable.ic_mic_off_24dp),
                            contentDescription = stringResource(if (isMicOn) R.string.voice_action_mute else R.string.voice_action_unmute),
                            tint = micFgColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Video Camera Button
                    val videoBgColor = if (isCameraOn) Color(0xFF2563EB) else MaterialTheme.colorScheme.surfaceContainer
                    val videoFgColor = if (isCameraOn) Color.White else MaterialTheme.colorScheme.onSurface
                    Button(
                        onClick = {
                            scope.launch {
                                room.localParticipant.setCameraEnabled(!isCameraOn)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = videoBgColor,
                            contentColor = videoFgColor
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(
                            painter = if (isCameraOn) painterResource(R.drawable.ic_videocam_24dp) else painterResource(R.drawable.ic_videocam_off_24dp),
                            contentDescription = stringResource(R.string.voice_action_video),
                            tint = videoFgColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Screen Share Button
                    val shareBgColor = if (isScreenShared) Color(0xFF2563EB) else MaterialTheme.colorScheme.surfaceContainer
                    val shareFgColor = if (isScreenShared) Color.White else MaterialTheme.colorScheme.onSurface
                    Button(
                        onClick = {
                            if (isScreenShared) {
                                scope.launch {
                                    room.localParticipant.setScreenShareEnabled(false)
                                }
                            } else {
                                context.getSystemService(MediaProjectionManager::class.java)
                                    ?.let { manager ->
                                        screenCaptureLauncher.launch(
                                            manager.createScreenCaptureIntent()
                                        )
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = shareBgColor,
                            contentColor = shareFgColor
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_screen_share_24dp),
                            contentDescription = stringResource(R.string.voice_action_screen_share),
                            tint = shareFgColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Deafen Button
                    val deafenBgColor = if (!isDeafened) Color(0xFF2563EB) else MaterialTheme.colorScheme.surfaceContainer
                    val deafenFgColor = if (!isDeafened) Color.White else MaterialTheme.colorScheme.error
                    Button(
                        onClick = { VoiceCallManager.toggleDeafen() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = deafenBgColor,
                            contentColor = deafenFgColor
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(
                            painter = if (isDeafened) painterResource(R.drawable.ic_headset_off_24dp) else painterResource(R.drawable.ic_headset_24dp),
                            contentDescription = stringResource(if (isDeafened) R.string.voice_action_undeafen else R.string.voice_action_deafen),
                            tint = deafenFgColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // More Options / Expand Button
                    Button(
                        onClick = { toolbarExpanded = !toolbarExpanded },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_forward_24dp),
                            contentDescription = null,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer { rotationZ = chevronRotation }
                        )
                    }

                    // Disconnect / End Call Button
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(26.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_call_end_24dp__fill),
                            contentDescription = stringResource(R.string.voice_action_disconnect),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun audioDeviceLabel(device: AudioDevice): String = when (device) {
    is AudioDevice.BluetoothHeadset -> device.name
    is AudioDevice.WiredHeadset -> stringResource(R.string.voice_audio_output_wired_headset)
    is AudioDevice.Earpiece -> stringResource(R.string.voice_audio_output_earpiece)
    is AudioDevice.Speakerphone -> stringResource(R.string.voice_audio_output_speaker)
}
