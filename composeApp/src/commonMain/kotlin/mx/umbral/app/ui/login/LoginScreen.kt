package mx.umbral.app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mx.umbral.app.data.Perfil
import mx.umbral.app.data.Repository
import mx.umbral.app.ui.BotonPrimario
import mx.umbral.app.ui.Campo
import mx.umbral.app.ui.Terciario
import mx.umbral.app.ui.TextoError

@Composable
fun LoginScreen(repo: Repository, onLogin: (Perfil) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listo = !cargando && email.isNotBlank() && password.isNotBlank()

    fun entrar() {
        val e = email; val p = password
        cargando = true; error = null
        scope.launch {
            runCatching { repo.login(e, p) }
                .onSuccess(onLogin)
                .onFailure { error = if (it.message?.contains("Invalid login") == true) "Correo o contraseña incorrectos" else it.message }
            cargando = false
        }
    }

    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 48.dp, bottom = 32.dp),
        ) {
            // Marca
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Home, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary) }
                Text(
                    "Umbral", Modifier.padding(start = 10.dp),
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em,
                )
            }
            Spacer(Modifier.height(64.dp))
            Text("Bienvenido", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Entra con la cuenta que te dio la administración de tu condominio.",
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(36.dp))
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Campo(
                    "Correo", email, { email = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                Campo(
                    "Contraseña", password, { password = it },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (listo) entrar() }),
                )
                error?.let { TextoError(it) }
                BotonPrimario("Entrar", { entrar() }, Modifier.padding(top = 8.dp), enabled = listo, cargando = cargando, alto = 50.dp)
            }
            Text(
                "¿No tienes cuenta? Pídela a la administración.\nUmbral · Control de accesos y comunidad",
                Modifier.fillMaxWidth().padding(top = 40.dp),
                fontSize = 12.sp, lineHeight = 18.sp, color = Terciario, textAlign = TextAlign.Center,
            )
        }
    }
}
