package mx.umbral.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import mx.umbral.app.platform.ContextoAndroid
import mx.umbral.app.push.Notificaciones

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ContextoAndroid.app = applicationContext
        // El canal debe existir antes de que llegue un push con la app cerrada.
        Notificaciones.crearCanal(this)
        setContent { App() }
    }
}
