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

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL, "Umbral", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Visitas en la caseta, paquetes y avisos del condominio"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
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
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, CANAL)
        else @Suppress("DEPRECATION") Notification.Builder(context).setPriority(Notification.PRIORITY_HIGH)
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
