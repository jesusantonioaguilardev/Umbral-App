package mx.umbral.app.ui.residente

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.umbral.app.data.Repository
import mx.umbral.app.ui.BotonPrimario
import mx.umbral.app.ui.BotonSecundario
import mx.umbral.app.ui.Campo
import mx.umbral.app.ui.Rojo
import mx.umbral.app.ui.RojoSuave
import mx.umbral.app.ui.Secundario
import mx.umbral.app.ui.TextoError
import mx.umbral.app.ui.Verde

/** Tiempo que hay que mantener presionado el botón para evitar alertas por toques accidentales. */
private const val MANTENER_MS = 2_000

/**
 * Botón "Emergencia" del inicio del residente: hay que mantenerlo presionado 2 s (se llena de rojo como barra de progreso).
 * `onAviso` recibe el texto de ayuda que conviene mostrar ("Sigue presionando…") o null para ocultarlo.
 * Con lector de pantalla, la acción de doble toque abre la hoja directamente (la hoja ya es la confirmación).
 */
@Composable
fun BotonEmergencia(onActivar: () -> Unit, onAviso: (String?) -> Unit, modifier: Modifier = Modifier) {
    val progreso = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current
    val activar by rememberUpdatedState(onActivar)
    val avisar by rememberUpdatedState(onAviso)
    val forma = RoundedCornerShape(22.dp)
    val rojo = Rojo
    val relleno = RojoSuave

    Box(
        modifier
            .height(44.dp)
            .clip(forma)
            .background(MaterialTheme.colorScheme.surface)
            // Relleno y barra inferior que avanzan mientras se mantiene presionado
            .drawBehind {
                val ancho = size.width * progreso.value
                if (ancho > 0f) {
                    drawRect(relleno, size = Size(ancho, size.height))
                    drawRect(rojo, topLeft = Offset(0f, size.height - 3.dp.toPx()), size = Size(ancho, 3.dp.toPx()))
                }
            }
            .border(1.dp, rojo, forma)
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = "Emergencia. Mantén presionado 2 segundos para avisar a caseta"
                onClick(label = "Enviar alerta de emergencia") { activar(); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    avisar("Sigue presionando…")
                    var completo = false
                    val carga = scope.launch {
                        progreso.animateTo(1f, tween(MANTENER_MS, easing = LinearEasing))
                        completo = true
                        haptico.performHapticFeedback(HapticFeedbackType.LongPress)
                        avisar(null)
                        activar()
                        progreso.snapTo(0f)
                    }
                    val arriba = waitForUpOrCancellation()
                    if (!completo) {
                        carga.cancel()
                        avisar(if (arriba != null) "Mantén presionado 2 segundos para pedir ayuda" else null)
                        scope.launch { progreso.animateTo(0f, tween(200)) }
                    }
                }
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ReportProblem, null, Modifier.size(18.dp), tint = rojo)
            Spacer(Modifier.width(6.dp))
            Text("Emergencia", color = rojo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Hoja para enviar la alerta: mensaje editable, envío y confirmación. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertaSheet(repo: Repository, onCerrar: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var mensaje by remember { mutableStateOf("Necesito ayuda en mi domicilio") }
    var enviando by remember { mutableStateOf(false) }
    var enviada by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun enviar() {
        if (mensaje.isBlank() || enviando) return
        enviando = true; error = null
        scope.launch {
            runCatching { repo.enviarAlerta(mensaje) }
                .onSuccess { enviada = true }
                .onFailure { error = "No se pudo enviar la alerta: ${it.message ?: "error de red"}. Intenta de nuevo o llama a caseta." }
            enviando = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!enviando) onCerrar() }, sheetState = sheet,
        containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (enviada) {
                Row(
                    Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Assertive },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Verde), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.surface)
                    }
                    Text("Alerta enviada a todo el condominio", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                }
                Secundario("Si la situación es grave, llama también al 911.")
                BotonPrimario("Listo", onCerrar)
            } else {
                Text("Alerta de emergencia", style = MaterialTheme.typography.headlineSmall)
                Secundario("Caseta, administración y tus vecinos recibirán este mensaje de inmediato junto con tu unidad.")
                Campo(
                    "Mensaje", mensaje, { mensaje = it; error = null },
                    singleLine = false, minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                error?.let { TextoError(it, Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
                Button(
                    onClick = ::enviar,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = mensaje.isNotBlank() && !enviando,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Rojo, contentColor = MaterialTheme.colorScheme.onError,
                        disabledContainerColor = Rojo.copy(alpha = 0.4f), disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.8f),
                    ),
                    elevation = null,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    if (enviando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onError)
                    else Text("Enviar alerta", style = MaterialTheme.typography.labelLarge)
                }
                BotonSecundario("Cancelar", onCerrar, enabled = !enviando)
            }
        }
    }
}
