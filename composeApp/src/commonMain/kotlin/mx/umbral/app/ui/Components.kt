package mx.umbral.app.ui

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Estado de una carga asíncrona. */
data class Carga<T>(val datos: T? = null, val error: String? = null, val cargando: Boolean = true)

@Composable
fun <T> cargar(vararg keys: Any?, bloque: suspend () -> T): State<Carga<T>> =
    produceState(Carga<T>(), *keys) {
        value = Carga(datos = value.datos, cargando = true)
        value = runCatching { bloque() }.fold(
            onSuccess = { Carga(datos = it, cargando = false) },
            onFailure = { Carga(error = it.message ?: "Error", cargando = false) },
        )
    }

@Composable
fun <T> Contenido(carga: Carga<T>, onReintentar: (() -> Unit)? = null, contenido: @Composable (T) -> Unit) {
    when {
        carga.datos != null -> contenido(carga.datos)
        carga.error != null -> Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(RojoSuave).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("No se pudo cargar", color = Rojo, style = MaterialTheme.typography.titleSmall)
            Text(carga.error, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            if (onReintentar != null) TextButton(onClick = onReintentar, contentPadding = PaddingValues(0.dp)) {
                Text("Reintentar", color = Rojo, fontWeight = FontWeight.SemiBold)
            }
        }
        else -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Tarjeta plana: borde de 1dp, sin sombra. */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    fondo: Color = MaterialTheme.colorScheme.surface,
    borde: Color = MaterialTheme.colorScheme.outlineVariant,
    padding: Dp = 16.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(fondo)
            .border(1.dp, borde, MaterialTheme.shapes.medium)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = contenido,
    )
}

/** Varias filas dentro de UNA tarjeta, separadas por divisores finos. `sangria`: margen izquierdo del divisor. */
@Composable
fun <T> ListaTarjeta(
    elementos: List<T>,
    modifier: Modifier = Modifier,
    sangria: Dp = 16.dp,
    fila: @Composable (T) -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
    ) {
        elementos.forEachIndexed { i, e ->
            if (i > 0) Divisor(Modifier.padding(start = sangria))
            fila(e)
        }
    }
}

/** Fila de una ListaTarjeta: padding 14/16 y al menos 48dp de alto. */
@Composable
fun FilaLista(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, contenido: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = contenido,
    )
}

@Composable
fun Divisor(modifier: Modifier = Modifier) = HorizontalDivider(modifier, thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

/** Chip de estado: radio 6, siempre con texto (no solo color). */
@Composable
fun Etiqueta(texto: String, color: Color = MaterialTheme.colorScheme.secondary, fondo: Color = MaterialTheme.colorScheme.surfaceVariant) {
    Text(
        texto,
        modifier = Modifier.clip(MaterialTheme.shapes.extraSmall).background(fondo).padding(horizontal = 8.dp, vertical = 3.dp),
        color = color, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1,
    )
}

@Composable fun EtiquetaOk(t: String) = Etiqueta(t, Verde, VerdeSuave)
@Composable fun EtiquetaMal(t: String) = Etiqueta(t, Rojo, RojoSuave)
@Composable fun EtiquetaAviso(t: String) = Etiqueta(t, Ambar, AmbarSuave)
@Composable fun EtiquetaAcento(t: String) = Etiqueta(t, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer)

/** Título de sección (15sp semibold). */
@Composable
fun Titulo(texto: String, modifier: Modifier = Modifier) =
    Text(texto, modifier, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

@Composable
fun Secundario(texto: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) =
    Text(texto, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = maxLines)

/** Encabezado de pantalla: línea de contexto arriba (opcional), título de 24sp y acciones a la derecha. */
@Composable
fun Encabezado(titulo: String, modifier: Modifier = Modifier, contexto: String? = null, acciones: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            if (!contexto.isNullOrBlank()) Secundario(contexto, Modifier.padding(bottom = 4.dp))
            Text(titulo, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        acciones()
    }
}

/** Encabezado de un bloque con acción de texto opcional a la derecha ("Ver todos"). */
@Composable
fun Seccion(titulo: String, accion: String? = null, onAccion: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Titulo(titulo, Modifier.weight(1f))
        if (accion != null && onAccion != null) {
            Text(
                accion,
                Modifier.clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClick = onAccion)
                    .minimumInteractiveComponentSize().padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun Vacio(texto: String) = Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Secundario(texto) }

/** Iniciales en círculo gris. */
@Composable
fun Avatar(nombre: String, size: Int = 36) {
    val ini = nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) { Text(ini, color = MaterialTheme.colorScheme.secondary, fontSize = (size * 0.36f).sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun Fila(modifier: Modifier = Modifier, contenido: @Composable RowScope.() -> Unit) =
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), content = contenido)

/** Punto de color para bitácoras; acompáñalo siempre de texto. */
@Composable
fun Punto(color: Color) = Box(Modifier.size(8.dp).clip(CircleShape).background(color))

@Composable
fun Cargando() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
}

/** Botón principal: oscuro por defecto (inverseSurface) o de acento. 52dp de alto. */
@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    acento: Boolean = false,
    enabled: Boolean = true,
    cargando: Boolean = false,
    alto: Dp = 52.dp,
    estilo: TextStyle = MaterialTheme.typography.labelLarge,
) {
    val fondo = if (acento) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inverseSurface
    val texto2 = if (acento) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.inverseOnSurface
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(alto),
        enabled = enabled && !cargando,
        shape = RoundedCornerShape(if (alto > 56.dp) 14.dp else 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = fondo, contentColor = texto2,
            disabledContainerColor = fondo.copy(alpha = 0.4f), disabledContentColor = texto2.copy(alpha = 0.8f),
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = texto2)
        else {
            if (icono != null) { Icon(icono, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(texto, style = estilo)
        }
    }
}

/** Botón secundario: superficie con borde de control. */
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.onSurface,
    alto: Dp = 52.dp,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(alto),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = color),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (icono != null) { Icon(icono, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(texto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
    }
}

/** Botón redondo de 44dp con borde, solo icono: `descripcion` es obligatoria para lectores de pantalla. */
@Composable
fun BotonIcono(icono: ImageVector, descripcion: String, onClick: () -> Unit, modifier: Modifier = Modifier, redondo: Boolean = true) {
    val forma = if (redondo) CircleShape else MaterialTheme.shapes.small
    Box(
        modifier
            .size(44.dp)
            .clip(forma)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, forma)
            .clickable(role = Role.Button, onClickLabel = descripcion, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icono, descripcion, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface) }
}

/** Opción seleccionable tipo píldora (secciones, días, horas). 36dp visibles, 48dp táctiles. */
@Composable
fun Opcion(texto: String, seleccionado: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    val forma = RoundedCornerShape(18.dp)
    val cs = MaterialTheme.colorScheme
    Box(Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .height(36.dp)
                .clip(forma)
                .background(if (seleccionado) cs.inverseSurface else cs.surface)
                .then(if (seleccionado) Modifier else Modifier.border(1.dp, cs.outline, forma))
                .semantics { selected = seleccionado }
                .clickable(enabled = enabled, role = Role.Tab, onClick = onClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                texto,
                color = when {
                    seleccionado -> cs.inverseOnSurface
                    !enabled -> Terciario
                    else -> cs.secondary
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

/** Campo de texto con etiqueta arriba (13sp), alto de 48dp y radio 10. */
@Composable
fun Campo(
    etiqueta: String,
    valor: String,
    onValor: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    estilo: TextStyle = MaterialTheme.typography.bodyLarge,
    accionEtiqueta: (@Composable () -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // La etiqueta visible no se lee aparte: el campo ya la anuncia como su nombre
            Text(etiqueta, Modifier.weight(1f).clearAndSetSemantics {}, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            accionEtiqueta?.invoke()
        }
        OutlinedTextField(
            valor, onValor, Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = etiqueta },
            singleLine = singleLine, minLines = minLines,
            textStyle = estilo,
            placeholder = placeholder?.let { { Text(it, style = estilo, color = Terciario) } },
            keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }
}

/** Mensaje de error en línea (el texto ya dice qué pasó; el color solo refuerza). */
@Composable
fun TextoError(texto: String, modifier: Modifier = Modifier) =
    Text(texto, modifier, color = Rojo, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
