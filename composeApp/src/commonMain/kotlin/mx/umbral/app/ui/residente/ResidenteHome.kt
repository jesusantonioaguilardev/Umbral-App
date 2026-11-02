package mx.umbral.app.ui.residente

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.umbral.app.Config
import mx.umbral.app.data.Perfil
import mx.umbral.app.data.Repository
import mx.umbral.app.ui.Divisor
import mx.umbral.app.ui.Terciario

// Accesos es el inicio del residente (como en el mockup aprobado).
private enum class Tab(val titulo: String, val icono: ImageVector) {
    Accesos("Accesos", Icons.Outlined.QrCode2),
    Comunidad("Comunidad", Icons.Outlined.Groups),
    Pagos("Pagos", Icons.Outlined.CreditCard),
    Perfil("Perfil", Icons.Outlined.Person),
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
    var tab by remember { mutableStateOf(Tab.Accesos) }
    var seccionComunidad by remember { mutableIntStateOf(SECCION_COMUNICADOS) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BarraInferior(tab) { t -> if (t == Tab.Comunidad) seccionComunidad = SECCION_COMUNICADOS; tab = t }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                Tab.Accesos -> AccesosTab(
                    repo, perfil,
                    onVerPagos = { tab = Tab.Pagos },
                    onVerPaquetes = { seccionComunidad = SECCION_PAQUETES; tab = Tab.Comunidad },
                )
                Tab.Pagos -> PagosTab(repo)
                Tab.Comunidad -> ComunidadTab(repo, seccionComunidad)
                Tab.Perfil -> PerfilTab(perfil, salir)
            }
        }
    }
}

/** Barra inferior propia: blanca con borde superior, sin píldora de Material. */
@Composable
private fun BarraInferior(actual: Tab, onCambio: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Divisor()
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                .height(64.dp).padding(horizontal = 8.dp).selectableGroup(),
        ) {
            tabsVisibles.forEach { t ->
                val activo = t == actual
                val color = if (activo) MaterialTheme.colorScheme.onSurface else Terciario
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .semantics { selected = activo }
                        .clickable(role = Role.Tab) { onCambio(t) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(t.icono, null, Modifier.size(22.dp), tint = color)
                        Text(
                            t.titulo, color = color, fontSize = 11.sp, lineHeight = 14.sp,
                            fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
