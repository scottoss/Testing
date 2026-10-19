package chat.stoat.voice

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.stoat.R
import chat.stoat.DawnChatApplication
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.api.routes.misc.LiveKitNode
import chat.stoat.api.routes.misc.Root
import chat.stoat.api.routes.misc.getRootRoute
import chat.stoat.api.routes.voice.JoinCallResponse
import chat.stoat.api.routes.voice.joinCall
import chat.stoat.composables.voice.VoiceSound
import chat.stoat.composables.voice.VoiceSoundPlayer
import chat.stoat.services.OngoingCallService
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalAudioTrackOptions
import io.livekit.android.room.track.LocalVideoTrackOptions
import io.livekit.android.room.track.RemoteAudioTrack
import io.livekit.android.room.track.ScreenSharePresets
import io.livekit.android.room.track.Track
import io.livekit.android.util.flow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import kotlin.time.Duration.Companion.seconds

/**
 * Owns the lifecycle of the current voice call.
 * The LiveKit [Room] lives here
 */
object VoiceCallManager {
    private val context: Context get() = DawnChatApplication.instance
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var activeChannelId: String? by mutableStateOf(null)
        private set

    var room: Room? by mutableStateOf(null)
        private set

    var errorResource: Int? by mutableStateOf<Int?>(null)
        private set

    var isDeafened: Boolean by mutableStateOf(false)
        private set

    var isScreenshareMuted: Boolean by mutableStateOf(true)
        private set

    val mutedScreenshareParticipants = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()

    var isSheetVisible: Boolean by mutableStateOf(false)

    var isFloatingPipVisible: Boolean by mutableStateOf(true)

    var tracksVersion: Long by mutableLongStateOf(0L)
        private set

    fun notifyTracksChanged() {
        tracksVersion++
    }

    var requestedChannelId: String? by mutableStateOf(null)
        private set

    fun openCall(channelId: String, openFullPage: Boolean = true) {
        requestedChannelId = channelId
        isFloatingPipVisible = true
        isSheetVisible = openFullPage
        join(channelId)
    }

    fun openSheet(channelId: String) {
        openCall(channelId, openFullPage = true)
    }

    fun dismissFloatingPip() {
        isFloatingPipVisible = false
    }

    fun showFloatingPip() {
        isFloatingPipVisible = true
    }

    private val probeClient by lazy { HttpClient(OkHttp) }

    private var micWasOnBeforeDeafen = false
    private var soundPlayer: VoiceSoundPlayer? = null
    private var sessionJob: Job? = null
    private var hasPlayedJoinSound = false
    private var hasPlayedLeaveSound = false

    init {
        scope.launch {
            OngoingCallService.hangupEvents.collect { leave() }
        }
    }

    fun join(channelId: String) {
        if (activeChannelId == channelId && room != null) return
        endSession()

        activeChannelId = channelId
        errorResource = null
        hasPlayedJoinSound = false
        hasPlayedLeaveSound = false
        soundPlayer = VoiceSoundPlayer(context)

        sessionJob = scope.launch {
            VoiceSettings.ensureLoaded()
            val newRoom = LiveKit.create(
                context.applicationContext,
                RoomOptions(
                    audioTrackCaptureDefaults = LocalAudioTrackOptions(
                        noiseSuppression = VoiceSettings.noiseSuppression,
                        echoCancellation = VoiceSettings.echoCancellation,
                        autoGainControl = VoiceSettings.autoGainControl,
                    ),
                    videoTrackCaptureDefaults = LocalVideoTrackOptions(
                        position = VoiceSettings.cameraPosition
                    ),
                    screenShareTrackCaptureDefaults = LocalVideoTrackOptions(
                        isScreencast = true,
                        captureParams = VoiceSettings.screenShareQuality.preset.capture
                    )
                )
            )
            room = newRoom
            VoiceSettings.applyOutputRoute(context)
            launch { watchRoomState(newRoom, channelId) }
            launch { watchRoomEvents(newRoom) }
            launch { watchLocalCamera(newRoom) }
            launch { watchLocalScreenShare(newRoom) }
            launch { watchLocalMicrophone(newRoom) }

            val connection = fetchVoiceToken(channelId) ?: return@launch
            try {
                newRoom.connect(connection.url, connection.token)
                applyAudioRouting(newRoom)
                val hasRecordAudio = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (hasRecordAudio) {
                    runCatching {
                        newRoom.localParticipant.setMicrophoneEnabled(true)
                    }
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "Could not connect to LiveKit room\n" + e.asLog() }
                errorResource = R.string.voice_error_generic
            }
        }
    }

    fun leave() {
        isSheetVisible = false
        isFloatingPipVisible = true
        requestedChannelId = null
        endSession()
    }

    private fun endSession() {
        sessionJob?.cancel()
        sessionJob = null

        val leftRoom = room
        room = null
        activeChannelId = null
        errorResource = null
        isDeafened = false
        isScreenshareMuted = true
        mutedScreenshareParticipants.clear()
        micWasOnBeforeDeafen = false

        if (leftRoom != null) {
            if (hasPlayedJoinSound && !hasPlayedLeaveSound) {
                hasPlayedLeaveSound = true
                soundPlayer?.play(VoiceSound.USER_LEAVE)
            }
            leftRoom.disconnect()
            leftRoom.release()
        }
        soundPlayer?.release()
        soundPlayer = null
        OngoingCallService.stop(context)
    }

    fun toggleMicrophone() {
        val room = room ?: return
        val isMicOn = room.localParticipant.isMicrophoneEnabled
        soundPlayer?.play(if (isMicOn) VoiceSound.MUTE else VoiceSound.UNMUTE)
        scope.launch {
            runCatching {
                room.localParticipant.setMicrophoneEnabled(!isMicOn)
            }
        }
    }

    fun toggleDeafen() {
        val room = room ?: return
        if (!isDeafened) {
            soundPlayer?.play(VoiceSound.DEAFEN)
            micWasOnBeforeDeafen = room.localParticipant.isMicrophoneEnabled
            isDeafened = true
            applyAudioRouting(room)
            scope.launch {
                runCatching {
                    room.localParticipant.setMicrophoneEnabled(false)
                }
            }
        } else {
            soundPlayer?.play(VoiceSound.UNDEAFEN)
            isDeafened = false
            applyAudioRouting(room)
            if (micWasOnBeforeDeafen) {
                scope.launch {
                    runCatching {
                        room.localParticipant.setMicrophoneEnabled(true)
                    }
                }
            }
        }
    }

    fun applyOutputSettings() {
        applyAudioRouting()
        VoiceSettings.applyOutputRoute(context)
    }

    fun toggleScreenshareMute() {
        val room = room ?: return
        isScreenshareMuted = !isScreenshareMuted
        applyAudioRouting(room)
    }

    fun isScreenshareMutedFor(participantId: String?): Boolean {
        if (isScreenshareMuted) return true
        if (participantId != null && mutedScreenshareParticipants[participantId] == true) return true
        return false
    }

    fun toggleScreenshareMuteForParticipant(participantId: String) {
        val room = room ?: return
        val current = mutedScreenshareParticipants[participantId] ?: false
        mutedScreenshareParticipants[participantId] = !current
        applyAudioRouting(room)
    }

    fun applyAudioRouting(room: Room? = null) {
        val targetRoom = room ?: this.room ?: return
        val baseVolume = VoiceSettings.outputVolume.toDouble()
        targetRoom.remoteParticipants.values.forEach { participant ->
            val participantId = participant.identity?.value
            val isParticipantShareMuted = isScreenshareMuted || (participantId != null && mutedScreenshareParticipants[participantId] == true)
            participant.trackPublications.values.forEach { pub ->
                val track = pub.track
                if (track is RemoteAudioTrack) {
                    val isScreenAudio = pub.source == Track.Source.SCREEN_SHARE_AUDIO || pub.source == Track.Source.SCREEN_SHARE
                    val volume = if (isDeafened) {
                        0.0
                    } else if (isScreenAudio && isParticipantShareMuted) {
                        0.0
                    } else {
                        baseVolume
                    }
                    track.setVolume(volume)
                }
            }
        }
    }

    private fun applyDeafenState(room: Room) {
        applyAudioRouting(room)
    }

    private suspend fun watchRoomState(room: Room, channelId: String) {
        room::state.flow.collect { state ->
            when (state) {
                Room.State.CONNECTED -> {
                    if (!hasPlayedJoinSound) {
                        hasPlayedJoinSound = true
                        soundPlayer?.play(VoiceSound.USER_JOIN)
                        OngoingCallService.start(
                            context,
                            channelId,
                            StoatAPI.channelCache[channelId]?.let(ChannelUtils::resolveName)
                        )
                    }
                }

                Room.State.DISCONNECTED -> {
                    if (hasPlayedJoinSound) leave()
                }

                Room.State.CONNECTING, Room.State.RECONNECTING -> Unit
            }
        }
    }

    private suspend fun watchRoomEvents(room: Room) {
        room.events.events.collect { event ->
            when (event) {
                is RoomEvent.TrackSubscribed -> {
                    tracksVersion++
                    applyDeafenState(room)
                }

                is RoomEvent.TrackUnsubscribed -> {
                    tracksVersion++
                }

                is RoomEvent.TrackMuted -> {
                    tracksVersion++
                }

                is RoomEvent.TrackUnmuted -> {
                    tracksVersion++
                }

                is RoomEvent.ParticipantConnected -> {
                    tracksVersion++
                    soundPlayer?.play(VoiceSound.USER_JOIN)
                }

                is RoomEvent.ParticipantDisconnected -> {
                    tracksVersion++
                    soundPlayer?.play(VoiceSound.USER_LEAVE)
                }

                is RoomEvent.TrackPublished -> {
                    tracksVersion++
                    if (event.participant != room.localParticipant &&
                        event.publication.source == Track.Source.SCREEN_SHARE
                    ) {
                        soundPlayer?.play(VoiceSound.STREAM_START)
                    }
                }

                is RoomEvent.TrackUnpublished -> {
                    tracksVersion++
                    if (event.participant != room.localParticipant &&
                        event.publication.source == Track.Source.SCREEN_SHARE
                    ) {
                        soundPlayer?.play(VoiceSound.STREAM_END)
                    }
                }

                else -> Unit
            }
        }
    }

    private suspend fun watchLocalMicrophone(room: Room) {
        room.localParticipant::isMicrophoneEnabled.flow.collect { enabled ->
            OngoingCallService.updateMicMuted(context, muted = !enabled)
        }
    }

    private suspend fun watchLocalCamera(room: Room) {
        room.localParticipant::isCameraEnabled.flow.collect {
            tracksVersion++
        }
    }

    private suspend fun watchLocalScreenShare(room: Room) {
        var wasSharing: Boolean? = null
        room.localParticipant::isScreenShareEnabled.flow.collect { isSharing ->
            tracksVersion++
            wasSharing?.let { previous ->
                if (isSharing && !previous) soundPlayer?.play(VoiceSound.STREAM_START)
                if (!isSharing && previous) soundPlayer?.play(VoiceSound.STREAM_END)
            }
            wasSharing = isSharing
        }
    }

    private suspend fun fastestNode(nodes: List<LiveKitNode>): LiveKitNode? =
        withTimeoutOrNull(5.seconds) {
            try {
                coroutineScope {
                    val winner = CompletableDeferred<LiveKitNode>()
                    val probes = nodes.map { node ->
                        launch {
                            try {
                                val probeUrl = node.publicUrl
                                    .replaceFirst("wss://", "https://")
                                    .replaceFirst("ws://", "http://")
                                if (probeClient.get(probeUrl).status.isSuccess()) {
                                    winner.complete(node)
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                    launch {
                        probes.joinAll()
                        winner.completeExceptionally(
                            IllegalStateException("No LiveKit node reachable")
                        )
                    }
                    try {
                        winner.await()
                    } finally {
                        coroutineContext.cancelChildren()
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

    private suspend fun fetchVoiceToken(channelId: String): JoinCallResponse? {
        val root: Root
        try {
            root = getRootRoute()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Could not get root route\n" + e.asLog() }
            errorResource = R.string.voice_error_generic
            return null
        }

        val lk = root.features.livekit

        if (lk == null) {
            logcat(LogPriority.ERROR) {
                IllegalStateException("LiveKit is not supported by this API version!").asLog()
            }
            errorResource = R.string.voice_error_not_supported
            return null
        }

        if (lk.nodes.isEmpty()) {
            logcat(LogPriority.ERROR) { IllegalStateException("No LiveKit nodes available!").asLog() }
            errorResource = R.string.voice_error_no_nodes
            return null
        }

        val node = fastestNode(lk.nodes)
        if (node == null) {
            logcat(LogPriority.ERROR) { "No LiveKit node responded to probing!" }
            errorResource = R.string.voice_error_no_nodes
            return null
        }

        return try {
            joinCall(channelId, node.name)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Could not get LiveKit token\n" + e.asLog() }
            errorResource = R.string.voice_error_generic
            null
        }
    }
}
