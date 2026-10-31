package mx.umbral.app.platform

import androidx.compose.runtime.Composable

actual val plataformaPush: String = "ios"

// Último token de FCM que mandó Swift. Hoy siempre es null: Firebase en iOS se agrega por
// Swift Package Manager desde Xcode (requiere Mac). Pasos completos en iosApp/PUSH_IOS.md.
private var ultimoToken: String? = null

/** Puente Swift→Kotlin: Swift lo llama desde MessagingDelegate (PushIosKt.recibirTokenPush(token:)). */
fun recibirTokenPush(token: String) {
    ultimoToken = token
    TokensPush.avisar(token)
}

actual suspend fun tokenPush(): String? = ultimoToken

// En iOS el permiso lo pide Swift (UNUserNotificationCenter) al arrancar; ver PUSH_IOS.md.
@Composable
actual fun PedirPermisoNotificaciones() {}

actual fun logPush(mensaje: String, error: Throwable?) {
    println("[Push] $mensaje${error?.let { ": ${it.message}" }.orEmpty()}")
}
