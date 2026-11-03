package mx.umbral.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import mx.umbral.app.platform.ContextoAndroid
import mx.umbral.app.platform.TokensPush

/**
 * Recibe los mensajes de FCM. Con la app en segundo plano Android muestra solo la parte `notification`;
 * aquí llegan los de primer plano (y los de solo `data`), que mostramos nosotros.
 */
class UmbralMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        ContextoAndroid.app = applicationContext
    }

    // Si la app está abierta con sesión, App.kt lo vuelve a registrar; si no, se registra al abrirla.
    @Deprecated("firebase-messaging 26 lo reemplaza por onRegistered; se conserva porque aún se llama")
    override fun onNewToken(token: String) {
        TokensPush.avisar(token)
    }

    // API nueva de firebase-messaging 26: mismo trato que onNewToken.
    override fun onRegistered(token: String) {
        TokensPush.avisar(token)
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val n = mensaje.notification
        val titulo = n?.title ?: mensaje.data["titulo"] ?: return
        val cuerpo = n?.body ?: mensaje.data["cuerpo"].orEmpty()
        Notificaciones.mostrar(this, titulo, cuerpo, mensaje.data)
    }
}
