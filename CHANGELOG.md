# Changelog

Cambios importantes de este proyecto. Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [Sin publicar]

### Agregado
- Botón **Enviar por WhatsApp** en el pase: abre WhatsApp con un mensaje listo para la visita (nombre, vigencia en palabras,
  código, enlace al pase y aviso de mostrar el QR en caseta). Si WhatsApp no abre, usa la hoja de compartir del teléfono;
  también queda el botón *Compartir por otra app*.
- El saldo de *Pagos* avisa cuántos cargos están vencidos y por cuánto.
- Modo oscuro completo: los colores de estado (verde, rojo, ámbar) tienen versión oscura y el resultado de la caseta ya no usa
  colores fijos, para que se lea bien con el tema oscuro del sistema.
- `Config.ZONA_HORARIA`: la zona del condominio, que debe coincidir con `ZONA_HORARIA` de `js/config.js` en Umbral-Web.
- `CLAUDE.md` con el contexto del repositorio para los agentes.

### Cambiado
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
