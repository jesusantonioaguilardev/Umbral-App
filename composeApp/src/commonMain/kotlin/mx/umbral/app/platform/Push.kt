package mx.umbral.app.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/** Plataforma como la espera la RPC registrar_dispositivo: android, ios, web o desktop. */
expect val plataformaPush: String

/** Token de push (FCM) de este dispositivo, o null si las notificaciones no están disponibles. */
expect suspend fun tokenPush(): String?

/** Pide el permiso de notificaciones si la plataforma lo necesita (Android 13+). No dibuja nada. */
@Composable
expect fun PedirPermisoNotificaciones()

/** Escribe en el log de la plataforma (logcat en Android, consola en las demás). */
expect fun logPush(mensaje: String, error: Throwable? = null)

/** Tokens nuevos que avisa la plataforma (onNewToken) para volver a registrarlos con la sesión abierta. */
object TokensPush {
    private val _nuevos = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nuevos: SharedFlow<String> = _nuevos
    fun avisar(token: String) { _nuevos.tryEmit(token) }
}

/**
 * Destino pendiente al tocar una notificación: el `data.tipo` del push (acceso, paquete, aviso, alerta).
 * La plataforma lo llena desde el Intent y ResidenteHome lo consume una sola vez; si aún no hay sesión, espera.
 */
object DestinoPush {
    /** Tipos que manda la base (schema.sql, notificaciones.datos.tipo). Los demás se ignoran. */
    val TIPOS = setOf("acceso", "paquete", "aviso", "alerta")

    private val _pendiente = MutableStateFlow<String?>(null)
    val pendiente: StateFlow<String?> = _pendiente.asStateFlow()

    fun avisar(tipo: String?) { if (tipo in TIPOS) _pendiente.value = tipo }

    /** Devuelve el destino pendiente y lo borra, para que no se navegue dos veces. */
    fun consumir(): String? = _pendiente.getAndUpdate { null }
}
