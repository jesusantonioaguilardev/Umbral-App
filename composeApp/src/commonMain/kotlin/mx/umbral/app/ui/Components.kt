package mx.umbral.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
        carga.error != null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(carga.error, color = MaterialTheme.colorScheme.error)
            if (onReintentar != null) TextButton(onClick = onReintentar) { Text("Reintentar") }
        }
        else -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
fun Tarjeta(modifier: Modifier = Modifier, fondo: Color = MaterialTheme.colorScheme.surface, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(fondo)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = contenido,
    )
}

@Composable
fun Etiqueta(texto: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, fondo: Color = MaterialTheme.colorScheme.surfaceVariant) {
    Text(
        texto,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(fondo).padding(horizontal = 10.dp, vertical = 3.dp),
        color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
    )
}

@Composable fun EtiquetaOk(t: String) = Etiqueta(t, Verde, VerdeSuave)
@Composable fun EtiquetaMal(t: String) = Etiqueta(t, Rojo, RojoSuave)
@Composable fun EtiquetaAviso(t: String) = Etiqueta(t, Ambar, AmbarSuave)

@Composable
fun Titulo(texto: String, modifier: Modifier = Modifier) =
    Text(texto, modifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

@Composable
fun Secundario(texto: String, modifier: Modifier = Modifier) =
    Text(texto, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun Vacio(texto: String) = Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Secundario(texto) }

@Composable
fun Avatar(nombre: String, size: Int = 40) {
    val ini = nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) { Text(ini, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
}

@Composable
fun Fila(modifier: Modifier = Modifier, contenido: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) =
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), content = contenido)

@Composable
fun Cargando() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
