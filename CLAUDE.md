# CLAUDE.md · Umbral-App

App móvil de Umbral para residentes (y guardias en *modo caseta*): Kotlin Multiplatform + Compose Multiplatform,
targets Android, iOS y escritorio (`jvm("desktop")`). Repo hermano: `C:/Users/user/Desktop/Umbral-Web` (panel web y
**el esquema `supabase/schema.sql`**). Ambos comparten una sola base de datos Supabase.
Guía para personas: [README.md](README.md). Cambios: [CHANGELOG.md](CHANGELOG.md).

## Arquitectura

- Un solo módulo Gradle: `:composeApp`. Casi todo vive en `commonMain`; las plataformas solo aportan punto de entrada y
  `rememberCompartir()` (`expect`/`actual` en `platform/Compartir*.kt`).
- `App.kt` crea `SupabaseRepository` y enruta por rol: residente → `ResidenteHome` (pestañas Inicio, Accesos, Pagos,
  Comunidad, Perfil); guardia → `GuardiaScreen`.
- Datos: interfaz `data/Repository.kt` → implementación `data/SupabaseRepository.kt` (supabase-kt 3.0.3: auth + postgrest).
  Las pantallas solo hablan con `Repository`.
- Seguridad en la base de datos (RLS, triggers, RPC). La app usa solo la clave **anon**.
- Versiones en `gradle/libs.versions.toml` (Kotlin 2.1.0, Compose MP 1.7.3, kotlinx-datetime 0.6.1, Ktor 3.0.3, qrose 1.0.1).

## Rutas clave (`composeApp/src/commonMain/kotlin/mx/umbral/app/`)

| Ruta | Qué es |
|---|---|
| `Config.kt` | `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `URL_PASE_WEB`, `ZONA_HORARIA`, `MODULO_COMUNIDAD`, `MODULO_FINANZAS` |
| `data/Models.kt` | Modelos `@Serializable` con `@SerialName` en snake_case = columnas de Supabase |
| `data/Repository.kt` | Contrato de operaciones (sesión, pases, cuotas, comunidad, caseta) |
| `data/SupabaseRepository.kt` | Consultas a tablas y RPC `generar_codigo_personal`, `validar_pase`, `registrar_dispositivo`, `quitar_dispositivo` |
| `platform/Push.kt` (+ `.android/.ios/.desktop`) | `plataformaPush`, `tokenPush()`, `PedirPermisoNotificaciones()`, `TokensPush` |
| `androidMain/.../push/` | `UmbralMessagingService` (FCM) y `Notificaciones` (canal `umbral`) |
| `ui/Theme.kt` | `UmbralTheme`, esquemas claro/oscuro y `LocalColoresEstado` (`Verde`, `Rojo`, `Ambar` y sus `*Suave`) |
| `ui/Components.kt` | Componentes compartidos (`Tarjeta`, `Etiqueta*`, `Contenido`, `Vacio`, `cargar`…) |
| `ui/residente/*Tab.kt`, `ui/caseta/GuardiaScreen.kt` | Pantallas |
| `util/Fechas.kt` | Fechas en la zona del condominio (`Fechas.zona`, `hoy()`, `fecha()`, `hora()`) |
| `util/Url.kt` | `codificarUrl` para el enlace `wa.me` |

## Convenciones

- Textos de interfaz en español de México.
- Fechas y "hoy": siempre `Fechas` (usa `Config.ZONA_HORARIA`), nunca `TimeZone.currentSystemDefault()` directo.
- Colores de estado: `Verde`/`Rojo`/`Ambar` (son `@Composable`, cambian en modo oscuro) o `MaterialTheme.colorScheme`;
  no uses `Color(0xFF…)` fijos en pantallas.
- Errores de red visibles al usuario: `runCatching { … }.onFailure { error = … }`; no los ocultes.
- Operación nueva de datos: agrégala a `Repository` y a `SupabaseRepository`, y su modelo a `Models.kt`.
- Un cargo `pendiente` con `vence` pasado se muestra como vencido (`Cargo.estaVencido()` en `PagosTab.kt`), igual que el panel.

## Contratos entre piezas

- **Config:** `Config.kt` y `Umbral-Web/js/config.js` deben coincidir en `MODULO_COMUNIDAD`/`MODULO_FINANZAS` ↔ `MODULOS`
  y `ZONA_HORARIA`. `schema.sql` usa `America/Hermosillo` fija.
- **Pase público:** `URL_PASE_WEB` = `https://jesusantonioaguilardev.github.io/Umbral-Web/pase.html?c=`; la app le concatena el código.
  GitHub Pages publica `main` de Umbral-Web.
- **QR:** `UMBRAL:<código>` (ver `AccesosTab.kt`, `PerfilTab.kt`); `GuardiaScreen` quita el prefijo antes de `validar_pase`.
- **Pagos:** el residente inserta en `pagos`; el trigger fuerza `confirmado = false`. `cargos()` marca `enRevision` los cargos
  con un pago sin confirmar, que no se pueden volver a pagar.
- **Encuestas:** `cerrada` si `cierra < hoy`; la política `vt_ins` rechaza votos en encuestas cerradas.
- **RPC usadas:** App → `validar_pase`, `generar_codigo_personal`, `registrar_dispositivo`, `quitar_dispositivo`.
  Cambiar firma o forma del JSON exige revisar ambos repos.
- **Push:** `App.kt` registra el token con sesión abierta y lo quita *antes* de `logout()` (la RPC necesita sesión).
  `plataformaPush` debe ser `android|ios|web|desktop` (check de `dispositivos`). El canal `umbral` de `Notificaciones.kt` es el
  default de FCM en el manifest. `google-services.json` está en `.gitignore`: nunca lo agregues; sin él el plugin no se aplica.
- El esquema no se edita aquí. Si la app necesita una columna o RPC nueva, el cambio va en `Umbral-Web/supabase/schema.sql`
  (agente `umbral-supabase`) y se menciona en ambos CHANGELOG.

## Verificación

```bash
./gradlew :composeApp:compileKotlinDesktop   # compila commonMain + desktop (rápido)
./gradlew :composeApp:assembleDebug          # APK Android de depuración
./gradlew :composeApp:run                    # corre la app de escritorio
```

En Windows usa `gradlew.bat`. iOS solo compila en Mac (`:composeApp:embedAndSignAppleFrameworkForXcode` desde Xcode;
no hay `.xcodeproj` en el repo). No hay pruebas automáticas (`desktopTest`/`allTests` existen pero sin fuentes de prueba).

## Reglas

- **Nunca ejecutes SQL contra el proyecto de Supabase de producción.** El SQL lo corre el usuario en el SQL Editor.
- **Sin secretos:** nada de `service_role`, contraseñas ni correos reales en código, docs o commits. En docs usa `https://xxxx.supabase.co`.
- `schema.sql` (en Umbral-Web) es idempotente; cualquier cambio debe poder re-ejecutarse completo.
- Si un cambio requiere ejecutar `schema.sql`, márcalo en el CHANGELOG: **(requiere ejecutar `supabase/schema.sql` en Umbral-Web)**.
- No cambies de rama ni hagas commit/push sin que lo pidan.

## Skills (`~/.claude/skills`)

| Skill | Cuándo |
|---|---|
| `umbral-pantalla-compose` | Pantalla, pestaña o componente Compose, u operación nueva en `Repository` |
| `umbral-tokens-diseno` | Colores, tipografía, radios o modo oscuro en `ui/Theme.kt` |
| `umbral-revision-contratos` | Después de cambiar modelos, `Repository` o algo que dependa del esquema |
| `umbral-checklist-accesibilidad` | Diseñar o revisar pantallas |
| `umbral-migracion-idempotente`, `umbral-checklist-rls` | Si el cambio necesita tocar `schema.sql` (en Umbral-Web) |
| `umbral-changelog` | Registrar cambios en `CHANGELOG.md` |
