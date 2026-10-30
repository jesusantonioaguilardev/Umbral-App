package mx.umbral.app.ui.residente

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.umbral.app.Config
import mx.umbral.app.data.Perfil
import mx.umbral.app.data.Repository
import mx.umbral.app.ui.Avatar
import mx.umbral.app.ui.Contenido
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

private enum class Tab(val titulo: String, val icono: ImageVector) {
    Inicio("Inicio", Icons.Filled.Home),
    Accesos("Accesos", Icons.Filled.QrCode2),
    Pagos("Pagos", Icons.Filled.Payments),
    Comunidad("Comunidad", Icons.Filled.Groups),
    Perfil("Perfil", Icons.Filled.Person),
}

private val tabsVisibles = Tab.entries.filter {
    when (it) {
        Tab.Pagos -> Config.MODULO_FINANZAS
        Tab.Comunidad -> Config.MODULO_COMUNIDAD
        else -> true
    }
}

@Composable
fun ResidenteHome(repo: Repository, perfil: Perfil, salir: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.Inicio) }
    var abrirNuevoPase by remember { mutableStateOf(false) }
    var seccionComunidad by remember { mutableIntStateOf(SECCION_COMUNICADOS) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabsVisibles.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t, onClick = { if (t == Tab.Comunidad) seccionComunidad = SECCION_COMUNICADOS; tab = t },
                        icon = { Icon(t.icono, null) }, label = { Text(t.titulo, fontSize = 11.sp) },
                    )
                }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                Tab.Inicio -> InicioTab(repo, perfil,
                    onNuevoAcceso = { abrirNuevoPase = true; tab = Tab.Accesos },
                    irA = { tab = it },
                    irAComunidad = { seccionComunidad = it; tab = Tab.Comunidad })
                Tab.Accesos -> AccesosTab(repo, perfil, abrirNuevo = abrirNuevoPase, onNuevoCerrado = { abrirNuevoPase = false })
                Tab.Pagos -> PagosTab(repo)
                Tab.Comunidad -> ComunidadTab(repo, seccionComunidad)
                Tab.Perfil -> PerfilTab(repo, perfil, salir)
            }
        }
    }
}

@Composable
private fun InicioTab(repo: Repository, perfil: Perfil, onNuevoAcceso: () -> Unit, irA: (Tab) -> Unit, irAComunidad: (Int) -> Unit) {
    var refresco by remember { mutableIntStateOf(0) }
    val fin = Config.MODULO_FINANZAS
    val com = Config.MODULO_COMUNIDAD
    val cargos by cargar(refresco) { if (fin) repo.cargos() else emptyList() }
    val paquetes by cargar(refresco) { if (com) repo.paquetes().filter { it.entregadoAt == null } else emptyList() }
    val avisos by cargar(refresco) { if (com) repo.avisos().take(3) else emptyList() }
    val accesos by cargar(refresco) { repo.accesosDeMiUnidad().take(4) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Fila {
                Column(Modifier.weight(1f)) {
                    Text("Hola, ${perfil.nombre.substringBefore(" ")}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Secundario(listOfNotNull(perfil.unidad?.nombre, perfil.condominio?.nombre).joinToString(" · "))
                }
                Avatar(perfil.nombre)
            }
        }
        item {
            Button(onClick = onNuevoAcceso, Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, null); Text("  Nuevo acceso para visita", fontWeight = FontWeight.SemiBold)
            }
        }
        if (com) item {
            OutlinedButton(onClick = { irAComunidad(SECCION_REPORTES) }, Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Build, null); Text("  Reportar un problema")
            }
        }
        if (fin) item {
            Contenido(cargos) { lista ->
                val pendiente = lista.filter { it.estado != "pagado" }.sumOf { it.monto }
                val vencido = lista.any { it.estado == "vencido" }
                Tarjeta(Modifier.clickable { irA(Tab.Pagos) }) {
                    Fila {
                        Secundario("Saldo de tu unidad", Modifier.weight(1f))
                        when {
                            vencido -> EtiquetaMal("Vencido")
                            pendiente > 0 -> EtiquetaAviso("Pendiente")
                            else -> EtiquetaOk("Al corriente")
                        }
                    }
                    Text(pendiente.mxn(), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    lista.firstOrNull { it.estado != "pagado" }?.let { Secundario("Próximo vencimiento: ${Fechas.fecha(it.vence)} · Toca para pagar") }
                }
            }
        }
        if (com) item {
            Contenido(paquetes) { lista ->
                if (lista.isNotEmpty()) Tarjeta(Modifier.clickable { irA(Tab.Comunidad) }) {
                    Titulo("📦 Tienes ${lista.size} paquete${if (lista.size > 1) "s" else ""} en caseta")
                    lista.forEach { Secundario("${it.paqueteria ?: "Paquete"} · ${it.descripcion ?: ""} · ${Fechas.fechaHora(it.createdAt)}") }
                }
            }
        }
        if (com) item {
            Fila {
                Titulo("Comunicados", Modifier.weight(1f))
                TextButton(onClick = { irAComunidad(SECCION_COMUNICADOS) }) { Text("Ver todos") }
            }
        }
        if (com) item {
            Contenido(avisos) { lista ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    lista.forEach { a ->
                        Tarjeta {
                            Fila { Text(a.titulo, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Secundario(Fechas.fecha(a.createdAt)) }
                            a.cuerpo?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
        }
        item { Titulo("Últimos accesos a tu unidad") }
        item {
            Contenido(accesos) { lista ->
                Tarjeta {
                    if (lista.isEmpty()) Secundario("Sin accesos recientes")
                    lista.forEach { a ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Secundario(Fechas.hora(a.createdAt), Modifier.padding(end = 12.dp))
                            Text(a.nombre, Modifier.weight(1f))
                            if (a.permitido) EtiquetaOk(a.tipo.etiqueta) else EtiquetaMal("Denegado")
                        }
                    }
                }
            }
        }
    }
}
