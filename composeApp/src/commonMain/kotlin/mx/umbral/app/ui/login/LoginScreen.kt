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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import mx.umbral.app.ui.Secundario
import mx.umbral.app.ui.Terciario
import mx.umbral.app.ui.TextoError

@Composable
fun LoginScreen(repo: Repository, onLogin: (Perfil) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var recuperando by remember { mutableStateOf(false) }
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
                TextButton(
                    onClick = { recuperando = true },
                    modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp),
                ) { Text("¿Olvidaste tu contraseña?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
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

    if (recuperando) RecuperarContrasenaDialog(repo, correoInicial = email.trim(), onCerrar = { recuperando = false })
}

private val FORMATO_CORREO = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

/** Pide el enlace para elegir contraseña nueva. Tras enviar, siempre muestra el mismo mensaje neutral. */
@Composable
private fun RecuperarContrasenaDialog(repo: Repository, correoInicial: String, onCerrar: () -> Unit) {
    var correo by remember { mutableStateOf(correoInicial) }
    var enviando by remember { mutableStateOf(false) }
    var enviado by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun enviar() {
        val c = correo.trim()
        if (!FORMATO_CORREO.matches(c)) { error = "Escribe un correo válido."; return }
        enviando = true; error = null
        scope.launch {
            runCatching { repo.recuperarContrasena(c) }
                .onSuccess { enviado = true }
                .onFailure { error = mensajeRecuperacion(it) }
            enviando = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!enviando) onCerrar() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(if (enviado) "Revisa tu correo" else "Recuperar contraseña", style = MaterialTheme.typography.titleLarge) },
        text = {
            if (enviado) {
                Text(
                    "Si el correo está registrado, te enviamos un enlace para elegir una contraseña nueva. Revisa tu bandeja y la carpeta de spam.",
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Secundario("Escribe el correo de tu cuenta y te mandaremos un enlace para elegir una contraseña nueva.")
                    Campo(
                        "Correo", correo, { correo = it; error = null },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { if (!enviando) enviar() }),
                    )
                    error?.let { TextoError(it, Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
                }
            }
        },
        confirmButton = {
            if (enviado) {
                TextButton(onClick = onCerrar, modifier = Modifier.heightIn(min = 48.dp)) { Text("Entendido", fontWeight = FontWeight.SemiBold) }
            } else {
                TextButton(
                    onClick = { enviar() },
                    modifier = Modifier.heightIn(min = 48.dp),
                    enabled = !enviando && correo.isNotBlank(),
                ) { Text(if (enviando) "Enviando…" else "Enviar", fontWeight = FontWeight.SemiBold) }
            }
        },
        dismissButton = {
            if (!enviado) TextButton(onClick = onCerrar, modifier = Modifier.heightIn(min = 48.dp), enabled = !enviando) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

/** Traduce el fallo de Supabase/red a un mensaje en español. */
private fun mensajeRecuperacion(e: Throwable): String {
    val causas = generateSequence(e) { it.cause }.take(5).toList()
    val texto = causas.joinToString(" ") { it.message.orEmpty() }.lowercase()
    val tipos = causas.joinToString(" ") { it::class.simpleName.orEmpty() }
    return when {
        "rate limit" in texto || "rate_limit" in texto || "security purposes" in texto || "429" in texto ->
            "Espera unos minutos antes de pedir otro enlace."
        listOf("HttpRequest", "Timeout", "UnknownHost", "Connect", "Socket", "IOException").any { it in tipos } ->
            "No hay conexión a internet. Revisa tu red e inténtalo de nuevo."
        else -> "No se pudo enviar el enlace. Inténtalo de nuevo más tarde."
    }
}
