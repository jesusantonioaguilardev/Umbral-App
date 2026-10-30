package mx.umbral.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Verde = Color(0xFF15803D)
val VerdeSuave = Color(0xFFE8F6EC)
val Rojo = Color(0xFFB91C1C)
val RojoSuave = Color(0xFFFDECEC)
val Ambar = Color(0xFFA16207)
val AmbarSuave = Color(0xFFFDF5E1)

private val Claro = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F4F2),
    onPrimaryContainer = Color(0xFF0B3B37),
    secondary = Color(0xFF475569),
    background = Color(0xFFF6F7F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F3F6),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE3E6EB),
    outlineVariant = Color(0xFFE3E6EB),
    error = Rojo,
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF04201D),
    primaryContainer = Color(0xFF12302D),
    onPrimaryContainer = Color(0xFFB8F0E8),
    background = Color(0xFF0D1113),
    surface = Color(0xFF151A1D),
    surfaceVariant = Color(0xFF1C2226),
    onSurfaceVariant = Color(0xFF98A2AB),
    outline = Color(0xFF283036),
    outlineVariant = Color(0xFF283036),
    error = Color(0xFFF87171),
)

@Composable
fun UmbralTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(20.dp),
        ),
        content = content,
    )
}
