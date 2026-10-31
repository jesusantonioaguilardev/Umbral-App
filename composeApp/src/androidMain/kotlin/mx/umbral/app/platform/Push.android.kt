package mx.umbral.app.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Contexto de la aplicación; lo fijan MainActivity y el servicio de mensajes. */
@SuppressLint("StaticFieldLeak")
internal object ContextoAndroid {
    lateinit var app: Context
    val listo get() = ::app.isInitialized
}

actual val plataformaPush: String = "android"

actual suspend fun tokenPush(): String? {
    if (!ContextoAndroid.listo) return null
    // Sin google-services.json Firebase no se inicializa: no hay push, pero tampoco crash.
    if (FirebaseApp.getApps(ContextoAndroid.app).isEmpty()) {
        logPush("Firebase no está configurado (falta google-services.json)")
        return null
    }
    return suspendCancellableCoroutine { cont ->
        // firebase-messaging 26 marca getToken() como obsoleto en favor de register()/onRegistered,
        // pero sigue siendo la forma de leer el token actual que pide el backend.
        @Suppress("DEPRECATION")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
            if (tarea.isSuccessful) cont.resume(tarea.result)
            else cont.resumeWithException(tarea.exception ?: IllegalStateException("No se obtuvo el token de FCM"))
        }
    }
}

@Composable
actual fun PedirPermisoNotificaciones() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (!concedido) logPush("El usuario no concedió el permiso de notificaciones")
    }
    LaunchedEffect(Unit) {
        val permiso = Manifest.permission.POST_NOTIFICATIONS
        if (context.checkSelfPermission(permiso) != PackageManager.PERMISSION_GRANTED) lanzador.launch(permiso)
    }
}

actual fun logPush(mensaje: String, error: Throwable?) {
    Log.w("UmbralPush", mensaje, error)
}
