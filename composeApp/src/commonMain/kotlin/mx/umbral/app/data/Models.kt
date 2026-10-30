package mx.umbral.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Rol { residente, guardia, admin }

@Serializable
data class Condominio(val id: String, val nombre: String, val ciudad: String? = null)

@Serializable
data class Perfil(
    val id: String,
    val nombre: String,
    val rol: Rol = Rol.residente,
    @SerialName("condominio_id") val condominioId: String? = null,
    @SerialName("unidad_id") val unidadId: String? = null,
    @SerialName("condominios") val condominio: Condominio? = null,
    @SerialName("unidades") val unidad: Unidad? = null,
)

@Serializable
data class Unidad(
    val id: String = "",
    val nombre: String,
    val torre: String? = null,
    val clabe: String? = null,
)

@Serializable
data class UnidadRef(val nombre: String)

enum class TipoAcceso(val etiqueta: String) {
    visita("Visita"), proveedor("Proveedor"), paqueteria("Paquetería"), airbnb("Huésped"), residente("Residente")
}

@Serializable
data class Pase(
    val id: String,
    val codigo: String,
    @SerialName("unidad_id") val unidadId: String,
    val visitante: String,
    val tipo: TipoAcceso = TipoAcceso.visita,
    val modalidad: String = "una_vez",
    val dias: List<Int>? = null,
    @SerialName("hora_inicio") val horaInicio: String? = null,
    @SerialName("hora_fin") val horaFin: String? = null,
    @SerialName("valido_desde") val validoDesde: String,
    @SerialName("valido_hasta") val validoHasta: String,
    @SerialName("usos_max") val usosMax: Int? = 1,
    val usos: Int = 0,
    val activo: Boolean = true,
)

/** Datos para crear un pase nuevo desde la app. */
@Serializable
data class NuevoPase(
    @SerialName("condominio_id") val condominioId: String,
    @SerialName("unidad_id") val unidadId: String,
    @SerialName("creado_por") val creadoPor: String,
    val visitante: String,
    val tipo: TipoAcceso,
    val modalidad: String,
    val dias: List<Int>? = null,
    @SerialName("hora_inicio") val horaInicio: String? = null,
    @SerialName("hora_fin") val horaFin: String? = null,
    @SerialName("valido_desde") val validoDesde: String,
    @SerialName("valido_hasta") val validoHasta: String,
    @SerialName("usos_max") val usosMax: Int? = 1,
)

@Serializable
data class CodigoPersonal(val codigo: String, val expira: String)

@Serializable
data class Validacion(
    val permitido: Boolean,
    val motivo: String? = null,
    val visitante: String? = null,
    val tipo: TipoAcceso? = null,
    val unidad: String? = null,
    val autorizo: String? = null,
)

@Serializable
data class Acceso(
    val id: Long,
    @SerialName("created_at") val createdAt: String,
    val nombre: String,
    val tipo: TipoAcceso,
    val permitido: Boolean,
    val motivo: String? = null,
    val autorizo: String? = null,
    @SerialName("unidades") val unidad: UnidadRef? = null,
)

@Serializable
data class Cargo(
    val id: String,
    val concepto: String,
    val extraordinaria: Boolean = false,
    val monto: Double,
    val vence: String,
    val estado: String, // pendiente | pagado | vencido
)

@Serializable
data class NuevoPago(
    @SerialName("condominio_id") val condominioId: String,
    @SerialName("cargo_id") val cargoId: String,
    @SerialName("unidad_id") val unidadId: String,
    val monto: Double,
    val metodo: String,
    val referencia: String?,
)

@Serializable
data class Aviso(
    val id: String,
    val titulo: String,
    val cuerpo: String? = null,
    val torre: String? = null,
    val importante: Boolean = false,
    @SerialName("adjunto_url") val adjuntoUrl: String? = null,
    @SerialName("adjunto_nombre") val adjuntoNombre: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class Paquete(
    val id: String,
    val paqueteria: String? = null,
    val descripcion: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("entregado_at") val entregadoAt: String? = null,
)

@Serializable
data class Ticket(
    val id: String,
    val titulo: String,
    val descripcion: String? = null,
    val estado: String = "abierto",
    val responsable: String? = null,
    val categoria: String = "Mantenimiento",
    val respuesta: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class NuevoTicket(
    @SerialName("condominio_id") val condominioId: String,
    @SerialName("unidad_id") val unidadId: String?,
    @SerialName("creado_por") val creadoPor: String,
    val titulo: String,
    val descripcion: String,
    val categoria: String,
)

/** Categorías de reporte que puede elegir el residente. */
val CATEGORIAS_TICKET = listOf("Mantenimiento", "Seguridad", "Limpieza", "Áreas comunes", "Ruido / vecinos", "Otro")

@Serializable
data class Area(
    val id: String,
    val nombre: String,
    val reglas: String? = null,
    val abre: String = "08:00",
    val cierra: String = "22:00",
)

@Serializable
data class Reservacion(
    val id: String,
    @SerialName("area_id") val areaId: String,
    @SerialName("unidad_id") val unidadId: String,
    val fecha: String,
    @SerialName("hora_inicio") val horaInicio: String,
    @SerialName("hora_fin") val horaFin: String,
)

@Serializable
data class NuevaReservacion(
    @SerialName("condominio_id") val condominioId: String,
    @SerialName("area_id") val areaId: String,
    @SerialName("unidad_id") val unidadId: String,
    val fecha: String,
    @SerialName("hora_inicio") val horaInicio: String,
    @SerialName("hora_fin") val horaFin: String,
)

@Serializable
data class Encuesta(
    val id: String,
    val pregunta: String,
    val opciones: List<String>,
    val cierra: String? = null,
)

@Serializable
data class Voto(
    @SerialName("encuesta_id") val encuestaId: String,
    @SerialName("unidad_id") val unidadId: String,
    val opcion: Int,
)

data class EncuestaConResultados(
    val encuesta: Encuesta,
    val votos: List<Int>,
    val miVoto: Int?,
) {
    val total get() = votos.sum()
}
