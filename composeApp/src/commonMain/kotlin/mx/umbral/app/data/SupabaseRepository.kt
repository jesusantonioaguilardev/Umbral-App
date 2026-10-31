package mx.umbral.app.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mx.umbral.app.util.Fechas

class SupabaseRepository(url: String, anonKey: String) : Repository {
    override var perfil: Perfil? = null
        private set

    private val client: SupabaseClient = createSupabaseClient(url, anonKey) {
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true; explicitNulls = false })
        install(Auth)
        install(Postgrest)
    }

    private val yo get() = perfil ?: error("Sesión no iniciada")
    private val condo get() = yo.condominioId ?: error("Tu usuario no tiene condominio asignado")
    private val unidadId get() = yo.unidadId ?: error("Tu usuario no tiene unidad asignada")

    private suspend fun cargarPerfil(): Perfil {
        val uid = client.auth.currentUserOrNull()?.id ?: error("Sin sesión")
        val p = client.from("profiles")
            .select(Columns.raw("*, condominios(*), unidades(*)")) { filter { eq("id", uid) } }
            .decodeSingle<Perfil>()
        perfil = p
        return p
    }

    override suspend fun login(email: String, password: String): Perfil {
        client.auth.signInWith(Email) { this.email = email.trim(); this.password = password }
        return cargarPerfil()
    }

    override suspend fun restaurarSesion(): Perfil? {
        val status = client.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        return if (status is SessionStatus.Authenticated) runCatching { cargarPerfil() }.getOrNull() else null
    }

    override suspend fun logout() { client.auth.signOut(); perfil = null }

    // ---------------- Notificaciones push ----------------
    override suspend fun registrarDispositivo(token: String, plataforma: String) {
        client.postgrest.rpc("registrar_dispositivo", buildJsonObject {
            put("p_token", token); put("p_plataforma", plataforma)
        })
    }

    override suspend fun quitarDispositivo(token: String) {
        client.postgrest.rpc("quitar_dispositivo", buildJsonObject { put("p_token", token) })
    }

    // RLS deja ver solo las propias
    override suspend fun notificaciones(): List<Notificacion> = client.from("notificaciones").select {
        order("created_at", Order.DESCENDING); limit(100)
    }.decodeList()

    // ---------------- Accesos ----------------
    override suspend fun misPases(): List<Pase> = client.from("pases").select {
        filter { eq("unidad_id", unidadId); neq("tipo", "residente") }
        order("created_at", Order.DESCENDING)
        limit(100)
    }.decodeList()

    override suspend fun crearPase(
        visitante: String, tipo: TipoAcceso, recurrente: Boolean, dias: List<Int>?,
        horaInicio: String?, horaFin: String?, validoDesde: String, validoHasta: String,
    ): Pase = client.from("pases").insert(
        NuevoPase(condo, unidadId, yo.id, visitante, tipo, if (recurrente) "recurrente" else "una_vez",
            dias, horaInicio, horaFin, validoDesde, validoHasta, if (recurrente) null else 1)
    ) { select() }.decodeSingle()

    override suspend fun cancelarPase(id: String) {
        client.from("pases").update({ set("activo", false) }) { filter { eq("id", id) } }
    }

    override suspend fun codigoPersonal(): CodigoPersonal =
        client.postgrest.rpc("generar_codigo_personal").decodeAs()

    override suspend fun accesosDeMiUnidad(): List<Acceso> = client.from("accesos")
        .select(Columns.raw("*, unidades(nombre)")) {
            filter { eq("unidad_id", unidadId) }
            order("created_at", Order.DESCENDING); limit(50)
        }.decodeList()

    // Botón de emergencia: inserta en alertas; el trigger de la base manda el push
    override suspend fun enviarAlerta(mensaje: String) {
        client.from("alertas").insert(NuevaAlerta(condo, mensaje.trim(), yo.id))
    }

    // ---------------- Cuotas ----------------
    override suspend fun miUnidad(): Unidad? = yo.unidad

    override suspend fun cargos(): List<Cargo> {
        val cargos = client.from("cargos").select {
            filter { eq("unidad_id", unidadId) }
            order("vence", Order.DESCENDING); limit(48)
        }.decodeList<Cargo>()
        val enRevision = client.from("pagos").select {
            filter { eq("unidad_id", unidadId); eq("confirmado", false) }
        }.decodeList<PagoReportado>().mapNotNull { it.cargoId }.toSet()
        return cargos.map { if (it.id in enRevision && it.estado != "pagado") it.copy(enRevision = true) else it }
    }

    /** El residente reporta su transferencia con la clave de rastreo.
     *  En producción conviene confirmarla con un webhook del proveedor SPEI / CEP de Banxico. */
    override suspend fun reportarPago(cargo: Cargo, metodo: String, referencia: String?) {
        client.from("pagos").insert(NuevoPago(condo, cargo.id, unidadId, cargo.monto, metodo, referencia))
    }

    // ---------------- Comunidad ----------------
    override suspend fun avisos(): List<Aviso> = client.from("avisos").select {
        order("created_at", Order.DESCENDING); limit(50)
    }.decodeList<Aviso>().filter { it.torre == null || it.torre == yo.unidad?.torre }

    override suspend fun paquetes(): List<Paquete> = client.from("paquetes").select {
        filter { eq("unidad_id", unidadId) }; order("created_at", Order.DESCENDING); limit(50)
    }.decodeList()

    override suspend fun tickets(): List<Ticket> = client.from("tickets").select {
        filter { eq("unidad_id", unidadId) }; order("created_at", Order.DESCENDING)
    }.decodeList()

    override suspend fun crearTicket(titulo: String, descripcion: String, categoria: String) {
        client.from("tickets").insert(NuevoTicket(condo, yo.unidadId, yo.id, titulo, descripcion, categoria))
    }

    override suspend fun areas(): List<Area> = client.from("areas").select().decodeList()

    override suspend fun reservaciones(): List<Reservacion> = client.from("reservaciones").select {
        filter { gte("fecha", Fechas.hoy().toString()) }; order("fecha", Order.ASCENDING)
    }.decodeList()

    override suspend fun reservar(areaId: String, fecha: String, horaInicio: String, horaFin: String) {
        client.from("reservaciones").insert(NuevaReservacion(condo, areaId, unidadId, fecha, horaInicio, horaFin))
    }

    override suspend fun encuestas(): List<EncuestaConResultados> {
        val es = client.from("encuestas").select { order("created_at", Order.DESCENDING) }.decodeList<Encuesta>()
        if (es.isEmpty()) return emptyList()
        val vs = client.from("votos").select { filter { isIn("encuesta_id", es.map { it.id }) } }.decodeList<Voto>()
        return es.map { e ->
            val mios = vs.filter { it.encuestaId == e.id }
            EncuestaConResultados(e, e.opciones.indices.map { i -> mios.count { it.opcion == i } }, mios.firstOrNull { it.unidadId == yo.unidadId }?.opcion,
                cerrada = e.cierra != null && e.cierra.take(10) < Fechas.hoy().toString())
        }
    }

    override suspend fun votar(encuestaId: String, opcion: Int) {
        client.from("votos").insert(Voto(encuestaId, unidadId, opcion))
    }

    // ---------------- Caseta ----------------
    override suspend fun validarPase(codigo: String): Validacion =
        client.postgrest.rpc("validar_pase", buildJsonObject {
            put("p_codigo", codigo.trim().uppercase()); put("p_punto", "App móvil caseta")
        }).decodeAs()

    override suspend fun accesosHoy(): List<Acceso> = client.from("accesos")
        .select(Columns.raw("*, unidades(nombre)")) {
            filter { gte("created_at", Fechas.inicioDelDia(Fechas.hoy()).toString()) }
            order("created_at", Order.DESCENDING); limit(200)
        }.decodeList()
}
