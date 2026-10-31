package mx.umbral.app.ui.residente

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.launch
import mx.umbral.app.data.Area
import mx.umbral.app.data.Aviso
import mx.umbral.app.data.CATEGORIAS_TICKET
import mx.umbral.app.data.Repository
import mx.umbral.app.ui.Ambar
import mx.umbral.app.ui.AmbarSuave
import mx.umbral.app.ui.BotonPrimario
import mx.umbral.app.ui.BotonSecundario
import mx.umbral.app.ui.Campo
import mx.umbral.app.ui.Contenido
import mx.umbral.app.ui.Encabezado
import mx.umbral.app.ui.Etiqueta
import mx.umbral.app.ui.EtiquetaAviso
import mx.umbral.app.ui.EtiquetaOk
import mx.umbral.app.ui.FilaLista
import mx.umbral.app.ui.ListaTarjeta
import mx.umbral.app.ui.Opcion
import mx.umbral.app.ui.Secundario
import mx.umbral.app.ui.Tarjeta
import mx.umbral.app.ui.TextoError
import mx.umbral.app.ui.Titulo
import mx.umbral.app.ui.Vacio
import mx.umbral.app.ui.Verde
import mx.umbral.app.ui.cargar
import mx.umbral.app.ui.tnum
import mx.umbral.app.util.Fechas

private val SECCIONES = listOf("Avisos", "Paquetes", "Reportes", "Reservar", "Encuestas")
const val SECCION_COMUNICADOS = 0
const val SECCION_PAQUETES = 1
const val SECCION_REPORTES = 2

@Composable
fun ComunidadTab(repo: Repository, seccionInicial: Int = SECCION_COMUNICADOS) {
    var sel by remember(seccionInicial) { mutableIntStateOf(seccionInicial) }
    // Se cargan aquí para mostrar en el chip cuántos paquetes esperan en caseta
    val paquetes by cargar { repo.paquetes() }
    val enCaseta = paquetes.datos.orEmpty().count { it.entregadoAt == null }
    Column(Modifier.fillMaxSize()) {
        Encabezado("Comunidad", Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(SECCIONES) { i, s ->
                Opcion(if (i == SECCION_PAQUETES && enCaseta > 0) "$s · $enCaseta" else s, sel == i, { sel = i })
            }
        }
        when (sel) {
            0 -> Avisos(repo)
            1 -> Paquetes(paquetes)
            2 -> Tickets(repo)
            3 -> Reservar(repo)
            else -> Encuestas(repo)
        }
    }
}

@Composable
private fun Lista(content: LazyListScope.() -> Unit) =
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )

@Composable
private fun Avisos(repo: Repository) {
    val avisos by cargar { repo.avisos() }
    val lista = avisos.datos.orEmpty()
    val importantes = lista.filter { it.importante }
    val normales = lista.filterNot { it.importante }
    Lista {
        item { Contenido(avisos) { if (it.isEmpty()) Vacio("Sin comunicados de la administración") } }
        // Los importantes van solos, con icono de aviso; el resto, en una sola tarjeta con divisores.
        items(importantes, key = { it.id }) { a ->
            Tarjeta {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(AmbarSuave),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.WarningAmber, null, Modifier.size(18.dp), tint = Ambar) }
                    Column(Modifier.weight(1f)) {
                        CuerpoAviso(a)
                        Text("Importante", Modifier.padding(top = 8.dp), color = Ambar, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        if (normales.isNotEmpty()) item {
            ListaTarjeta(normales, sangria = 0.dp) { a -> Column(Modifier.fillMaxWidth().padding(16.dp)) { CuerpoAviso(a) } }
        }
    }
}

@Composable
private fun CuerpoAviso(a: Aviso) {
    val uri = LocalUriHandler.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(a.titulo, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Secundario(Fechas.corta(a.createdAt))
    }
    a.torre?.let { Box(Modifier.padding(top = 6.dp)) { Etiqueta("Torre $it") } }
    a.cuerpo?.let {
        Text(it, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
    }
    a.adjuntoUrl?.let { url ->
        Row(
            Modifier.padding(top = 4.dp).clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button) { runCatching { uri.openUri(url) } }
                .heightIn(min = 44.dp).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Description, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(a.adjuntoNombre ?: "Ver adjunto", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Paquetes(paquetes: mx.umbral.app.ui.Carga<List<mx.umbral.app.data.Paquete>>) {
    Lista {
        item {
            Contenido(paquetes) { lista ->
                if (lista.isEmpty()) Vacio("No tienes paquetes registrados")
                else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (lista.any { it.entregadoAt == null }) Secundario("Puedes recoger tus paquetes en caseta / recepción.")
                    ListaTarjeta(lista) { p ->
                        FilaLista {
                            Column(Modifier.weight(1f)) {
                                Text(p.paqueteria ?: "Paquete", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Secundario(
                                    listOfNotNull(p.descripcion?.takeIf { it.isNotBlank() }, "llegó ${Fechas.cuando(p.createdAt)}").joinToString(" · "),
                                    Modifier.padding(top = 2.dp),
                                )
                            }
                            if (p.entregadoAt == null) EtiquetaAviso("En recepción") else EtiquetaOk("Entregado")
                        }
                    }
                }
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonPrimario("Reportar un problema", { nuevo = true; aviso = null })
                Secundario("Fugas, luminarias, seguridad, limpieza, ruido… La administración te responde aquí mismo.")
                aviso?.let { Text(it, color = Verde, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
            }
        }
        item {
            Contenido(tickets, onReintentar = { refresco++ }) { lista ->
                if (lista.isEmpty()) Vacio("Aún no has hecho reportes")
                else ListaTarjeta(lista) { t ->
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(t.titulo, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            when (t.estado) { "cerrado" -> EtiquetaOk("Resuelto"); "en_proceso" -> EtiquetaAviso("En proceso"); else -> Etiqueta("Recibido") }
                        }
                        Secundario(t.categoria)
                        t.descripcion?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary) }
                        t.respuesta?.takeIf { it.isNotBlank() }?.let {
                            Column(
                                Modifier.fillMaxWidth().padding(top = 4.dp).clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text("Respuesta de administración", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Text(it, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Secundario("Reportado ${Fechas.fecha(t.createdAt)}${t.responsable?.let { " · Atiende: $it" } ?: ""}")
                    }
                }
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
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Reportar un problema", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("Categoría", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CATEGORIAS_TICKET.forEach { c -> Opcion(c, categoria == c, { categoria = c }) }
                        }
                    }
                    Campo("¿Qué pasa?", titulo, { titulo = it }, placeholder = "Fuga en el jardín, lámpara fundida…")
                    Campo("Detalles y ubicación", desc, { desc = it }, singleLine = false, minLines = 3)
                    error?.let { TextoError(it) }
                }
            },
            confirmButton = {
                TextButton(enabled = titulo.isNotBlank() && !enviando, onClick = {
                    enviando = true; error = null
                    scope.launch {
                        runCatching { repo.crearTicket(titulo.trim(), desc.trim(), categoria) }
                            .onSuccess { nuevo = false; aviso = "✓ Reporte enviado. Te avisaremos aquí cuando lo atiendan."; refresco++ }
                            .onFailure { error = "No se pudo enviar: ${it.message}" }
                        enviando = false
                    }
                }) { Text(if (enviando) "Enviando…" else "Enviar reporte", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { nuevo = false }, enabled = !enviando) { Text("Cancelar", color = MaterialTheme.colorScheme.secondary) }
            },
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
    var msgError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Lista {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Titulo("Elige un área")
                Contenido(areas) { lista ->
                    if (lista.isEmpty()) Secundario("La administración aún no registra áreas comunes.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        lista.forEach { a -> Opcion(a.nombre, area?.id == a.id, { area = a; hora = null }) }
                    }
                }
            }
        }
        area?.let { a ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Secundario(listOfNotNull(a.reglas, "Horario ${a.abre.take(5)}–${a.cierra.take(5)}").joinToString(" · "))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items((0..13).toList()) { n -> Opcion(Fechas.etiquetaDia(Fechas.diasDesdeHoy(n)), dia == n, { dia = n; hora = null }) }
                    }
                }
            }
            item {
                val fecha = Fechas.diasDesdeHoy(dia).toString()
                val ocupadas = reservas.datos.orEmpty().filter { it.areaId == a.id && it.fecha == fecha }.map { it.horaInicio.take(5) }
                val ini = a.abre.take(2).toIntOrNull() ?: 8
                val fin = a.cierra.take(2).toIntOrNull() ?: 22
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Hora", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (ini until fin).map { "${it.toString().padStart(2, '0')}:00" }.forEach { h ->
                            val ocupada = h in ocupadas
                            Opcion(if (ocupada) "$h · ocupado" else h, hora == h, { hora = h }, enabled = !ocupada)
                        }
                    }
                }
            }
            item {
                BotonPrimario("Reservar ${hora ?: ""}".trim(), enabled = hora != null, onClick = {
                    val h = hora!!; val finH = "${(h.take(2).toInt() + 1).toString().padStart(2, '0')}:00"
                    scope.launch {
                        runCatching { repo.reservar(a.id, Fechas.diasDesdeHoy(dia).toString(), h, finH) }
                            .onSuccess { msg = "✓ Reservado: ${a.nombre}, ${Fechas.etiquetaDia(Fechas.diasDesdeHoy(dia))} $h"; msgError = false; hora = null; refresco++ }
                            .onFailure { msg = it.message; msgError = true }
                    }
                })
            }
        }
        msg?.let {
            item {
                if (msgError) TextoError(it)
                else Text(it, color = Verde, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Titulo("Próximas reservaciones")
                val nombres = areas.datos.orEmpty().associate { it.id to it.nombre }
                Contenido(reservas, onReintentar = { refresco++ }) { lista ->
                    if (lista.isEmpty()) Secundario("No tienes reservaciones próximas.")
                    else ListaTarjeta(lista) { r ->
                        FilaLista {
                            Text(nombres[r.areaId] ?: "Área", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                "${Fechas.fecha(r.fecha)} · ${r.horaInicio.take(5)}–${r.horaFin.take(5)}",
                                style = MaterialTheme.typography.bodySmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
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
        item { Contenido(encuestas, onReintentar = { refresco++ }) { if (it.isEmpty()) Vacio("Sin encuestas activas") } }
        error?.let { item { TextoError(it) } }
        items(encuestas.datos.orEmpty(), key = { it.encuesta.id }) { e ->
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(e.encuesta.pregunta, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (e.cerrada) Etiqueta("Cerrada")
                }
                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    e.encuesta.opciones.forEachIndexed { i, op ->
                        val pct = if (e.total > 0) e.votos[i].toFloat() / e.total else 0f
                        if (e.miVoto == null && !e.cerrada) {
                            BotonSecundario(op, alto = 48.dp, onClick = {
                                scope.launch {
                                    runCatching { repo.votar(e.encuesta.id, i) }
                                        .onSuccess { error = null }
                                        .onFailure { error = "No se pudo registrar tu voto: ${it.message}" }
                                    refresco++
                                }
                            })
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row {
                                    Text(op + if (e.miVoto == i) "  ✓ Tu voto" else "", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (e.miVoto == i) FontWeight.SemiBold else FontWeight.Normal)
                                    Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.tnum(), fontWeight = FontWeight.SemiBold)
                                }
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    gapSize = 0.dp,
                                    drawStopIndicator = {},
                                )
                            }
                        }
                    }
                }
                Secundario("${e.total} votos" + (e.encuesta.cierra?.let { (if (e.cerrada) " · cerró " else " · cierra ") + Fechas.fecha(it) } ?: ""))
            }
        }
    }
}
