package mx.umbral.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.umbral.app.data.Perfil
import mx.umbral.app.data.Repository
import mx.umbral.app.data.Rol
import mx.umbral.app.data.SupabaseRepository
import mx.umbral.app.ui.Cargando
import mx.umbral.app.ui.UmbralTheme
import mx.umbral.app.ui.caseta.GuardiaScreen
import mx.umbral.app.ui.login.LoginScreen
import mx.umbral.app.ui.residente.ResidenteHome

/** Raíz de la app: la pantalla cambia según el rol de quien inicia sesión. */
@Composable
fun App() {
    if (!Config.configurado) {
        UmbralTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { SinConfiguracion() } }
        return
    }
    val repo: Repository = remember { SupabaseRepository(Config.SUPABASE_URL, Config.SUPABASE_ANON_KEY) }
    var perfil by remember { mutableStateOf<Perfil?>(null) }
    var iniciando by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        perfil = runCatching { repo.restaurarSesion() }.getOrNull()
        iniciando = false
    }
    val salir: () -> Unit = { scope.launch { runCatching { repo.logout() }; perfil = null } }

    UmbralTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val p = perfil
            when {
                iniciando -> Cargando()
                p == null -> LoginScreen(repo) { perfil = it }
                p.rol == Rol.guardia -> GuardiaScreen(repo, p, salir)
                p.rol == Rol.residente -> ResidenteHome(repo, p, salir)
                else -> AdminEnMovil(p, salir)
            }
        }
    }
}

@Composable
private fun SinConfiguracion() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Falta configurar Supabase", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Escribe SUPABASE_URL y SUPABASE_ANON_KEY en Config.kt y vuelve a compilar la app.",
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AdminEnMovil(p: Perfil, salir: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Hola, ${p.nombre}", style = MaterialTheme.typography.headlineSmall)
        Text(
            "La administración se gestiona desde el panel web: unidades, usuarios, pases y bitácora de accesos.",
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = salir) { Text("Cerrar sesión") }
    }
}
