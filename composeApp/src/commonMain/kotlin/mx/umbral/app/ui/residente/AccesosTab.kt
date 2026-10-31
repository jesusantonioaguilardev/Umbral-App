package mx.umbral.app.ui.residente

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import mx.umbral.app.Config
import mx.umbral.app.data.Pase
import mx.umbral.app.data.Perfil
import mx.umbral.app.data.Repository
import mx.umbral.app.data.TipoAcceso
import mx.umbral.app.platform.rememberCompartir
import mx.umbral.app.ui.Contenido
import mx.umbral.app.ui.Etiqueta
import mx.umbral.app.ui.EtiquetaOk
import mx.umbral.app.ui.Fila
import mx.umbral.app.ui.Secundario
import mx.umbral.app.ui.Tarjeta
import mx.umbral.app.ui.Titulo
import mx.umbral.app.ui.Vacio
import mx.umbral.app.ui.cargar
import mx.umbral.app.util.Fechas
import mx.umbral.app.util.codificarUrl
import kotlin.time.Duration.Companion.hours

fun Pase.vigente(): Boolean = activo && (Fechas.parse(validoHasta)?.let { it > Fechas.ahora() } ?: false) &&
    (usosMax == null || usos < usosMax)

fun Pase.descripcionHorario(): String = if (modalidad == "recurrente") {
    val d = dias?.joinToString(", ") { Fechas.DIAS[it - 1] } ?: "Todos los días"
    d + (horaInicio?.let { " · ${it.take(5)} a ${horaFin?.take(5)}" } ?: "") + " · hasta ${Fechas.fecha(validoHasta)}"
} else "Una vez · ${Fechas.fechaHora(validoDesde)}"

/** Vigencia en palabras para el mensaje a la visita. */
fun Pase.vigenciaLegible(): String = if (modalidad == "recurrente") {
    val d = dias?.sorted()?.map { Fechas.DIAS[it - 1] }?.let { l ->
        if (l.size == 1) l[0] else l.dropLast(1).joinToString(", ") + " y " + l.last()
    } ?: "Todos los días"
    val horario = horaInicio?.let { ", de ${it.take(5)} a ${horaFin?.take(5) ?: "23:59"}" } ?: ""
    // valido_hasta es el inicio del día siguiente al último día válido
    val ultimo = Fechas.parse(validoHasta)?.toLocalDateTime(Fechas.zona)?.let {
        if (it.hour == 0 && it.minute == 0) it.date.minus(1, DateTimeUnit.DAY) else it.date
    }
    "$d$horario, hasta el ${ultimo?.let { Fechas.fecha(it.toString()) } ?: Fechas.fecha(validoHasta)}"
} else {
    val dia = Fechas.parse(validoDesde)?.toLocalDateTime(Fechas.zona)?.date
    val nombre = dia?.let { "${Fechas.DIAS[it.dayOfWeek.ordinal]} ${Fechas.fecha(it.toString())}" } ?: Fechas.fecha(validoDesde)
    "$nombre, desde las ${Fechas.hora(validoDesde)}"
}

/** Texto que se envía a la visita por WhatsApp o la hoja de compartir. */
fun Pase.mensajeParaVisita(condo: String, unidad: String?): String = buildString {
    append("Hola, $visitante. Te comparto tu pase de acceso a $condo")
    unidad?.let { append(" ($it)") }
    append(".\n\nVigencia: ${vigenciaLegible()}\n")
    append("Código: $codigo\n\n")
    append("Abre tu pase: ${Config.URL_PASE_WEB}$codigo\n")
    append("Al llegar, muestra el QR en caseta.")
}

@Composable
fun AccesosTab(repo: Repository, perfil: Perfil, abrirNuevo: Boolean, onNuevoCerrado: () -> Unit) {
    var refresco by remember { mutableIntStateOf(0) }
    val pases by cargar(refresco) { repo.misPases() }
    var nuevo by remember { mutableStateOf(abrirNuevo) }
    var detalle by remember { mutableStateOf<Pase?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Accesos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Secundario("Crea pases para visitas y proveedores. La visita no necesita instalar nada.")
            }
            item {
                Contenido(pases, onReintentar = { refresco++ }) { lista ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (lista.isEmpty()) Vacio("Aún no has creado pases")
                        lista.forEach { p ->
                            Tarjeta(Modifier.clickable { detalle = p }) {
                                Fila {
                                    Text(p.visitante, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    if (p.vigente()) EtiquetaOk("Vigente") else Etiqueta(if (!p.activo) "Cancelado" else "Expirado")
                                }
                                Secundario("${p.tipo.etiqueta} · ${p.descripcionHorario()}")
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { nuevo = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Nuevo acceso") },
        )
    }

    if (nuevo) NuevoPaseSheet(repo, onCerrar = { creado ->
        nuevo = false; onNuevoCerrado()
        if (creado != null) { refresco++; detalle = creado }
    })
    detalle?.let { p ->
        PaseDialog(p, perfil, onCerrar = { detalle = null }, onCancelar = if (p.vigente()) ({
            detalle = null
        }) else null, repo = repo, onCambio = { refresco++ })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NuevoPaseSheet(repo: Repository, onCerrar: (Pase?) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var visitante by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoAcceso.visita) }
    var recurrente by remember { mutableStateOf(false) }
    var diaOffset by remember { mutableIntStateOf(0) }
    var hora by remember { mutableStateOf("18:00") }
    val dias = remember { mutableStateListOf(1, 3, 5) }
    var horaInicio by remember { mutableStateOf("08:00") }
    var horaFin by remember { mutableStateOf("14:00") }
    var vigenciaDias by remember { mutableIntStateOf(30) }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = { onCerrar(null) }, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Nuevo acceso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(visitante, { visitante = it }, Modifier.fillMaxWidth(), label = { Text("Nombre de la visita") }, singleLine = true)

            Secundario("Tipo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TipoAcceso.visita, TipoAcceso.proveedor, TipoAcceso.paqueteria, TipoAcceso.airbnb).forEach { t ->
                    FilterChip(selected = tipo == t, onClick = { tipo = t; if (t == TipoAcceso.proveedor) recurrente = true }, label = { Text(t.etiqueta) })
                }
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(!recurrente, { recurrente = false }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Una vez") }
                SegmentedButton(recurrente, { recurrente = true }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Recurrente") }
            }

            if (!recurrente) {
                Secundario("Cuándo")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((0..6).toList()) { n ->
                        FilterChip(selected = diaOffset == n, onClick = { diaOffset = n }, label = { Text(Fechas.etiquetaDia(Fechas.diasDesdeHoy(n))) })
                    }
                }
                OutlinedTextField(hora, { hora = it }, Modifier.fillMaxWidth(), label = { Text("Hora aproximada (HH:mm)") }, singleLine = true)
                Secundario("El pase funciona desde una hora antes y hasta el final del día.")
            } else {
                Secundario("Días permitidos")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Fechas.DIAS.forEachIndexed { i, d ->
                        val n = i + 1
                        FilterChip(selected = n in dias, onClick = { if (n in dias) dias.remove(n) else dias.add(n) }, label = { Text(d) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(horaInicio, { horaInicio = it }, Modifier.weight(1f), label = { Text("Desde") }, singleLine = true)
                    OutlinedTextField(horaFin, { horaFin = it }, Modifier.weight(1f), label = { Text("Hasta") }, singleLine = true)
                }
                Secundario("Vigencia")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30, 90, 180).forEach { v -> FilterChip(selected = vigenciaDias == v, onClick = { vigenciaDias = v }, label = { Text("$v días") }) }
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = !guardando,
                onClick = {
                    if (visitante.isBlank()) { error = "Escribe el nombre de la visita"; return@Button }
                    if (recurrente && dias.isEmpty()) { error = "Elige al menos un día"; return@Button }
                    guardando = true; error = null
                    scope.launch {
                        runCatching {
                            if (recurrente) {
                                repo.crearPase(
                                    visitante.trim(), tipo, true, dias.sorted(),
                                    Fechas.normalizarHora(horaInicio), Fechas.normalizarHora(horaFin),
                                    Fechas.ahora().toString(),
                                    Fechas.inicioDelDia(Fechas.hoy().plus(vigenciaDias + 1, DateTimeUnit.DAY)).toString(),
                                )
                            } else {
                                val dia = Fechas.diasDesdeHoy(diaOffset)
                                val desde = Fechas.en(dia, hora) - 1.hours
                                val hasta = Fechas.inicioDelDia(dia.plus(1, DateTimeUnit.DAY))
                                repo.crearPase(visitante.trim(), tipo, false, null, null, null,
                                    desde.toString(), hasta.toString())
                            }
                        }.onSuccess { onCerrar(it) }.onFailure { error = it.message; guardando = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Crear pase", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun PaseDialog(p: Pase, perfil: Perfil, repo: Repository, onCerrar: () -> Unit, onCancelar: (() -> Unit)?, onCambio: () -> Unit) {
    val compartir = rememberCompartir()
    val uri = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    val mensaje = p.mensajeParaVisita(perfil.condominio?.nombre ?: "el condominio", perfil.unidad?.nombre)
    // wa.me abre WhatsApp (app o web); si no se puede, cae a la hoja nativa
    fun enviarWhatsApp() {
        runCatching { uri.openUri("https://wa.me/?text=${codificarUrl(mensaje)}") }.onFailure { compartir(mensaje) }
    }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(p.visitante) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(14.dp)) {
                    Image(rememberQrCodePainter("UMBRAL:${p.codigo}"), "Código QR", Modifier.size(210.dp))
                }
                Text(p.codigo, fontFamily = FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("${p.tipo.etiqueta} · ${p.descripcionHorario()}", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                if (!p.vigente()) Text("Este pase ya no es válido", color = MaterialTheme.colorScheme.error)
                if (p.vigente()) {
                    Button(onClick = ::enviarWhatsApp, Modifier.fillMaxWidth().height(52.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                        Text("  Enviar por WhatsApp", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(onClick = { compartir(mensaje) }, Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Text("  Compartir por otra app")
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
        dismissButton = {
            if (onCancelar != null) TextButton(onClick = {
                scope.launch {
                    runCatching { repo.cancelarPase(p.id) }
                        .onSuccess { onCambio(); onCancelar() }
                        .onFailure { error = it.message ?: "No se pudo cancelar el pase" }
                }
            }) { Text("Cancelar pase", color = MaterialTheme.colorScheme.error) }
        },
    )
}
