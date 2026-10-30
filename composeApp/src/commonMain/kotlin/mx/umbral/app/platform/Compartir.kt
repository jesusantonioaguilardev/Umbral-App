package mx.umbral.app.platform

import androidx.compose.runtime.Composable

/** Devuelve una función que abre la hoja nativa para compartir (WhatsApp, SMS, etc.). */
@Composable
expect fun rememberCompartir(): (texto: String) -> Unit
