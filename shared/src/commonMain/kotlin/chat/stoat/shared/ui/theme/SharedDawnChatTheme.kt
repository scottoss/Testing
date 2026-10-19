package chat.stoat.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DawnChatDarkColors = darkColorScheme(
    primary = Color(0xFFFF6B6B),
    onPrimary = Color(0xFF2B0808),
    primaryContainer = Color(0xFF5D1919),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFF7AD0FF),
    onSecondary = Color(0xFF003549),
    secondaryContainer = Color(0xFF004C68),
    onSecondaryContainer = Color(0xFFC2E8FF),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF1A1B22),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF262833),
    onSurfaceVariant = Color(0xFFC7C5D0)
)

private val DawnChatLightColors = lightColorScheme(
    primary = Color(0xFFC43232),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF00668A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC2E8FF),
    onSecondaryContainer = Color(0xFF001E2C),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFF6F2FA),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE4E1EC),
    onSurfaceVariant = Color(0xFF46464F)
)

@Composable
fun SharedDawnChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DawnChatDarkColors else DawnChatLightColors,
        content = content
    )
}
