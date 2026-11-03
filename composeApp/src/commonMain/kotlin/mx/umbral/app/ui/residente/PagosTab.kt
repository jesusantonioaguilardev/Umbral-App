package mx.umbral.app.ui.residente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import mx.umbral.app.data.Cargo
import mx.umbral.app.data.Repository
import mx.umbral.app.ui.BotonIcono
import mx.umbral.app.ui.BotonPrimario
import mx.umbral.app.ui.BotonSecundario
import mx.umbral.app.ui.Campo
import mx.umbral.app.ui.Contenido
import mx.umbral.app.ui.Divisor
import mx.umbral.app.ui.Encabezado
import mx.umbral.app.ui.Etiqueta
import mx.umbral.app.ui.EtiquetaAcento
import mx.umbral.app.ui.EtiquetaMal
import mx.umbral.app.ui.FilaLista
import mx.umbral.app.ui.ListaTarjeta
import mx.umbral.app.ui.Rojo
import mx.umbral.app.ui.Secundario
import mx.umbral.app.ui.Tarjeta
import mx.umbral.app.ui.Terciario
import mx.umbral.app.ui.TextoError
import mx.umbral.app.ui.Titulo
import mx.umbral.app.ui.Vacio
import mx.umbral.app.ui.Verde
import mx.umbral.app.ui.cargar
import mx.umbral.app.ui.tnum
import mx.umbral.app.util.Fechas
import mx.umbral.app.util.mxn

private fun String.clabeLegible() = chunked(3).joinToString(" ")

/** Pendiente con fecha de vencimiento ya pasada (o marcado vencido por administración). */
fun Cargo.estaVencido(hoy: LocalDate = Fechas.hoy()): Boolean = estado == "vencido" ||
    (estado == "pendiente" && runCatching { LocalDate.parse(vence.take(10)) < hoy }.getOrDefault(false))

private fun Cargo.sePuedePagar() = estado != "pagado" && !enRevision

@Composable
fun PagosTab(repo: Repository) {
    var refresco by remember { mutableIntStateOf(0) }
    val cargos by cargar(refresco) { repo.cargos() }
    val unidad by cargar { repo.miUnidad() }
    var pagar by remember { mutableStateOf<Cargo?>(null) }
    var copiada by remember { mutableStateOf(false) }
    val clip = LocalClipboardManager.current

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Encabezado("Pagos") }
        item {
            Contenido(cargos, onReintentar = { refresco++ }) { lista ->
                val hoy = Fechas.hoy()
                val porPagar = lista.filter { it.estado != "pagado" }
                val pendiente = porPagar.sumOf { it.monto }
                val vencidos = porPagar.filter { !it.enRevision && it.estaVencido(hoy) }
                Tarjeta(padding = 20.dp) {
                    Secundario("Saldo pendiente")
                    Text(
                        pendiente.mxn(), style = MaterialTheme.typography.headlineMedium.tnum(),
                        fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.02).em,
                    )
                    when {
                        vencidos.isNotEmpty() -> Text(
                            "${vencidos.size} ${if (vencidos.size == 1) "cargo vencido" else "cargos vencidos"} · ${vencidos.sumOf { it.monto }.mxn()}",
                            color = Rojo, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                        )
                        pendiente == 0.0 -> Text("Estás al corriente. ¡Gracias!", color = Verde, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        else -> Secundario("${porPagar.size} cargo(s) por pagar")
                    }
                    unidad.datos?.clabe?.let { clabe ->
                        Divisor(Modifier.padding(top = 10.dp, bottom = 10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("CLABE SPEI de tu unidad", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    clabe.clabeLegible(), Modifier.padding(top = 4.dp),
                                    style = MaterialTheme.typography.bodyLarge.tnum(), fontWeight = FontWeight.Medium, letterSpacing = 0.04.em,
                                )
                            }
                            BotonIcono(Icons.Outlined.ContentCopy, "Copiar CLABE", { clip.setText(AnnotatedString(clabe)); copiada = true }, redondo = false)
                        }
                        Secundario(if (copiada) "CLABE copiada." else "Tus transferencias a esta CLABE se concilian automáticamente.")
                    }
                }
            }
        }
        cargos.datos?.let { lista ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Titulo("Estado de cuenta")
                    if (lista.isEmpty()) Vacio("Aún no hay cargos en tu unidad")
                    else ListaTarjeta(lista) { c -> FilaCargo(c) { pagar = c } }
                }
            }
            // Atajo del mockup: abre el pago del cargo más urgente (vencido primero, luego el que vence antes).
            val siguiente = lista.filter { it.sePuedePagar() }.sortedWith(compareBy({ !it.estaVencido() }, { it.vence })).firstOrNull()
            if (siguiente != null) item {
                BotonPrimario("Ya transferí, avisar a administración", { pagar = siguiente })
            }
        }
    }

    pagar?.let { c -> PagarSheet(repo, c, unidad.datos?.clabe, onCerrar = { ok -> pagar = null; if (ok) refresco++ }) }
}

@Composable
private fun FilaCargo(c: Cargo, onPagar: () -> Unit) {
    val pagado = c.estado == "pagado"
    val vencido = !pagado && !c.enRevision && c.estaVencido()
    FilaLista(onClick = if (c.sePuedePagar()) onPagar else null) {
        Column(Modifier.weight(1f)) {
            Text(c.concepto, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Secundario(
                (if (vencido) "Venció " else "Vence ") + Fechas.fecha(c.vence) + if (c.extraordinaria) " · Extraordinaria" else "",
                Modifier.padding(top = 2.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                c.monto.mxn(), style = MaterialTheme.typography.bodyLarge.tnum(), fontWeight = FontWeight.SemiBold,
                color = if (pagado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            when {
                pagado -> EtiquetaAcento("✓ Pagado")
                c.enRevision -> Etiqueta("En revisión")
                vencido -> EtiquetaMal("Vencido")
                else -> Etiqueta("Pendiente")
            }
        }
        if (c.sePuedePagar()) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Pagar", Modifier.size(20.dp), tint = Terciario)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PagarSheet(repo: Repository, cargo: Cargo, clabe: String?, onCerrar: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val clip = LocalClipboardManager.current
    var referencia by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    fun reportar(metodo: String) {
        enviando = true
        scope.launch {
            runCatching { repo.reportarPago(cargo, metodo, referencia.ifBlank { null }) }
                .onSuccess { onCerrar(true) }
                .onFailure { estado = it.message; enviando = false }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onCerrar(false) }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text("Pagar por SPEI", style = MaterialTheme.typography.headlineSmall)
                Secundario(cargo.concepto, Modifier.padding(top = 4.dp))
            }
            Text(cargo.monto.mxn(), style = MaterialTheme.typography.headlineMedium.tnum(), fontSize = 32.sp)
            if (clabe != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Transfiere a la CLABE de tu unidad", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(clabe.clabeLegible(), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge.tnum(), fontWeight = FontWeight.Medium, letterSpacing = 0.04.em)
                        BotonIcono(Icons.Outlined.ContentCopy, "Copiar CLABE", { clip.setText(AnnotatedString(clabe)) }, redondo = false)
                    }
                }
            }
            Campo("Clave de rastreo (opcional)", referencia, { referencia = it }, estilo = MaterialTheme.typography.bodyLarge.tnum())
            estado?.let { TextoError(it) }
            BotonPrimario("Ya transferí", { reportar("spei") }, cargando = enviando)
            // Integra aquí tu pasarela de tarjeta (Stripe, Conekta, Openpay…) y llama a reportarPago al confirmar.
            BotonSecundario("Pagar con tarjeta", { reportar("tarjeta") }, enabled = !enviando)
            Secundario("Comisión SPEI según tu proveedor bancario.")
        }
    }
}
