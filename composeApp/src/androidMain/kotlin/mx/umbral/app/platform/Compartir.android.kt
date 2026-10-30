package mx.umbral.app.platform

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberCompartir(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { texto: String ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, texto)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir pase").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
