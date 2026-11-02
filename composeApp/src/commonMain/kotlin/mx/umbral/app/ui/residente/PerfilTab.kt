package mx.umbral.app.ui.residente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mx.umbral.app.data.Perfil
import mx.umbral.app.ui.Avatar
import mx.umbral.app.ui.BotonSecundario
import mx.umbral.app.ui.Encabezado
import mx.umbral.app.ui.FilaLista
import mx.umbral.app.ui.ListaTarjeta
import mx.umbral.app.ui.Rojo
import mx.umbral.app.ui.Secundario
import mx.umbral.app.ui.Tarjeta
import mx.umbral.app.ui.Fila

// El código personal ahora vive en Accesos (inicio del residente).
@Composable
fun PerfilTab(perfil: Perfil, salir: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Encabezado("Perfil")

        Tarjeta(padding = 20.dp) {
            Fila {
                Avatar(perfil.nombre, 56)
                Column(Modifier.weight(1f)) {
                    Text(perfil.nombre, style = MaterialTheme.typography.titleLarge)
                    Secundario("Residente", Modifier.padding(top = 2.dp))
                }
            }
        }

        val datos = listOfNotNull(
            perfil.unidad?.let { "Unidad" to (listOfNotNull(it.nombre, it.torre?.let { t -> "Torre $t" }).joinToString(" · ")) },
            perfil.condominio?.let { "Condominio" to (listOfNotNull(it.nombre, it.ciudad).joinToString(" · ")) },
        )
        if (datos.isNotEmpty()) ListaTarjeta(datos) { (k, v) ->
            FilaLista {
                Text(k, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(v, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonSecundario("Cerrar sesión", salir, icono = Icons.AutoMirrored.Outlined.Logout, color = Rojo)
            Secundario("Para cambiar tus datos, pídeselo a la administración.")
        }
    }
}
