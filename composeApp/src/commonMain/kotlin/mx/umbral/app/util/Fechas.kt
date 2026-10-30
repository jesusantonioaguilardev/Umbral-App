package mx.umbral.app.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object Fechas {
    private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    val DIAS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val zona: TimeZone get() = TimeZone.currentSystemDefault()

    fun ahora(): Instant = Clock.System.now()
    fun hoy(): LocalDate = ahora().toLocalDateTime(zona).date
    fun diasDesdeHoy(n: Int): LocalDate = hoy().plus(n, DateTimeUnit.DAY)

    fun inicioDelDia(d: LocalDate): Instant = d.atStartOfDayIn(zona)
    fun en(d: LocalDate, hora: String): Instant =
        LocalDateTime(d, LocalTime.parse(normalizarHora(hora))).toInstant(zona)

    fun normalizarHora(h: String): String {
        val p = h.trim().split(":")
        val hh = p.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
        val mm = p.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return "${hh.toString().padStart(2, '0')}:${mm.toString().padStart(2, '0')}"
    }

    fun parse(iso: String): Instant? = runCatching { Instant.parse(iso) }.getOrNull()
        ?: runCatching { Instant.parse(iso.replace(" ", "T").let { if (it.length <= 19) it + "Z" else it }) }.getOrNull()

    fun hora(iso: String): String = parse(iso)?.toLocalDateTime(zona)?.let {
        "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
    } ?: iso

    fun fecha(iso: String): String {
        val d = if (iso.length == 10) runCatching { LocalDate.parse(iso) }.getOrNull()
        else parse(iso)?.toLocalDateTime(zona)?.date
        return d?.let { "${it.dayOfMonth} ${MESES[it.monthNumber - 1]} ${it.year}" } ?: iso
    }

    fun fechaHora(iso: String) = "${fecha(iso)} · ${hora(iso)}"

    fun etiquetaDia(d: LocalDate): String = when (d) {
        hoy() -> "Hoy"
        diasDesdeHoy(1) -> "Mañana"
        else -> "${DIAS[d.dayOfWeek.ordinal]} ${d.dayOfMonth} ${MESES[d.monthNumber - 1]}"
    }
}

fun Double.mxn(): String {
    val entero = kotlin.math.round(this).toLong()
    val s = kotlin.math.abs(entero).toString().reversed().chunked(3).joinToString(",").reversed()
    return (if (entero < 0) "-$" else "$") + s
}
