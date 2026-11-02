package mx.umbral.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

// Pendiente: en iOS el escáner requiere AVFoundation (AVCaptureSession + AVCaptureMetadataOutput con tipo QR)
// y NSCameraUsageDescription en Info.plist; solo se puede compilar y probar en Mac. Por ahora no está disponible.
@Composable
actual fun escanerQrDisponible(): Boolean = false

@Composable
actual fun EscanerQr(onLeido: (String) -> Unit, onCerrar: () -> Unit) {
    LaunchedEffect(Unit) { onCerrar() }
}
