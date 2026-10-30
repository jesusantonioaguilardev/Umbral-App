package mx.umbral.app.ui.residente

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.delay
import mx.umbral.app.data.CodigoPersonal
import mx.umbral.app.data.Perfil
import mx.umbral.app.data.Repository
import mx.umbral.app.ui.Avatar
import mx.umbral.app.ui.Fila
import mx.umbral.app.ui.Secundario
import mx.umbral.app.ui.Tarjeta
import mx.umbral.app.ui.Titulo
import mx.umbral.app.util.Fechas

@Composable
fun PerfilTab(repo: Repository, perfil: Perfil, salir: () -> Unit) {
    var activo by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf<CodigoPersonal?>(null) }
    var restante by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    // Código personal dinámico: se regenera cada 30 s mientras está visible
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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Fila {
            Avatar(perfil.nombre, 56)
            Column(Modifier.weight(1f)) {
                Text(perfil.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Secundario(listOfNotNull(perfil.unidad?.nombre, perfil.condominio?.nombre).joinToString(" · "))
            }
        }

        Tarjeta {
            Titulo("Mi código de acceso")
            Secundario("Muéstralo en caseta para entrar. Cambia cada 30 segundos y solo sirve una vez.")
            if (activo && codigo != null) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(14.dp)) {
                        Image(rememberQrCodePainter("UMBRAL:${codigo!!.codigo}"), "Código personal", Modifier.size(220.dp))
                    }
                    Text(codigo!!.codigo, fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = { restante / 30f }, modifier = Modifier.fillMaxWidth())
                    Secundario("Se renueva en $restante s")
                }
                OutlinedButton(onClick = { activo = false }, Modifier.fillMaxWidth()) { Text("Ocultar") }
            } else {
                Button(onClick = { activo = true }, Modifier.fillMaxWidth()) { Text("Mostrar mi código") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        OutlinedButton(onClick = salir, Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
    }
}
