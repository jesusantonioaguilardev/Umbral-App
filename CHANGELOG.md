# Changelog

Cambios importantes de este proyecto. Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [Sin publicar]

### Agregado
- **Notificaciones push en Android:** el residente recibe un aviso en el teléfono cuando entra su visita (o un proveedor,
  paquetería o Airbnb de su unidad), cuando caseta recibe un paquete para su unidad y cuando administración publica un aviso
  (todos los residentes, o solo los de la torre del aviso). Al iniciar sesión la app registra el teléfono con
  `registrar_dispositivo` y al cerrar sesión lo quita con `quitar_dispositivo`, para que el siguiente usuario del mismo teléfono
  no reciba avisos ajenos. En iOS y escritorio todavía no llegan (iOS: ver `iosApp/PUSH_IOS.md`).
  Ver también Umbral-Web (tablas, triggers y Edge Function `notificar`). **(requiere ejecutar `supabase/schema.sql` en Umbral-Web)**
- En Android 13 o más reciente, la app pide al residente el permiso de notificaciones (`POST_NOTIFICATIONS`) al entrar;
  si lo niega, la app funciona igual pero sin avisos. A guardias y administradores no se les pide.
- Para activar el push hace falta poner `composeApp/google-services.json` (se descarga de Firebase y no se versiona).
  Sin ese archivo la app compila y funciona, pero sin notificaciones.
- Botón **Enviar por WhatsApp** en el pase: abre WhatsApp con un mensaje listo para la visita (nombre, vigencia en palabras,
  código, enlace al pase y aviso de mostrar el QR en caseta). Si WhatsApp no abre, usa la hoja de compartir del teléfono;
  también queda el botón *Compartir por otra app*.
- El saldo de *Pagos* avisa cuántos cargos están vencidos y por cuánto.
- Modo oscuro completo: los colores de estado (verde, rojo, ámbar) tienen versión oscura y el resultado de la caseta ya no usa
  colores fijos, para que se lea bien con el tema oscuro del sistema.
- `Config.ZONA_HORARIA`: la zona del condominio, que debe coincidir con `ZONA_HORARIA` de `js/config.js` en Umbral-Web.
- `CLAUDE.md` con el contexto del repositorio para los agentes.

### Cambiado
- **Rediseño minimalista** de todas las pantallas: paleta sobria con un solo acento, sin sombras, listas agrupadas en una tarjeta y
  estados siempre con texto además de color. El inicio del residente ahora es Accesos (código personal, "Crear pase para visita",
  pases activos y pendientes); el pase creado se abre a pantalla completa; la caseta muestra un resultado grande con
  "Levanta la pluma" o "No levantes la pluma". Barra inferior de 4 secciones: Accesos, Comunidad, Pagos y Perfil. Ver también Umbral-Web.
- Un cargo con un pago reportado que administración aún no confirma se muestra **En revisión** y no se puede volver a pagar,
  para evitar pagos duplicados mientras se verifica la transferencia. Necesita el cambio de esquema de Umbral-Web
  (`pagos.confirmado`). **(requiere ejecutar `supabase/schema.sql` en Umbral-Web)**
- Un cargo pendiente cuya fecha de vencimiento ya pasó se muestra como **Vencido**, igual que en el panel web (ver también Umbral-Web).
- Fechas, horas, "hoy" y vigencias se calculan en la zona del condominio y no en la del teléfono, para que el residente vea los
  mismos días y horarios que caseta y administración. Si el teléfono no conoce la zona, se usa la del sistema. Ver también Umbral-Web.
- El enlace del pase que se manda a la visita apunta a `pase.html` del panel publicado en GitHub Pages
  (`https://jesusantonioaguilardev.github.io/Umbral-Web/pase.html?c=<código>`); antes apuntaba a un dominio de ejemplo y no abría.
- Las encuestas cerradas muestran los resultados en lugar de los botones para votar, y dicen *"cerró"* con la fecha.
  Va con la regla del esquema que rechaza votos en encuestas cerradas. **(requiere ejecutar `supabase/schema.sql` en Umbral-Web)**
- El README describe dos repositorios separados (antes hablaba de un monorepo con `mobile/` y `web/`), cómo configurar
  `Config.kt` y cómo compilar para escritorio, Android e iOS.

### Corregido
- Si falla cancelar un pase, la app muestra el error en lugar de cerrarse como si se hubiera cancelado.
- Si falla registrar un voto, la app muestra el error en lugar de ignorarlo.
