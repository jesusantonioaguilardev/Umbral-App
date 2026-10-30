package mx.umbral.app.ui.caseta

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mx.umbral.app.data.Perfil
import mx.umbral.app.data.Repository
import mx.umbral.app.data.Validacion
import mx.umbral.app.ui.EtiquetaMal
import mx.umbral.app.ui.EtiquetaOk
import mx.umbral.app.ui.Fila
import mx.umbral.app.ui.Rojo
import mx.umbral.app.ui.RojoSuave
import mx.umbral.app.ui.Secundario
import mx.umbral.app.ui.Tarjeta
import mx.umbral.app.ui.Titulo
import mx.umbral.app.ui.Verde
import mx.umbral.app.ui.VerdeSuave
import mx.umbral.app.ui.cargar
import mx.umbral.app.util.Fechas

/** Modo caseta en el teléfono del guardia: validar códigos y ver la bitácora del día.
 *  Para escanear con cámara usa el panel web de caseta (tableta). */
@Composable
fun GuardiaScreen(repo: Repository, perfil: Perfil, salir: () -> Unit) {
    var codigo by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf<Validacion?>(null) }
    var refresco by remember { mutableIntStateOf(0) }
    val accesos by cargar(refresco) { repo.accesosHoy() }
    val scope = rememberCoroutineScope()

    fun validar() {
        val c = codigo.removePrefix("UMBRAL:").trim()
        if (c.isBlank()) return
        scope.launch {
            resultado = runCatching { repo.validarPase(c) }.getOrElse { Validacion(false, it.message) }
            codigo = ""; refresco++
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Fila {
                Column(Modifier.weight(1f)) {
                    Text("Caseta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Secundario("${perfil.nombre} · ${perfil.condominio?.nombre ?: ""}")
                }
                IconButton(onClick = { refresco++ }) { Icon(Icons.Filled.Refresh, "Actualizar") }
                IconButton(onClick = salir) { Icon(Icons.AutoMirrored.Filled.Logout, "Salir") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    codigo, { codigo = it.uppercase() }, Modifier.weight(1f), label = { Text("Código del pase") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { validar() }),
                )
                Button(onClick = ::validar, Modifier.height(56.dp)) { Text("Validar") }
            }
        }
        resultado?.let { r ->
            item {
                val ok = r.permitido
                Column(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                        .background(if (ok) VerdeSuave else RojoSuave)
                        .border(2.dp, if (ok) Verde else Rojo, MaterialTheme.shapes.large).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(if (ok) "✓ Acceso válido" else "✕ Acceso denegado", color = if (ok) Verde else Rojo, fontWeight = FontWeight.Bold)
                    Text(r.visitante ?: "—", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = androidx.compose.ui.graphics.Color(0xFF111827))
                    Text(listOfNotNull(r.tipo?.etiqueta, r.unidad, r.autorizo?.let { "Autorizó: $it" }).joinToString(" · "), color = androidx.compose.ui.graphics.Color(0xFF4B5563))
                    r.motivo?.let { Text(it, color = Rojo, fontWeight = FontWeight.Bold) }
                }
            }
        }
        item { Titulo("Bitácora de hoy") }
        items(accesos.datos.orEmpty(), key = { it.id }) { a ->
            Tarjeta {
                Fila {
                    Secundario(Fechas.hora(a.createdAt))
                    Column(Modifier.weight(1f)) {
                        Text(a.nombre, fontWeight = FontWeight.SemiBold)
                        Secundario(listOfNotNull(a.unidad?.nombre, a.motivo).joinToString(" · "))
                    }
                    if (a.permitido) EtiquetaOk(a.tipo.etiqueta) else EtiquetaMal("Denegado")
                }
            }
        }
    }
}
