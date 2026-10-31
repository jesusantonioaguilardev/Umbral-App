package mx.umbral.app.ui.residente

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import mx.umbral.app.data.Cargo
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
import mx.umbral.app.ui.cargar
import mx.umbral.app.util.Fechas
import mx.umbral.app.util.mxn

private fun String.clabeLegible() = chunked(3).joinToString(" ")

/** Pendiente con fecha de vencimiento ya pasada (o marcado vencido por administración). */
fun Cargo.estaVencido(hoy: LocalDate = Fechas.hoy()): Boolean = estado == "vencido" ||
    (estado == "pendiente" && runCatching { LocalDate.parse(vence.take(10)) < hoy }.getOrDefault(false))

@Composable
fun PagosTab(repo: Repository) {
    var refresco by remember { mutableIntStateOf(0) }
    val cargos by cargar(refresco) { repo.cargos() }
    val unidad by cargar { repo.miUnidad() }
    var pagar by remember { mutableStateOf<Cargo?>(null) }
    val clip = LocalClipboardManager.current

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Cuotas y pagos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Contenido(cargos, onReintentar = { refresco++ }) { lista ->
                val hoy = Fechas.hoy()
                val porPagar = lista.filter { it.estado != "pagado" }
                val pendiente = porPagar.sumOf { it.monto }
                val vencidos = porPagar.filter { !it.enRevision && it.estaVencido(hoy) }
                Tarjeta(fondo = MaterialTheme.colorScheme.primaryContainer) {
                    Secundario("Saldo pendiente")
                    Text(pendiente.mxn(), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Secundario(if (pendiente == 0.0) "Estás al corriente. ¡Gracias!" else "${porPagar.size} cargo(s) por pagar")
                    if (vencidos.isNotEmpty()) EtiquetaMal(
                        "${vencidos.size} ${if (vencidos.size == 1) "cargo vencido" else "cargos vencidos"} · ${vencidos.sumOf { it.monto }.mxn()}"
                    )
                }
            }
        }
        item {
            unidad.datos?.clabe?.let { clabe ->
                Tarjeta {
                    Secundario("CLABE SPEI de tu unidad")
                    Fila {
                        Text(clabe.clabeLegible(), Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { clip.setText(AnnotatedString(clabe)) }) { Icon(Icons.Filled.ContentCopy, "Copiar CLABE") }
                    }
                    Secundario("Tus transferencias a esta CLABE se concilian automáticamente.")
                }
            }
        }
        item { Titulo("Estado de cuenta") }
        cargos.datos?.let { lista ->
            items(lista, key = { it.id }) { c ->
                Tarjeta(Modifier.clickable(enabled = c.estado != "pagado" && !c.enRevision) { pagar = c }) {
                    Fila {
                        Column(Modifier.weight(1f)) {
                            Text(c.concepto, fontWeight = FontWeight.SemiBold)
                            Secundario("Vence ${Fechas.fecha(c.vence)}" + if (c.extraordinaria) " · Extraordinaria" else "")
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(c.monto.mxn(), fontWeight = FontWeight.Bold)
                            when {
                                c.estado == "pagado" -> EtiquetaOk("Pagado")
                                c.enRevision -> Etiqueta("En revisión")
                                c.estaVencido() -> EtiquetaMal("Vencido")
                                else -> EtiquetaAviso("Pagar")
                            }
                        }
                    }
                }
            }
        }
    }

    pagar?.let { c -> PagarSheet(repo, c, unidad.datos?.clabe, onCerrar = { ok -> pagar = null; if (ok) refresco++ }) }
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

    ModalBottomSheet(onDismissRequest = { onCerrar(false) }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Pagar por SPEI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Secundario(cargo.concepto)
            Text(cargo.monto.mxn(), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            if (clabe != null) {
                Secundario("Transfiere a la CLABE de tu unidad:")
                Fila(Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                    Text(clabe.clabeLegible(), Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { clip.setText(AnnotatedString(clabe)) }) { Icon(Icons.Filled.ContentCopy, "Copiar") }
                }
            }
            OutlinedTextField(referencia, { referencia = it }, Modifier.fillMaxWidth(), label = { Text("Clave de rastreo (opcional)") }, singleLine = true)
            estado?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { reportar("spei") }, Modifier.fillMaxWidth().height(50.dp), enabled = !enviando) { Text("Ya transferí") }
            // Integra aquí tu pasarela de tarjeta (Stripe, Conekta, Openpay…) y llama a reportarPago al confirmar.
            OutlinedButton(onClick = { reportar("tarjeta") }, Modifier.fillMaxWidth().height(50.dp), enabled = !enviando) { Text("Pagar con tarjeta") }
            Etiqueta("Comisión SPEI según tu proveedor bancario")
        }
    }
}
