package chat.stoat.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

@Composable
expect fun PlatformLoopingVideoView(
    url: String,
    modifier: Modifier = Modifier
)

expect class PlatformAudioPlayer() {
    val isPlaying: StateFlow<Boolean>
    val currentUrl: StateFlow<String?>

    fun play(url: String)
    fun pause()
    fun stop()
}
