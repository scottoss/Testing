package chat.stoat.voice

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.stoat.DawnChatApplication
import chat.stoat.persistence.KVStorage
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.ScreenSharePresets
import io.livekit.android.room.track.VideoPreset

enum class VoiceOutputRoute { Auto, Speaker, Earpiece, Bluetooth }

enum class ScreenShareQuality(val preset: VideoPreset) {
    Low(ScreenSharePresets.H720_FPS5),
    High(ScreenSharePresets.H720_FPS30),
    Text(ScreenSharePresets.H1080_FPS15),
}

/** Device-local voice preferences, mirroring the web client's voice store. */
object VoiceSettings {
    private const val PREFIX = "voice."

    var noiseSuppression by mutableStateOf(true)
    var echoCancellation by mutableStateOf(true)
    var autoGainControl by mutableStateOf(true)
    var outputVolume by mutableFloatStateOf(1f)
    var outputRoute by mutableStateOf(VoiceOutputRoute.Auto)
    var defaultCameraFront by mutableStateOf(true)
    var screenShareQuality by mutableStateOf(ScreenShareQuality.High)

    private var loaded = false

    suspend fun ensureLoaded() {
        if (loaded) return
        val kv = KVStorage(DawnChatApplication.instance)
        noiseSuppression = kv.getBoolean(PREFIX + "noiseSuppression") ?: true
        echoCancellation = kv.getBoolean(PREFIX + "echoCancellation") ?: true
        autoGainControl = kv.getBoolean(PREFIX + "autoGainControl") ?: true
        outputVolume = (kv.getInt(PREFIX + "outputVolume") ?: 100) / 100f
        outputRoute = kv.get(PREFIX + "outputRoute")
            ?.let { runCatching { VoiceOutputRoute.valueOf(it) }.getOrNull() }
            ?: VoiceOutputRoute.Auto
        defaultCameraFront = kv.getBoolean(PREFIX + "defaultCameraFront") ?: true
        screenShareQuality = kv.get(PREFIX + "screenShareQuality")
            ?.let { runCatching { ScreenShareQuality.valueOf(it) }.getOrNull() }
            ?: ScreenShareQuality.High
        loaded = true
    }

    suspend fun persist() {
        val kv = KVStorage(DawnChatApplication.instance)
        kv.set(PREFIX + "noiseSuppression", noiseSuppression)
        kv.set(PREFIX + "echoCancellation", echoCancellation)
        kv.set(PREFIX + "autoGainControl", autoGainControl)
        kv.set(PREFIX + "outputVolume", (outputVolume * 100).toInt())
        kv.set(PREFIX + "outputRoute", outputRoute.name)
        kv.set(PREFIX + "defaultCameraFront", defaultCameraFront)
        kv.set(PREFIX + "screenShareQuality", screenShareQuality.name)
    }

    val cameraPosition: CameraPosition
        get() = if (defaultCameraFront) CameraPosition.FRONT else CameraPosition.BACK

    fun applyOutputRoute(context: Context) {
        if (outputRoute == VoiceOutputRoute.Auto) return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wanted = am.availableCommunicationDevices.firstOrNull { it.type in outputRoute.deviceTypes() }
            if (wanted != null) am.setCommunicationDevice(wanted)
        } else {
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = outputRoute == VoiceOutputRoute.Speaker
        }
    }

    private fun VoiceOutputRoute.deviceTypes(): Set<Int> = when (this) {
        VoiceOutputRoute.Speaker -> setOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
        VoiceOutputRoute.Earpiece -> setOf(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
        VoiceOutputRoute.Bluetooth -> setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
        )
        VoiceOutputRoute.Auto -> emptySet()
    }
}
