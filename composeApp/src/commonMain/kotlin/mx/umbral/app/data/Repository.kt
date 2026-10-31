package mx.umbral.app.data

/** Contrato de datos. Implementación: SupabaseRepository. */
interface Repository {
    val perfil: Perfil?

    suspend fun login(email: String, password: String): Perfil
    suspend fun restaurarSesion(): Perfil?
    suspend fun logout()

    // Notificaciones push
    suspend fun registrarDispositivo(token: String, plataforma: String)
    suspend fun quitarDispositivo(token: String)
    suspend fun notificaciones(): List<Notificacion>

    // Accesos
    suspend fun misPases(): List<Pase>
    suspend fun crearPase(
        visitante: String, tipo: TipoAcceso, recurrente: Boolean, dias: List<Int>?,
        horaInicio: String?, horaFin: String?, validoDesde: String, validoHasta: String,
    ): Pase
    suspend fun cancelarPase(id: String)
    suspend fun codigoPersonal(): CodigoPersonal
    suspend fun accesosDeMiUnidad(): List<Acceso>

    // Cuotas
    suspend fun miUnidad(): Unidad?
    suspend fun cargos(): List<Cargo>
    suspend fun reportarPago(cargo: Cargo, metodo: String, referencia: String?)

    // Comunidad
    suspend fun avisos(): List<Aviso>
    suspend fun paquetes(): List<Paquete>
    suspend fun tickets(): List<Ticket>
    suspend fun crearTicket(titulo: String, descripcion: String, categoria: String)
    suspend fun areas(): List<Area>
    suspend fun reservaciones(): List<Reservacion>
    suspend fun reservar(areaId: String, fecha: String, horaInicio: String, horaFin: String)
    suspend fun encuestas(): List<EncuestaConResultados>
    suspend fun votar(encuestaId: String, opcion: Int)

    // Caseta (rol guardia)
    suspend fun validarPase(codigo: String): Validacion
    suspend fun accesosHoy(): List<Acceso>
}
