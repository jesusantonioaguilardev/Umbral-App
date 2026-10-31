package mx.umbral.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Colores de estado (ok / bad / warn de css/styles.css), en claro y oscuro.
// *Borde: contorno de los avisos grandes (resultado de caseta).
@Immutable
class ColoresEstado(
    val verde: Color, val verdeSuave: Color, val verdeBorde: Color,
    val rojo: Color, val rojoSuave: Color, val rojoBorde: Color,
    val ambar: Color, val ambarSuave: Color,
)

private val EstadoClaro = ColoresEstado(
    verde = Color(0xFF15803D), verdeSuave = Color(0xFFEEF7F0), verdeBorde = Color(0xFFCFE8D6),
    rojo = Color(0xFFB42318), rojoSuave = Color(0xFFFDECEA), rojoBorde = Color(0xFFF6CFCB),
    ambar = Color(0xFFB54708), ambarSuave = Color(0xFFFDF3E7),
)

private val EstadoOscuro = ColoresEstado(
    verde = Color(0xFF4ADE80), verdeSuave = Color(0xFF12291A), verdeBorde = Color(0xFF1F4A2E),
    rojo = Color(0xFFF87171), rojoSuave = Color(0xFF2F1515), rojoBorde = Color(0xFF5C2626),
    ambar = Color(0xFFFBBF24), ambarSuave = Color(0xFF2C2610),
)

// Colores de apoyo que Material no trae: texto terciario y la tarjeta destacada (código personal).
@Immutable
class ColoresApoyo(
    val terciario: Color,
    val destacado: Color, val sobreDestacado: Color, val sobreDestacadoSuave: Color, val bordeDestacado: Color,
)

private val ApoyoClaro = ColoresApoyo(
    terciario = Color(0xFF6B7280),
    destacado = Color(0xFF111418), sobreDestacado = Color(0xFFFFFFFF), sobreDestacadoSuave = Color(0xFFA7B0BA),
    bordeDestacado = Color(0xFF111418),
)

private val ApoyoOscuro = ColoresApoyo(
    terciario = Color(0xFF8A949E),
    destacado = Color(0xFF1C2226), sobreDestacado = Color(0xFFE8ECEF), sobreDestacadoSuave = Color(0xFF98A2AB),
    bordeDestacado = Color(0xFF262D33),
)

val LocalColoresEstado = staticCompositionLocalOf { EstadoClaro }
val LocalColoresApoyo = staticCompositionLocalOf { ApoyoClaro }

val Verde: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.verde
val VerdeSuave: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.verdeSuave
val VerdeBorde: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.verdeBorde
val Rojo: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.rojo
val RojoSuave: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.rojoSuave
val RojoBorde: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.rojoBorde
val Ambar: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.ambar
val AmbarSuave: Color @Composable @ReadOnlyComposable get() = LocalColoresEstado.current.ambarSuave

val Terciario: Color @Composable @ReadOnlyComposable get() = LocalColoresApoyo.current.terciario
val Destacado: Color @Composable @ReadOnlyComposable get() = LocalColoresApoyo.current.destacado
val SobreDestacado: Color @Composable @ReadOnlyComposable get() = LocalColoresApoyo.current.sobreDestacado
val SobreDestacadoSuave: Color @Composable @ReadOnlyComposable get() = LocalColoresApoyo.current.sobreDestacadoSuave
val BordeDestacado: Color @Composable @ReadOnlyComposable get() = LocalColoresApoyo.current.bordeDestacado

/** Fondo del QR: siempre blanco para que lo lea cualquier escáner, también en modo oscuro. */
val FondoQr = Color.White

// outline = borde de controles; outlineVariant = borde de tarjetas y divisores.
// secondary = texto de etiquetas (#3A414A); inverseSurface = botón primario oscuro.
private val Claro = lightColorScheme(
    primary = Color(0xFF0F5F59),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7F2F0),
    onPrimaryContainer = Color(0xFF0F5F59),
    secondary = Color(0xFF3A414A),
    onSecondary = Color.White,
    background = Color(0xFFFAFAF9),
    onBackground = Color(0xFF111418),
    surface = Color.White,
    onSurface = Color(0xFF111418),
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = Color(0xFF5B6470),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF1F3F5),
    outline = Color(0xFFE3E6EA),
    outlineVariant = Color(0xFFECEEF1),
    inverseSurface = Color(0xFF111418),
    inverseOnSurface = Color.White,
    error = Color(0xFFB42318),
    onError = Color.White,
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFFB42318),
    scrim = Color(0xFF111418),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF04201D),
    primaryContainer = Color(0xFF12302D),
    onPrimaryContainer = Color(0xFF2DD4BF),
    secondary = Color(0xFFC3CBD2),
    onSecondary = Color(0xFF0D1113),
    background = Color(0xFF0D1113),
    onBackground = Color(0xFFE8ECEF),
    surface = Color(0xFF151A1D),
    onSurface = Color(0xFFE8ECEF),
    surfaceVariant = Color(0xFF1C2226),
    onSurfaceVariant = Color(0xFF98A2AB),
    surfaceContainerLowest = Color(0xFF151A1D),
    surfaceContainerLow = Color(0xFF151A1D),
    surfaceContainer = Color(0xFF151A1D),
    surfaceContainerHigh = Color(0xFF151A1D),
    surfaceContainerHighest = Color(0xFF1C2226),
    outline = Color(0xFF323A41),
    outlineVariant = Color(0xFF262D33),
    inverseSurface = Color(0xFFE8ECEF),
    inverseOnSurface = Color(0xFF0D1113),
    error = Color(0xFFF87171),
    onError = Color(0xFF2A0707),
    errorContainer = Color(0xFF2F1515),
    onErrorContainer = Color(0xFFF87171),
    scrim = Color.Black,
)

// Fuente del sistema; títulos con letter spacing negativo como en los mockups.
private val Tipografia = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

/** Cifras tabulares (montos, horas, códigos) para que no "bailen" al cambiar. */
fun TextStyle.tnum(): TextStyle = merge(TextStyle(fontFeatureSettings = "tnum"))

@Composable
fun UmbralTheme(content: @Composable () -> Unit) {
    val oscuro = isSystemInDarkTheme()
    CompositionLocalProvider(
        LocalColoresEstado provides if (oscuro) EstadoOscuro else EstadoClaro,
        LocalColoresApoyo provides if (oscuro) ApoyoOscuro else ApoyoClaro,
    ) {
        MaterialTheme(
            colorScheme = if (oscuro) Oscuro else Claro,
            typography = Tipografia,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(6.dp),   // chips de estado
                small = RoundedCornerShape(10.dp),       // campos y botones pequeños
                medium = RoundedCornerShape(14.dp),      // tarjetas
                large = RoundedCornerShape(16.dp),       // tarjetas destacadas
                extraLarge = RoundedCornerShape(18.dp),  // pase y resultado de caseta
            ),
            content = content,
        )
    }
}
