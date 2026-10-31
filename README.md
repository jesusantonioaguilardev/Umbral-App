# Umbral · App móvil

Umbral es una plataforma para condominios: control de accesos con QR, cuotas y cobranza, comunidad y reportes.
Este repositorio contiene la **app para residentes** (y guardias en *modo caseta*), hecha con
Kotlin Multiplatform y Compose Multiplatform para **Android, iOS y escritorio**.

| Repositorio | Contenido | Para quién |
|---|---|---|
| [Umbral-App](https://github.com/jesusantonioaguilardev/Umbral-App) (este) | App Kotlin Multiplatform + Compose | Residentes (y guardias en *modo caseta*) |
| [Umbral-Web](https://github.com/jesusantonioaguilardev/Umbral-Web) | Panel web (caseta y administración), `pase.html` y el esquema `supabase/` | Guardia, administración y la visita que recibe el pase |

Los dos usan **la misma base de datos Supabase**. El esquema (`supabase/schema.sql`), la instalación de la base de datos
y la referencia de funciones SQL están en Umbral-Web:

- [Instalar la base de datos](https://github.com/jesusantonioaguilardev/Umbral-Web#1-instalar-la-base-de-datos)
- [Referencia de funciones RPC, triggers y RLS](https://github.com/jesusantonioaguilardev/Umbral-Web#referencia-de-la-base-de-datos)

Contenido: [Requisitos](#requisitos) · [1. Configurar](#1-configurar-configkt) · [2. Compilar y correr](#2-compilar-y-correr) ·
[Qué hace la app](#qué-hace-la-app) · [Estructura](#estructura) · [Pendientes](#pendientes-para-producción)

---

## Requisitos

- JDK 17 o más reciente (el proyecto compila a JVM 17).
- Android Studio con el plugin *Kotlin Multiplatform* y el SDK de Android 35 (mínimo soportado: Android 7.0, API 24).
- Para iOS: una Mac con Xcode.
- Un proyecto de Supabase con `schema.sql` ya ejecutado (ver Umbral-Web).

## 1. Configurar `Config.kt`

Edita `composeApp/src/commonMain/kotlin/mx/umbral/app/Config.kt`:

```kotlin
object Config {
    const val SUPABASE_URL = "https://xxxx.supabase.co"
    const val SUPABASE_ANON_KEY = "<clave pública anon>"
    const val URL_PASE_WEB = "https://jesusantonioaguilardev.github.io/Umbral-Web/pase.html?c="
    const val ZONA_HORARIA = "America/Hermosillo"
    const val MODULO_COMUNIDAD = true
    const val MODULO_FINANZAS = false
}
```

| Constante | Qué es |
|---|---|
| `SUPABASE_URL`, `SUPABASE_ANON_KEY` | *Project Settings → API* de Supabase. Usa solo la clave **anon**; nunca la `service_role` |
| `URL_PASE_WEB` | Prefijo del enlace que el residente manda a su visita. Apunta a `pase.html` del panel publicado en GitHub Pages; cámbialo si usas otro dominio |
| `ZONA_HORARIA` | Zona del condominio para "hoy", horarios y vigencias, en lugar de la zona del teléfono |
| `MODULO_COMUNIDAD`, `MODULO_FINANZAS` | Muestran u ocultan las pestañas Comunidad y Pagos. Control de accesos siempre está activo |

Sin URL y clave, la app muestra una pantalla pidiendo configurarla.

### Debe coincidir con el panel web

`Config.kt` y `js/config.js` de [Umbral-Web](https://github.com/jesusantonioaguilardev/Umbral-Web) deben tener los mismos
valores en el proyecto de Supabase, los módulos y la zona horaria:

| App (`Config.kt`) | Panel (`js/config.js`) |
|---|---|
| `SUPABASE_URL`, `SUPABASE_ANON_KEY` | `SUPABASE_URL`, `SUPABASE_ANON_KEY` |
| `MODULO_COMUNIDAD` | `MODULOS.comunidad` |
| `MODULO_FINANZAS` | `MODULOS.finanzas` |
| `ZONA_HORARIA` | `ZONA_HORARIA` |

Si no coinciden, la app y el panel muestran secciones distintas o calculan "hoy" en días distintos. Las funciones SQL usan
`America/Hermosillo` fija, así que la zona también debe coincidir con `schema.sql`.

## 2. Compilar y correr

Todos los comandos se ejecutan en la raíz del repositorio (en Windows usa `gradlew.bat` o `.\gradlew`).

### Escritorio (prueba rápida, sin emulador)

```bash
./gradlew :composeApp:run
```

### Android

Abre la carpeta del repositorio en Android Studio y ejecuta la configuración `composeApp`, o desde la terminal:

```bash
./gradlew :composeApp:assembleDebug     # genera el APK de depuración
./gradlew :composeApp:installDebug      # lo instala en el dispositivo o emulador conectado
```

El APK queda en `composeApp/build/outputs/apk/debug/`.

### iOS (en Mac con Xcode)

El repositorio trae el código Swift (`iosApp/iosApp/iOSApp.swift` y `ContentView.swift`) pero **no** el proyecto de Xcode
(`.xcodeproj`). Para crearlo:

1. En Xcode crea un proyecto de app iOS dentro de `iosApp/` (o genera uno con el asistente de kmp.jetbrains.com)
   y usa los archivos de `iosApp/iosApp/`.
2. Agrega una *Run Script Phase* antes de *Compile Sources*:
   ```bash
   cd "$SRCROOT/.." && ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
   ```
3. En *Framework Search Paths* agrega `$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.
4. Ejecuta en el simulador o en un dispositivo.

El framework se llama `ComposeApp` (estático) y se genera para `iosArm64`, `iosX64` e `iosSimulatorArm64`.

### Verificar que compila

```bash
./gradlew :composeApp:compileKotlinDesktop
./gradlew :composeApp:assembleDebug
```

## Qué hace la app

Al iniciar sesión, la pantalla depende del rol (`App.kt`):

- **Residente**
  - **Inicio:** saldo, paquetes, avisos y últimos accesos.
  - **Accesos:** crear pases de una vez o recurrentes, ver el QR, **enviar por WhatsApp** o compartir por otra app, y cancelar.
  - **Pagos** (si `MODULO_FINANZAS`): estado de cuenta, CLABE para SPEI y reportar pago.
  - **Comunidad** (si `MODULO_COMUNIDAD`): avisos, paquetes, tickets, reservaciones y encuestas.
  - **Perfil:** código personal dinámico que dura 30 segundos.
- **Guardia** → *modo caseta*: validar un código tecleado y ver la bitácora del día.
  El escaneo con cámara está en el panel web de caseta.

### Por qué un cargo aparece "En revisión"

Cuando el residente reporta un pago, la base de datos lo guarda **por confirmar** (`pagos.confirmado = false`), porque solo
se tiene la clave de rastreo y aún no hay conciliación SPEI automática. Mientras administración no lo confirma en el panel,
el cargo se muestra **En revisión** y no se puede volver a pagar; al confirmarlo pasa a **Pagado**.
Un cargo `pendiente` con fecha de vencimiento ya pasada se muestra como **Vencido** aunque el cron del servidor aún no lo haya marcado.

### El pase por WhatsApp

El botón **Enviar por WhatsApp** abre `https://wa.me/?text=…` con un mensaje para la visita: nombre, vigencia en palabras,
código, el enlace `URL_PASE_WEB + código` y el aviso de mostrar el QR en caseta. Si no se puede abrir WhatsApp, cae a la hoja
nativa de compartir. La visita abre el enlace en el navegador y ve su QR sin instalar nada.

## Estructura

```
composeApp/src/
  commonMain/kotlin/mx/umbral/app/
    App.kt                ← elige pantalla según el rol
    Config.kt             ← Supabase, URL del pase, zona horaria y módulos
    data/                 ← Models.kt, Repository.kt (interfaz), SupabaseRepository.kt
    ui/                   ← Theme.kt, Components.kt, login/, residente/, caseta/
    util/Fechas.kt        ← fechas en la zona del condominio
    util/Url.kt           ← codificación de URL para wa.me
    platform/Compartir.kt ← expect de la hoja de compartir
  androidMain/            ← MainActivity y "compartir" de Android
  iosMain/                ← MainViewController y "compartir" de iOS
  desktopMain/            ← main.kt y "compartir" de escritorio
iosApp/iosApp/            ← código Swift de arranque para iOS
gradle/libs.versions.toml ← versiones (Kotlin, Compose, Supabase, Ktor…)
```

## Pendientes para producción

- **Pago con tarjeta:** el botón *Pagar con tarjeta* hoy solo llama a `reportarPago(cargo, "tarjeta", …)`, que queda por confirmar.
  Falta integrar una pasarela (Stripe, Conekta, Openpay…) antes de esa llamada.
- **Conciliación SPEI automática** (ver Umbral-Web).
- **Notificaciones push** (paquete recibido, alerta de emergencia).
- **Escaneo con cámara en la app:** hoy el guardia teclea el código.
- **Proyecto de Xcode** versionado en `iosApp/`.

Historial de cambios: [CHANGELOG.md](CHANGELOG.md). Contexto para agentes: [CLAUDE.md](CLAUDE.md).
