package mx.umbral.app.platform

import androidx.compose.runtime.Composable

// Escritorio no recibe push: solo sirve para probar la app.
actual val plataformaPush: String = "desktop"

actual suspend fun tokenPush(): String? = null

@Composable
actual fun PedirPermisoNotificaciones() {}

actual fun logPush(mensaje: String, error: Throwable?) {
    println("[Push] $mensaje${error?.let { ": ${it.message}" }.orEmpty()}")
}
