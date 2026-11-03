package mx.umbral.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

// En escritorio no hay escáner: la caseta escribe el código (o usa un lector QR tipo teclado).
@Composable
actual fun escanerQrDisponible(): Boolean = false

@Composable
actual fun EscanerQr(onLeido: (String) -> Unit, onCerrar: () -> Unit) {
    LaunchedEffect(Unit) { onCerrar() }
}
