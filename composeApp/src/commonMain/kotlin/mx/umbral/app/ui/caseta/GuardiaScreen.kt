package mx.umbral.app.ui.caseta

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.umbral.app.data.Perfil
import mx.umbral.app.data.Repository
import mx.umbral.app.data.Validacion
import mx.umbral.app.ui.BotonIcono
import mx.umbral.app.ui.BotonPrimario
import mx.umbral.app.ui.Campo
import mx.umbral.app.ui.Contenido
import mx.umbral.app.ui.Punto
import mx.umbral.app.ui.Rojo
import mx.umbral.app.ui.RojoBorde
import mx.umbral.app.ui.RojoSuave
import mx.umbral.app.ui.Secundario
import mx.umbral.app.ui.Tarjeta
import mx.umbral.app.ui.Titulo
import mx.umbral.app.ui.Verde
import mx.umbral.app.ui.VerdeBorde
import mx.umbral.app.ui.VerdeSuave
import mx.umbral.app.ui.cargar
import mx.umbral.app.ui.tnum
import mx.umbral.app.util.Fechas

/** Modo caseta en el teléfono del guardia: validar códigos y ver la bitácora del día.
 *  Para escanear con cámara usa el panel web de caseta (tableta). Un lector QR tipo teclado escribe en el campo. */
@Composable
fun GuardiaScreen(repo: Repository, perfil: Perfil, salir: () -> Unit) {
    var codigo by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf<Validacion?>(null) }
    var validando by remember { mutableStateOf(false) }
    var refresco by remember { mutableIntStateOf(0) }
    val accesos by cargar(refresco) { repo.accesosHoy() }
    val scope = rememberCoroutineScope()
    var reloj by remember { mutableStateOf(Fechas.hora(Fechas.ahora().toString())) }

    LaunchedEffect(Unit) {
        while (true) { reloj = Fechas.hora(Fechas.ahora().toString()); delay(15_000) }
    }

    fun validar() {
        val c = codigo.removePrefix("UMBRAL:").trim()
        if (c.isBlank() || validando) return
        validando = true
        scope.launch {
            resultado = runCatching { repo.validarPase(c) }.getOrElse { Validacion(false, it.message) }
            codigo = ""; refresco++; validando = false
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Secundario(listOfNotNull("Caseta", perfil.condominio?.nombre).joinToString(" · "), maxLines = 1)
                    Text(reloj, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.headlineSmall.tnum())
                    Secundario(perfil.nombre, maxLines = 1)
                }
                BotonIcono(Icons.Outlined.Refresh, "Actualizar bitácora", { refresco++ })
                BotonIcono(Icons.AutoMirrored.Outlined.Logout, "Cerrar sesión", salir)
            }
        }
        item {
            val r = resultado
            if (r == null) Tarjeta(padding = 24.dp) {
                Text("Listo para validar", style = MaterialTheme.typography.titleMedium)
                Secundario("Escribe el código del pase o léelo con el lector. El resultado aparece aquí.")
            } else Resultado(r)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Campo(
                    "Código del pase", codigo, { codigo = it.uppercase() },
                    placeholder = "Ej. PX7M2Q",
                    estilo = MaterialTheme.typography.titleLarge.tnum().copy(letterSpacing = 0.08.em),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { validar() }),
                )
                BotonPrimario(
                    "Validar código", ::validar, icono = Icons.Outlined.QrCode2, alto = 64.dp,
                    enabled = codigo.isNotBlank(), cargando = validando,
                    estilo = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp),
                )
            }
        }
        item { Titulo("Bitácora de hoy") }
        item {
            Contenido(accesos, onReintentar = { refresco++ }) { lista ->
                if (lista.isEmpty()) Secundario("Aún no hay accesos registrados hoy.")
            }
        }
        items(accesos.datos.orEmpty(), key = { it.id }) { a ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Punto(if (a.permitido) Verde else Rojo)
                Column(Modifier.weight(1f)) {
                    Text(
                        listOfNotNull(a.nombre, a.unidad?.nombre).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(a.tipo.etiqueta, if (a.permitido) null else "Denegado", a.motivo).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (a.permitido) MaterialTheme.colorScheme.onSurfaceVariant else Rojo,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(Fechas.hora(a.createdAt), style = MaterialTheme.typography.bodyMedium.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Resultado grande y legible a distancia; el texto dice el estado, el color solo lo refuerza. */
@Composable
private fun Resultado(r: Validacion) {
    val ok = r.permitido
    val color = if (ok) Verde else Rojo
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
            .background(if (ok) VerdeSuave else RojoSuave)
            .border(1.dp, if (ok) VerdeBorde else RojoBorde, MaterialTheme.shapes.extraLarge)
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                Icon(if (ok) Icons.Rounded.Check else Icons.Rounded.Close, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.surface)
            }
            Text(if (ok) "Acceso permitido" else "Acceso denegado", color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Column {
            Text(r.visitante ?: "—", fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
            val detalle = listOfNotNull(r.tipo?.etiqueta, r.unidad, r.autorizo?.let { "Autorizó $it" }).joinToString(" · ")
            if (detalle.isNotBlank()) Text(detalle, Modifier.padding(top = 6.dp), fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
        }
        r.motivo?.let { Text(it, color = Rojo, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Text(
            if (ok) "Levanta la pluma" else "No levantes la pluma",
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
