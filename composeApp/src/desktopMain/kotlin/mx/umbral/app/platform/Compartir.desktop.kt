package mx.umbral.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun rememberCompartir(): (String) -> Unit = remember {
    { texto: String -> Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(texto), null) }
}
