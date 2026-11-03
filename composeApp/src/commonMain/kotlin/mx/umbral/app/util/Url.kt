package mx.umbral.app.util

private const val HEX = "0123456789ABCDEF"

/** Codifica un texto para usarlo en una URL (RFC 3986, UTF-8 por bytes). Los espacios van como %20. */
fun codificarUrl(texto: String): String = buildString {
    for (b in texto.encodeToByteArray()) {
        val n = b.toInt() and 0xFF
        val c = n.toChar()
        if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '-' || c == '.' || c == '_' || c == '~') append(c)
        else append('%').append(HEX[n shr 4]).append(HEX[n and 0x0F])
    }
}
