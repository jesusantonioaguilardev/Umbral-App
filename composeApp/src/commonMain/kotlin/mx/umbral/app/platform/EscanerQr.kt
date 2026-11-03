package mx.umbral.app.platform

import androidx.compose.runtime.Composable

/** true si este dispositivo puede leer QR con la cámara (hoy solo Android con cámara). */
@Composable
expect fun escanerQrDisponible(): Boolean

/**
 * Cámara a pantalla completa que lee un QR. Pide el permiso si hace falta.
 * Llama `onLeido` una sola vez con el texto del QR y se espera que quien la muestra la cierre;
 * `onCerrar` si el usuario sale sin leer nada.
 */
@Composable
expect fun EscanerQr(onLeido: (String) -> Unit, onCerrar: () -> Unit)
