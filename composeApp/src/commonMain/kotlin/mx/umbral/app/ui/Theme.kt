package mx.umbral.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Colores de estado (ok / bad / warn de css/styles.css), en claro y oscuro
@Immutable
class ColoresEstado(
    val verde: Color, val verdeSuave: Color,
    val rojo: Color, val rojoSuave: Color,
    val ambar: Color, val ambarSuave: Color,
)

private val EstadoClaro = ColoresEstado(
    verde = Color(0xFF15803D), verdeSuave = Color(0xFFE8F6EC),
    rojo = Color(0xFFB91C1C), rojoSuave = Color(0xFFFDECEC),
    ambar = Color(0xFFA16207), ambarSuave = Color(0xFFFDF5E1),
)

private val EstadoOscuro = ColoresEstado(
    verde = Color(0xFF4ADE80), verdeSuave = Color(0xFF12291A),
    rojo = Color(0xFFF87171), rojoSuave = Color(0xFF2F1515),
    ambar = Color(0xFFFACC15), ambarSuave = Color(0xFF2C2610),
)

val LocalColoresEstado = staticCompositionLocalOf { EstadoClaro }

val Verde: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.verde
val VerdeSuave: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.verdeSuave
val Rojo: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.rojo
val RojoSuave: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.rojoSuave
val Ambar: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.ambar
val AmbarSuave: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.ambarSuave

private val Claro = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F4F2),
    onPrimaryContainer = Color(0xFF0B3B37),
    secondary = Color(0xFF475569),
    background = Color(0xFFF6F7F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F3F6),
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE3E6EB),
    outlineVariant = Color(0xFFE3E6EB),
    error = Color(0xFFB91C1C),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF04201D),
    primaryContainer = Color(0xFF12302D),
    onPrimaryContainer = Color(0xFFB8F0E8),
    background = Color(0xFF0D1113),
    surface = Color(0xFF151A1D),
    surfaceVariant = Color(0xFF1C2226),
    onBackground = Color(0xFFE8ECEF),
    onSurface = Color(0xFFE8ECEF),
    onSurfaceVariant = Color(0xFF98A2AB),
    outline = Color(0xFF283036),
    outlineVariant = Color(0xFF283036),
    error = Color(0xFFF87171),
)

@Composable
fun UmbralTheme(content: @Composable () -> Unit) {
    val oscuro = isSystemInDarkTheme()
    CompositionLocalProvider(LocalColoresEstado provides if (oscuro) EstadoOscuro else EstadoClaro) {
        MaterialTheme(
            colorScheme = if (oscuro) Oscuro else Claro,
            shapes = Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(20.dp),
            ),
            content = content,
        )
    }
}
