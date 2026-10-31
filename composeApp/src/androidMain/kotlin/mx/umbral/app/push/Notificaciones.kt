package mx.umbral.app.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import mx.umbral.app.MainActivity
import mx.umbral.app.R

/** Canal y armado de notificaciones locales de Umbral. */
object Notificaciones {
    // Id del canal "Umbral"; también va en el manifiesto como canal por defecto de FCM (el push llega sin channel_id).
    const val CANAL = "umbral"
    // Canal aparte para las alertas del botón de emergencia (data.tipo == "alerta"): el usuario puede
    // silenciar los avisos normales sin perder estas. Para que el push en segundo plano también use este canal,
    // el backend debe mandar android.notification.channel_id = "emergencias".
    const val CANAL_EMERGENCIAS = "emergencias"

    /** Crea los canales "Umbral" y "Emergencias" (no hace nada si ya existen). */
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL, "Umbral", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Visitas en la caseta, paquetes y avisos del condominio"
        }
        // IMPORTANCE_HIGH es la importancia más alta que Android respeta para apps (IMPORTANCE_MAX no se usa)
        val emergencias = NotificationChannel(CANAL_EMERGENCIAS, "Emergencias", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alertas de emergencia del condominio"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(canal, emergencias))
    }

    fun mostrar(context: Context, titulo: String, cuerpo: String, datos: Map<String, String>) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return
        crearCanal(context)

        // Al tocarla abre la app; los datos (tipo e ids) viajan como extras para navegar después.
        val abrir = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            datos.forEach { (k, v) -> putExtra(k, v) }
        }
        val pendiente = PendingIntent.getActivity(
            context, 0, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val emergencia = datos["tipo"] == "alerta"
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, if (emergencia) CANAL_EMERGENCIAS else CANAL)
        else @Suppress("DEPRECATION") Notification.Builder(context)
            .setPriority(if (emergencia) Notification.PRIORITY_MAX else Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
        if (emergencia) builder.setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PUBLIC)
        val notificacion = builder
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(Notification.BigTextStyle().bigText(cuerpo))
            .setAutoCancel(true)
            .setContentIntent(pendiente)
            .build()
        // Con el mismo notificacion_id no se duplica; si no viene, una nueva.
        val id = datos["notificacion_id"]?.toLongOrNull()?.toInt() ?: System.currentTimeMillis().toInt()
        manager.notify(id, notificacion)
    }
}
