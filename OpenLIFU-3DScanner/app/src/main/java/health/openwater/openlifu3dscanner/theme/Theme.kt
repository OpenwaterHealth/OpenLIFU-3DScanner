package health.openwater.openlifu3dscanner.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/* --- Light theme --- */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC83010),           // Deep red from icon outer arcs
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD3),  // Pale red-pink
    onPrimaryContainer = Color(0xFF3D0A00),

    secondary = Color(0xFFE05020),         // Red-orange from mid arcs
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBC8),
    onSecondaryContainer = Color(0xFF321200),

    tertiary = Color(0xFFF07030),          // Warm orange from icon center
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF2C1300),

    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A19),

    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A19),
    surfaceVariant = Color(0xFFF5DDD8),
    onSurfaceVariant = Color(0xFF534340),
)

/* --- Dark theme --- */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6040),           // Vivid red-orange, high chroma
    onPrimary = Color.White,
    primaryContainer = Color(0xFF7A1C08),
    onPrimaryContainer = Color(0xFFFFDAD3),

    secondary = Color(0xFFFF8050),         // Vivid orange
    onSecondary = Color(0xFF2A0D00),
    secondaryContainer = Color(0xFF5C2A00),
    onSecondaryContainer = Color(0xFFFFDBC8),

    tertiary = Color(0xFFFFA060),          // Bright amber
    onTertiary = Color(0xFF301800),
    tertiaryContainer = Color(0xFF4E2600),
    onTertiaryContainer = Color(0xFFFFDCBE),

    background = Color(0xFF120806),        // Near-black with warm tint
    onBackground = Color(0xFFF2E0DC),

    surface = Color(0xFF120806),
    onSurface = Color(0xFFF2E0DC),
    surfaceVariant = Color(0xFF3A2420),
    onSurfaceVariant = Color(0xFFD0B8B3),
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
        typography = AppTypography,
        content = content
    )
}
