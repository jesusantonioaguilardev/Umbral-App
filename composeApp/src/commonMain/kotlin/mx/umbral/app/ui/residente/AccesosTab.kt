package mx.umbral.app.ui.residente

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import mx.umbral.app.Config
import mx.umbral.app.data.CodigoPersonal
import mx.umbral.app.data.Pase
import mx.umbral.app.data.Perfil
import mx.umbral.app.data.Repository
import mx.umbral.app.data.TipoAcceso
import mx.umbral.app.platform.rememberCompartir
import mx.umbral.app.ui.Avatar
import mx.umbral.app.ui.BordeDestacado
import mx.umbral.app.ui.BotonIcono
import mx.umbral.app.ui.BotonPrimario
import mx.umbral.app.ui.BotonSecundario
import mx.umbral.app.ui.Campo
import mx.umbral.app.ui.Contenido
import mx.umbral.app.ui.Destacado
import mx.umbral.app.ui.Divisor
import mx.umbral.app.ui.Encabezado
import mx.umbral.app.ui.Etiqueta
import mx.umbral.app.ui.EtiquetaAcento
import mx.umbral.app.ui.EtiquetaAviso
import mx.umbral.app.ui.EtiquetaMal
import mx.umbral.app.ui.FilaLista
import mx.umbral.app.ui.FondoQr
import mx.umbral.app.ui.ListaTarjeta
import mx.umbral.app.ui.Opcion
import mx.umbral.app.ui.Rojo
import mx.umbral.app.ui.RojoSuave
import mx.umbral.app.ui.Secundario
import mx.umbral.app.ui.Seccion
import mx.umbral.app.ui.SobreDestacado
import mx.umbral.app.ui.SobreDestacadoSuave
import mx.umbral.app.ui.TextoError
import mx.umbral.app.ui.Vacio
import mx.umbral.app.ui.cargar
import mx.umbral.app.ui.tnum
import mx.umbral.app.util.Fechas
import mx.umbral.app.util.codificarUrl
import mx.umbral.app.util.mxn
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

/** Código en grupos de 3 para leerlo en voz alta ("PX7 M2Q"). */
private fun String.agrupado() = if (length > 4 && ' ' !in this) chunked(3).joinToString(" ") else this

@Composable
fun AccesosTab(repo: Repository, perfil: Perfil, onVerPagos: () -> Unit, onVerPaquetes: () -> Unit) {
    var refresco by remember { mutableIntStateOf(0) }
    val pases by cargar(refresco) { repo.misPases() }
    val accesos by cargar(refresco) { repo.accesosDeMiUnidad().take(4) }
    val fin = Config.MODULO_FINANZAS
    val com = Config.MODULO_COMUNIDAD
    val cargos by cargar(refresco) { if (fin) repo.cargos() else emptyList() }
    val paquetes by cargar(refresco) { if (com) repo.paquetes().filter { it.entregadoAt == null } else emptyList() }
    var nuevo by remember { mutableStateOf(false) }
    var detalle by remember { mutableStateOf<Pase?>(null) }
    var verTodos by remember { mutableStateOf(false) }
    var alerta by remember { mutableStateOf(false) }
    var avisoEmergencia by remember { mutableStateOf<String?>(null) }
    // La ayuda del botón de emergencia se oculta sola (mantenerlo dura 2 s, menos que esto)
    LaunchedEffect(avisoEmergencia) {
        if (avisoEmergencia != null) { delay(3_000); avisoEmergencia = null }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Encabezado(
                    "Hola, ${perfil.nombre.substringBefore(" ")}",
                    contexto = listOfNotNull(perfil.unidad?.nombre, perfil.condominio?.nombre).joinToString(" · "),
                ) { BotonEmergencia(onActivar = { alerta = true }, onAviso = { avisoEmergencia = it }) }
                avisoEmergencia?.let {
                    Text(
                        it, Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                        color = Rojo, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, textAlign = TextAlign.End,
                    )
                }
            }
        }
        item { CodigoPersonalTarjeta(repo) }
        item { BotonPrimario("Crear pase para visita", { nuevo = true }, icono = Icons.Rounded.Add, acento = true) }

        // Pendientes que antes vivían en la pestaña Inicio: paquetes en caseta y saldo.
        item {
            val paq = paquetes.datos.orEmpty()
            val lista = cargos.datos.orEmpty()
            val porPagar = lista.filter { it.estado != "pagado" }
            val pendiente = porPagar.sumOf { it.monto }
            val vencido = porPagar.any { !it.enRevision && it.estaVencido() }
            val filas = buildList {
                if (paq.isNotEmpty()) add("paquetes")
                if (pendiente > 0) add("saldo")
            }
            if (filas.isNotEmpty()) ListaTarjeta(filas, sangria = 64.dp) { f ->
                if (f == "paquetes") FilaPendiente(
                    Icons.Outlined.Inventory2,
                    "Tienes ${paq.size} paquete${if (paq.size > 1) "s" else ""} en caseta",
                    paq.first().let { listOfNotNull(it.paqueteria, Fechas.cuando(it.createdAt)).joinToString(" · ") },
                    onClick = onVerPaquetes,
                ) { EtiquetaAviso("Por recoger") }
                else FilaPendiente(
                    Icons.Outlined.Payments,
                    "Saldo pendiente ${pendiente.mxn()}",
                    porPagar.minByOrNull { it.vence }?.let { "Próximo vencimiento: ${Fechas.fecha(it.vence)}" } ?: "",
                    onClick = onVerPagos,
                ) { if (vencido) EtiquetaMal("Vencido") else Etiqueta("Pendiente") }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val todos = pases.datos.orEmpty()
                val activos = todos.filter { it.vigente() }
                Seccion(
                    if (verTodos) "Todos tus pases" else "Pases activos",
                    accion = when {
                        verTodos -> "Solo activos"
                        todos.size > activos.size -> "Ver todos"
                        else -> null
                    },
                    onAccion = { verTodos = !verTodos },
                )
                Contenido(pases, onReintentar = { refresco++ }) {
                    val mostrar = if (verTodos) todos else activos
                    if (mostrar.isEmpty()) Vacio(if (todos.isEmpty()) "Aún no has creado pases" else "No tienes pases activos")
                    else ListaTarjeta(mostrar, sangria = 64.dp) { p -> FilaPase(p) { detalle = p } }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Titulo2("Últimas entradas")
                Contenido(accesos, onReintentar = { refresco++ }) { lista ->
                    if (lista.isEmpty()) Secundario("Sin entradas recientes a tu unidad")
                    lista.forEach { a ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (a.tipo == TipoAcceso.visita) a.nombre else "${a.tipo.etiqueta} · ${a.nombre}",
                                Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            if (!a.permitido) Text("Denegado  ", color = Rojo, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text(
                                Fechas.cuando(a.createdAt),
                                style = MaterialTheme.typography.bodyMedium.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (alerta) AlertaSheet(repo, onCerrar = { alerta = false })
    if (nuevo) NuevoPaseSheet(repo, onCerrar = { creado ->
        nuevo = false
        if (creado != null) { refresco++; detalle = creado }
    })
    detalle?.let { p ->
        PasePantalla(p, perfil, repo, onCerrar = { detalle = null }, onCambio = { refresco++ })
    }
}

@Composable
private fun Titulo2(t: String) = Text(t, style = MaterialTheme.typography.titleMedium)

@Composable
private fun FilaPendiente(
    icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, detalle: String,
    onClick: () -> Unit, etiqueta: @Composable () -> Unit,
) {
    FilaLista(onClick = onClick) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) { Icon(icono, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary) }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (detalle.isNotBlank()) Secundario(detalle, Modifier.padding(top = 2.dp))
        }
        etiqueta()
    }
}

@Composable
private fun FilaPase(p: Pase, onClick: () -> Unit) {
    FilaLista(onClick = onClick) {
        Avatar(p.visitante)
        Column(Modifier.weight(1f)) {
            Text(p.visitante, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Secundario("${p.tipo.etiqueta} · ${p.descripcionHorario()}", Modifier.padding(top = 2.dp), maxLines = 2)
        }
        when {
            !p.activo -> Etiqueta("Cancelado")
            !p.vigente() -> Etiqueta("Expirado")
            p.modalidad == "recurrente" -> Etiqueta("Recurrente")
            else -> EtiquetaAcento("Vigente")
        }
    }
}

/** Código personal dinámico en tarjeta oscura. Se pide al tocar "Mostrar" y se regenera cada 30 s mientras está visible. */
@Composable
private fun CodigoPersonalTarjeta(repo: Repository) {
    var activo by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf<CodigoPersonal?>(null) }
    var restante by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var ampliar by remember { mutableStateOf(false) }

    LaunchedEffect(activo) {
        while (activo) {
            runCatching { repo.codigoPersonal() }
                .onSuccess { codigo = it; error = null }
                .onFailure { error = it.message; activo = false }
            val expira = codigo?.expira?.let { Fechas.parse(it) }
            do {
                restante = expira?.let { ((it - Fechas.ahora()).inWholeSeconds).toInt().coerceAtLeast(0) } ?: 0
                delay(1000)
            } while (activo && restante > 0)
        }
    }

    val c = codigo
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(Destacado)
            .border(1.dp, BordeDestacado, MaterialTheme.shapes.large).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    "TU CÓDIGO DE ENTRADA", color = SobreDestacadoSuave,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.04.em,
                )
                if (activo && c != null) {
                    Text(
                        c.codigo.agrupado(), Modifier.padding(top = 8.dp), color = SobreDestacado,
                        style = MaterialTheme.typography.headlineMedium.tnum(), fontSize = 30.sp, letterSpacing = 0.12.em,
                    )
                    Text("Se renueva en $restante s", Modifier.padding(top = 6.dp), color = SobreDestacadoSuave, fontSize = 13.sp)
                } else {
                    Text(
                        "Muéstralo en caseta para entrar. Cambia cada 30 segundos y solo sirve una vez.",
                        Modifier.padding(top = 8.dp), color = SobreDestacadoSuave, fontSize = 13.sp, lineHeight = 18.sp,
                    )
                }
            }
            if (activo && c != null) {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(FondoQr)
                        .clickable(role = Role.Button, onClickLabel = "Ampliar QR") { ampliar = true }.padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) { Image(rememberQrCodePainter("UMBRAL:${c.codigo}"), "Código QR personal, toca para ampliar", Modifier.fillMaxSize()) }
            }
        }
        if (activo && c != null) {
            TextButton(onClick = { activo = false }, contentPadding = PaddingValues(0.dp)) {
                Text("Ocultar código", color = SobreDestacado, fontWeight = FontWeight.Medium)
            }
        } else {
            BotonClaro(if (activo) "Generando…" else "Mostrar mi código", enabled = !activo) { activo = true }
        }
        error?.let { Text(it, color = SobreDestacado, style = MaterialTheme.typography.bodySmall) }
    }

    if (ampliar && activo && c != null) Dialog(onDismissRequest = { ampliar = false }) {
        Column(
            Modifier.clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.surface).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(FondoQr).padding(12.dp)) {
                Image(rememberQrCodePainter("UMBRAL:${c.codigo}"), "Código QR personal", Modifier.size(240.dp))
            }
            Text(c.codigo.agrupado(), style = MaterialTheme.typography.titleLarge.tnum(), letterSpacing = 0.16.em)
            Secundario("Se renueva en $restante s")
            BotonSecundario("Cerrar", { ampliar = false }, alto = 48.dp)
        }
    }
}

/** Botón claro sobre la tarjeta oscura. */
@Composable
private fun BotonClaro(texto: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp)).background(SobreDestacado)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(texto, color = Destacado, style = MaterialTheme.typography.labelLarge) }
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

    ModalBottomSheet(
        onDismissRequest = { onCerrar(null) }, sheetState = sheet,
        containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Crear pase", style = MaterialTheme.typography.headlineSmall)
            Campo("Nombre de la visita", visitante, { visitante = it }, placeholder = "Ej. Jorge Ramírez")

            Bloque("Tipo") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TipoAcceso.visita, TipoAcceso.proveedor, TipoAcceso.paqueteria, TipoAcceso.airbnb).forEach { t ->
                        Opcion(t.etiqueta, tipo == t, { tipo = t; if (t == TipoAcceso.proveedor) recurrente = true })
                    }
                }
            }

            Bloque("Frecuencia") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Opcion("Una vez", !recurrente, { recurrente = false })
                    Opcion("Recurrente", recurrente, { recurrente = true })
                }
            }

            if (!recurrente) {
                Bloque("Cuándo") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items((0..6).toList()) { n -> Opcion(Fechas.etiquetaDia(Fechas.diasDesdeHoy(n)), diaOffset == n, { diaOffset = n }) }
                    }
                }
                Campo("Hora aproximada (HH:mm)", hora, { hora = it }, estilo = MaterialTheme.typography.bodyLarge.tnum())
                Secundario("El pase funciona desde una hora antes y hasta el final del día.")
            } else {
                Bloque("Días permitidos") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Fechas.DIAS.forEachIndexed { i, d ->
                            val n = i + 1
                            Opcion(d, n in dias, { if (n in dias) dias.remove(n) else dias.add(n) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Campo("Desde", horaInicio, { horaInicio = it }, Modifier.weight(1f), estilo = MaterialTheme.typography.bodyLarge.tnum())
                    Campo("Hasta", horaFin, { horaFin = it }, Modifier.weight(1f), estilo = MaterialTheme.typography.bodyLarge.tnum())
                }
                Bloque("Vigencia") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7, 30, 90, 180).forEach { v -> Opcion("$v días", vigenciaDias == v, { vigenciaDias = v }) }
                    }
                }
            }

            error?.let { TextoError(it) }
            BotonPrimario(
                "Crear pase",
                cargando = guardando,
                onClick = {
                    if (visitante.isBlank()) { error = "Escribe el nombre de la visita"; return@BotonPrimario }
                    if (recurrente && dias.isEmpty()) { error = "Elige al menos un día"; return@BotonPrimario }
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
            )
        }
    }
}

@Composable
private fun Bloque(etiqueta: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        contenido()
    }
}

/** Último instante válido en palabras: valido_hasta a las 00:00 se muestra como el día anterior a las 23:59. */
private fun Pase.hastaLegible(): String {
    val t = Fechas.parse(validoHasta)?.toLocalDateTime(Fechas.zona) ?: return Fechas.fechaHora(validoHasta)
    return if (t.hour == 0 && t.minute == 0) "${Fechas.etiquetaDia(t.date.minus(1, DateTimeUnit.DAY))}, 23:59"
    else "${Fechas.etiquetaDia(t.date)}, ${Fechas.hora(validoHasta)}"
}

private fun Pase.usosLegible(): String = when {
    usosMax == null -> if (usos == 0) "Sin límite" else "Sin límite · $usos usado${if (usos > 1) "s" else ""}"
    usosMax == 1 -> if (usos == 0) "1 entrada" else "Ya se usó"
    else -> "$usos de $usosMax entradas"
}

/** Pase a pantalla completa (Pase.dc.html): QR, código, vigencia y acciones para compartir o cancelar. */
@Composable
private fun PasePantalla(p: Pase, perfil: Perfil, repo: Repository, onCerrar: () -> Unit, onCambio: () -> Unit) {
    val compartir = rememberCompartir()
    val uri = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var cancelando by remember { mutableStateOf(false) }
    val vigente = p.vigente()
    val mensaje = p.mensajeParaVisita(perfil.condominio?.nombre ?: "el condominio", perfil.unidad?.nombre)
    // wa.me abre WhatsApp (app o web); si no se puede, cae a la hoja nativa
    fun enviarWhatsApp() {
        runCatching { uri.openUri("https://wa.me/?text=${codificarUrl(mensaje)}") }.onFailure { compartir(mensaje) }
    }

    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 480.dp).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BotonIcono(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", onCerrar)
                    Text(
                        if (vigente) "Pase listo" else "Pase", Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.width(44.dp))
                }

                Column(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(p.visitante, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        Text(
                            "${p.tipo.etiqueta}${perfil.unidad?.nombre?.let { " a $it" } ?: ""}",
                            Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!vigente) {
                        Text(
                            if (!p.activo) "Pase cancelado · ya no es válido" else "Este pase ya no es válido",
                            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(RojoSuave).padding(10.dp),
                            color = Rojo, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                        )
                    }
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(FondoQr).padding(8.dp)) {
                        Image(rememberQrCodePainter("UMBRAL:${p.codigo}"), "Código QR del pase", Modifier.size(176.dp))
                    }
                    Text(p.codigo.agrupado(), style = MaterialTheme.typography.headlineSmall.tnum(), fontSize = 22.sp, letterSpacing = 0.16.em)
                    Divisor()
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (p.modalidad == "recurrente") {
                            Dato("Días", p.dias?.sorted()?.joinToString(", ") { Fechas.DIAS[it - 1] } ?: "Todos los días")
                            p.horaInicio?.let { Dato("Horario", "${it.take(5)}–${p.horaFin?.take(5) ?: "23:59"}") }
                        } else {
                            val desde = Fechas.parse(p.validoDesde)?.toLocalDateTime(Fechas.zona)?.date
                            Dato("Válido", "${desde?.let { Fechas.etiquetaDia(it) } ?: Fechas.fecha(p.validoDesde)}, desde las ${Fechas.hora(p.validoDesde)}")
                        }
                        Dato("Hasta", p.hastaLegible())
                        Dato("Usos", p.usosLegible())
                    }
                }

                if (vigente) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonPrimario("Enviar por WhatsApp", ::enviarWhatsApp, icono = Icons.AutoMirrored.Outlined.Send, acento = true)
                    BotonSecundario("Compartir por otra app", { compartir(mensaje) }, icono = Icons.Outlined.Share)
                    TextButton(
                        enabled = !cancelando,
                        onClick = {
                            cancelando = true
                            scope.launch {
                                runCatching { repo.cancelarPase(p.id) }
                                    .onSuccess { onCambio(); onCerrar() }
                                    .onFailure { error = it.message ?: "No se pudo cancelar el pase"; cancelando = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                    ) { Text(if (cancelando) "Cancelando…" else "Cancelar pase", color = Rojo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
                } else BotonSecundario("Cerrar", onCerrar)
                error?.let { TextoError(it, Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(valor, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall.tnum(), textAlign = TextAlign.End)
    }
}
