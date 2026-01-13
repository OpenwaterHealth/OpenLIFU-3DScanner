package health.openwater.openlifu3dscanner.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/* --- Light theme --- */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF497073),      // Green 800
    onPrimary = Color.White,

    secondary = Color(0xFF66BB6A),    // Green 400
    onSecondary = Color.Black,

    primaryContainer = Color(0xFF497073),
    onPrimaryContainer = Color.White,

    secondaryContainer = Color(0xFF2C4346),
    onSecondaryContainer = Color.White,

    tertiary = Color(0xFF26A69A),     // Teal-ish accent
    onTertiary = Color.Black,

    background = Color(0xFFFFFFFF),  // White
    onBackground = Color(0xFF1C1B1F),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F)
)

/* --- Dark theme --- */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF62979A),      // Green 300
    onPrimary = Color.White,

    secondary = Color(0xFFA5D6A7),    // Green 200
    onSecondary = Color(0xFF003300),

    primaryContainer = Color(0xFF497073),
    onPrimaryContainer = Color.White,

    secondaryContainer = Color(0xFF2C4346),
    onSecondaryContainer = Color.White,

    tertiary = Color(0xFF4DB6AC),
    onTertiary = Color(0xFF00201A),

    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E1E5),

    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E1E5)
)

@Composable
fun HeadScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
