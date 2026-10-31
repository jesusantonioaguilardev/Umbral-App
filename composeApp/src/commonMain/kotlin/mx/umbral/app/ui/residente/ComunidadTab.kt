package mx.umbral.app.ui.residente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.umbral.app.data.Area
import mx.umbral.app.data.CATEGORIAS_TICKET
import mx.umbral.app.data.Repository
import mx.umbral.app.ui.Contenido
import mx.umbral.app.ui.Etiqueta
import mx.umbral.app.ui.EtiquetaAviso
import mx.umbral.app.ui.EtiquetaMal
import mx.umbral.app.ui.EtiquetaOk
import mx.umbral.app.ui.Fila
import mx.umbral.app.ui.Secundario
import mx.umbral.app.ui.Tarjeta
import mx.umbral.app.ui.Titulo
import mx.umbral.app.ui.Vacio
import mx.umbral.app.ui.cargar
import mx.umbral.app.util.Fechas

private val SECCIONES = listOf("Comunicados", "Paquetes", "Mis reportes", "Reservar", "Encuestas")
const val SECCION_COMUNICADOS = 0
const val SECCION_REPORTES = 2

@Composable
fun ComunidadTab(repo: Repository, seccionInicial: Int = SECCION_COMUNICADOS) {
    var sel by remember(seccionInicial) { mutableIntStateOf(seccionInicial) }
    Column(Modifier.fillMaxSize()) {
        Text("Comunidad", Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        ScrollableTabRow(selectedTabIndex = sel, edgePadding = 12.dp) {
            SECCIONES.forEachIndexed { i, s -> Tab(selected = sel == i, onClick = { sel = i }, text = { Text(s) }) }
        }
        when (sel) {
            0 -> Avisos(repo)
            1 -> Paquetes(repo)
            2 -> Tickets(repo)
            3 -> Reservar(repo)
            else -> Encuestas(repo)
        }
    }
}

@Composable
private fun Lista(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) =
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)

@Composable
private fun Avisos(repo: Repository) {
    val avisos by cargar { repo.avisos() }
    val uri = LocalUriHandler.current
    Lista {
        item { Contenido(avisos) { if (it.isEmpty()) Vacio("Sin comunicados de la administración") } }
        items(avisos.datos.orEmpty()) { a ->
            Tarjeta {
                Fila {
                    Text(a.titulo, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (a.importante) EtiquetaMal("Importante")
                    if (a.torre != null) Etiqueta("Torre ${a.torre}")
                }
                a.cuerpo?.let { Text(it) }
                a.adjuntoUrl?.let { url ->
                    OutlinedButton(onClick = { runCatching { uri.openUri(url) } }) { Text("📎 ${a.adjuntoNombre ?: "Ver adjunto"}") }
                }
                Secundario(Fechas.fechaHora(a.createdAt))
            }
        }
    }
}

@Composable
private fun Paquetes(repo: Repository) {
    val paquetes by cargar { repo.paquetes() }
    Lista {
        item { Contenido(paquetes) { if (it.isEmpty()) Vacio("No tienes paquetes registrados") } }
        items(paquetes.datos.orEmpty()) { p ->
            Tarjeta {
                Fila {
                    Text(p.paqueteria ?: "Paquete", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (p.entregadoAt == null) EtiquetaAviso("En recepción") else EtiquetaOk("Entregado")
                }
                Secundario("${p.descripcion ?: ""} · llegó ${Fechas.fechaHora(p.createdAt)}")
                if (p.entregadoAt == null) Secundario("Puedes recogerlo en caseta / recepción.")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tickets(repo: Repository) {
    var refresco by remember { mutableIntStateOf(0) }
    val tickets by cargar(refresco) { repo.tickets() }
    var nuevo by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    Lista {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { nuevo = true; aviso = null }, Modifier.fillMaxWidth()) { Text("Reportar un problema") }
                Secundario("Fugas, luminarias, seguridad, limpieza, ruido… La administración te responde aquí mismo.")
                aviso?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
            }
        }
        item { Contenido(tickets) { if (it.isEmpty()) Vacio("Aún no has hecho reportes") } }
        items(tickets.datos.orEmpty()) { t ->
            Tarjeta {
                Fila {
                    Text(t.titulo, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    when (t.estado) { "cerrado" -> EtiquetaOk("Resuelto"); "en_proceso" -> EtiquetaAviso("En proceso"); else -> Etiqueta("Recibido") }
                }
                Etiqueta(t.categoria)
                t.descripcion?.takeIf { it.isNotBlank() }?.let { Text(it) }
                t.respuesta?.takeIf { it.isNotBlank() }?.let {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text("Respuesta de administración", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(it)
                    }
                }
                Secundario("Reportado ${Fechas.fecha(t.createdAt)}${t.responsable?.let { " · Atiende: $it" } ?: ""}")
            }
        }
    }
    if (nuevo) {
        val scope = rememberCoroutineScope()
        var titulo by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var categoria by remember { mutableStateOf(CATEGORIAS_TICKET.first()) }
        var enviando by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!enviando) nuevo = false },
            title = { Text("Reportar un problema") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Secundario("Categoría")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CATEGORIAS_TICKET.forEach { c -> FilterChip(selected = categoria == c, onClick = { categoria = c }, label = { Text(c) }) }
                    }
                    OutlinedTextField(titulo, { titulo = it }, label = { Text("¿Qué pasa?") }, singleLine = true, placeholder = { Text("Fuga en el jardín, lámpara fundida…") })
                    OutlinedTextField(desc, { desc = it }, label = { Text("Detalles y ubicación") }, minLines = 3)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(enabled = titulo.isNotBlank() && !enviando, onClick = {
                    enviando = true; error = null
                    scope.launch {
                        runCatching { repo.crearTicket(titulo.trim(), desc.trim(), categoria) }
                            .onSuccess { nuevo = false; aviso = "✓ Reporte enviado. Te avisaremos aquí cuando lo atiendan."; refresco++ }
                            .onFailure { error = "No se pudo enviar: ${it.message}" }
                        enviando = false
                    }
                }) { Text(if (enviando) "Enviando…" else "Enviar reporte") }
            },
            dismissButton = { TextButton(onClick = { nuevo = false }, enabled = !enviando) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Reservar(repo: Repository) {
    var refresco by remember { mutableIntStateOf(0) }
    val areas by cargar { repo.areas() }
    val reservas by cargar(refresco) { repo.reservaciones() }
    var area by remember { mutableStateOf<Area?>(null) }
    var dia by remember { mutableIntStateOf(1) }
    var hora by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Lista {
        item { Titulo("Elige un área") }
        item {
            Contenido(areas) { lista ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lista.forEach { a -> FilterChip(selected = area?.id == a.id, onClick = { area = a; hora = null }, label = { Text(a.nombre) }) }
                }
            }
        }
        area?.let { a ->
            item { Secundario(listOfNotNull(a.reglas, "Horario ${a.abre.take(5)}–${a.cierra.take(5)}").joinToString(" · ")) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((0..13).toList()) { n -> FilterChip(selected = dia == n, onClick = { dia = n; hora = null }, label = { Text(Fechas.etiquetaDia(Fechas.diasDesdeHoy(n))) }) }
                }
            }
            item {
                val fecha = Fechas.diasDesdeHoy(dia).toString()
                val ocupadas = reservas.datos.orEmpty().filter { it.areaId == a.id && it.fecha == fecha }.map { it.horaInicio.take(5) }
                val ini = a.abre.take(2).toIntOrNull() ?: 8
                val fin = a.cierra.take(2).toIntOrNull() ?: 22
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (ini until fin).map { "${it.toString().padStart(2, '0')}:00" }.forEach { h ->
                        val ocupada = h in ocupadas
                        FilterChip(selected = hora == h, enabled = !ocupada, onClick = { hora = h }, label = { Text(if (ocupada) "$h ✕" else h) })
                    }
                }
            }
            item {
                Button(enabled = hora != null, modifier = Modifier.fillMaxWidth(), onClick = {
                    val h = hora!!; val finH = "${(h.take(2).toInt() + 1).toString().padStart(2, '0')}:00"
                    scope.launch {
                        runCatching { repo.reservar(a.id, Fechas.diasDesdeHoy(dia).toString(), h, finH) }
                            .onSuccess { msg = "Reservado: ${a.nombre}, ${Fechas.etiquetaDia(Fechas.diasDesdeHoy(dia))} $h"; hora = null; refresco++ }
                            .onFailure { msg = it.message }
                    }
                }) { Text("Reservar ${hora ?: ""}") }
            }
        }
        msg?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        item { Titulo("Próximas reservaciones") }
        val nombres = areas.datos.orEmpty().associate { it.id to it.nombre }
        items(reservas.datos.orEmpty()) { r ->
            Tarjeta { Fila { Text(nombres[r.areaId] ?: "Área", Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Secundario("${Fechas.fecha(r.fecha)} · ${r.horaInicio.take(5)}–${r.horaFin.take(5)}") } }
        }
    }
}

@Composable
private fun Encuestas(repo: Repository) {
    var refresco by remember { mutableIntStateOf(0) }
    val encuestas by cargar(refresco) { repo.encuestas() }
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    Lista {
        item { Contenido(encuestas) { if (it.isEmpty()) Vacio("Sin encuestas activas") } }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        items(encuestas.datos.orEmpty()) { e ->
            Tarjeta {
                Text(e.encuesta.pregunta, fontWeight = FontWeight.SemiBold)
                e.encuesta.opciones.forEachIndexed { i, op ->
                    val pct = if (e.total > 0) e.votos[i].toFloat() / e.total else 0f
                    if (e.miVoto == null && !e.cerrada) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                runCatching { repo.votar(e.encuesta.id, i) }
                                    .onSuccess { error = null }
                                    .onFailure { error = "No se pudo registrar tu voto: ${it.message}" }
                                refresco++
                            }
                        }, Modifier.fillMaxWidth()) { Text(op) }
                    } else {
                        Fila { Text(op + if (e.miVoto == i) "  ✓" else "", Modifier.weight(1f)); Text("${(pct * 100).toInt()}%", fontWeight = FontWeight.Bold) }
                        LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())
                    }
                }
                Secundario("${e.total} votos" + (e.encuesta.cierra?.let { (if (e.cerrada) " · cerró " else " · cierra ") + Fechas.fecha(it) } ?: ""))
            }
        }
    }
}
