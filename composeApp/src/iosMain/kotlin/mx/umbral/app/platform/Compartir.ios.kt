package mx.umbral.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun rememberCompartir(): (String) -> Unit = remember {
    { texto: String ->
        val vc = UIActivityViewController(activityItems = listOf(texto), applicationActivities = null)
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(vc, animated = true, completion = null)
    }
}
