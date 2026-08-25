package fjallkartan.fjallkartan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E4F),
    secondary = Color(0xFFF28C28),
    surface = Color(0xFFF8F8F4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF62D6A7),
    secondary = Color(0xFFFFB66D),
)

@Composable
fun FjallkartanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
