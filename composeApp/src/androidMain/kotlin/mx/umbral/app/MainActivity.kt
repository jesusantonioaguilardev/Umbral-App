package mx.umbral.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import mx.umbral.app.platform.ContextoAndroid
import mx.umbral.app.platform.DestinoPush
import mx.umbral.app.push.Notificaciones

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ContextoAndroid.app = applicationContext
        // El canal debe existir antes de que llegue un push con la app cerrada.
        Notificaciones.crearCanal(this)
        // Al recrearse (o abrirse desde recientes) el Intent viejo no debe volver a navegar.
        val desdeHistorial = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !desdeHistorial) leerDestino(intent)
        setContent { App() }
    }

    // launchMode singleTop: tocar una notificación con la app abierta llega aquí.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leerDestino(intent)
    }

    /** El tipo del push viaja como extra: lo arma Notificaciones.mostrar (primer plano) o FCM (segundo plano). */
    private fun leerDestino(intent: Intent) {
        val tipo = intent.getStringExtra("tipo") ?: return
        DestinoPush.avisar(tipo)
        intent.removeExtra("tipo")
    }
}
