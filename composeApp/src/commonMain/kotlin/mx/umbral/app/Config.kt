package mx.umbral.app

/**
 * Configuración de Supabase (Project Settings → API).
 * Obligatorio: sin estos datos la app muestra una pantalla pidiendo configurarla.
 */
object Config {
    const val SUPABASE_URL = "https://shqlonhfxvlayhmshnzo.supabase.co"       // ej. "https://xxxx.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_3Fpxg6yeBYXI_ZnW8F3Kmw_ygyavdDZ"  // clave pública "anon"

    /** Página web pública que muestra el QR al visitante (web/pase.html del dashboard). */
    const val URL_PASE_WEB = "https://tu-dominio.mx/pase.html?c="

    /** Zona horaria del condominio. Debe coincidir con ZONA_HORARIA de web/js/config.js. */
    const val ZONA_HORARIA = "America/Hermosillo"

    /** Módulos activos. Deben coincidir con MODULOS de web/js/config.js. Accesos siempre está activo. */
    const val MODULO_COMUNIDAD = true   // avisos, paquetes, tickets, reservaciones y encuestas
    const val MODULO_FINANZAS = false   // estado de cuenta y pagos

    val configurado get() = SUPABASE_URL.isNotBlank() && SUPABASE_ANON_KEY.isNotBlank()
}
