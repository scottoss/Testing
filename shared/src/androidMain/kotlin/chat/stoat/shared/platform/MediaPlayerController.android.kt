package chat.stoat.shared.platform

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AndroidMediaBridge {
    var loopingVideoRenderer: (@Composable (url: String, modifier: Modifier) -> Unit)? = null
}

@Composable
actual fun PlatformLoopingVideoView(
    url: String,
    modifier: Modifier
) {
    val customRenderer = AndroidMediaBridge.loopingVideoRenderer
    if (customRenderer != null) {
        customRenderer(url, modifier)
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Animated GIF Video (${url.substringAfterLast('/')})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

actual class PlatformAudioPlayer actual constructor() {
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    actual val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentUrl = MutableStateFlow<String?>(null)
    actual val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    actual fun play(url: String) {
        stop()
        runCatching {
            val mp = MediaPlayer()
            mp.setDataSource(url)
            mp.setOnPreparedListener {
                it.start()
                _isPlaying.value = true
                _currentUrl.value = url
            }
            mp.setOnCompletionListener {
                _isPlaying.value = false
            }
            mp.prepareAsync()
            mediaPlayer = mp
        }
    }

    actual fun pause() {
        runCatching {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _isPlaying.value = false
            }
        }
    }

    actual fun stop() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentUrl.value = null
    }
}
