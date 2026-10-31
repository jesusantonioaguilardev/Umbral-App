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
[3. Notificaciones push](#3-notificaciones-push-android) · [4. Escáner de QR](#4-escáner-de-qr-en-la-caseta-android) ·
[5. Botón de emergencia](#5-botón-de-emergencia) · [Qué hace la app](#qué-hace-la-app) · [Estructura](#estructura) · [Pendientes](#pendientes-para-producción)

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

## 3. Notificaciones push (Android)

El residente recibe un push cuando entra su visita (o un proveedor, paquetería o Airbnb de su unidad), cuando caseta recibe un
paquete para su unidad y cuando administración publica un aviso. La app solo registra el teléfono; quien arma y envía cada
notificación es la base de datos (triggers) y la Edge Function `notificar` de Umbral-Web.

Para activarlo en Android:

1. En la [consola de Firebase](https://console.firebase.google.com) crea un proyecto para Umbral (o usa uno que ya tengas).
2. En ese proyecto, **Agregar app → Android**, con el nombre de paquete `mx.umbral.app` (el `applicationId` de
   `composeApp/build.gradle.kts`). Si el paquete no coincide, Firebase no entrega los mensajes.
3. Descarga `google-services.json` y cópialo a `composeApp/google-services.json`.
   - Está en `.gitignore`: no lo subas al repositorio; cada quien descarga el suyo.
   - Sin ese archivo la app **compila y funciona igual**, pero sin push: `composeApp/build.gradle.kts` solo aplica el plugin
     `com.google.gms.google-services` si el archivo existe, y la app escribe en logcat (etiqueta `UmbralPush`)
     *"Firebase no está configurado (falta google-services.json)"*.
4. Recompila e instala:
   ```bash
   ./gradlew :composeApp:installDebug
   ```
5. Configura la parte del servidor (tablas, Edge Function `notificar`, secretos y Database Webhook) siguiendo
   [Umbral-Web/supabase/functions/notificar/README.md](https://github.com/jesusantonioaguilardev/Umbral-Web/blob/main/supabase/functions/notificar/README.md).
   Necesita el `supabase/schema.sql` actualizado de Umbral-Web.

Cómo se comporta:

- Al iniciar sesión, la app registra el token del teléfono con la RPC `registrar_dispositivo`. Al cerrar sesión lo quita con
  `quitar_dispositivo` **antes** de cerrar la sesión, porque la RPC solo deja quitar tokens propios y necesita al usuario
  autenticado; así el siguiente usuario del mismo teléfono no recibe avisos ajenos.
- En Android 13 o más reciente, la app pide el permiso de notificaciones (`POST_NOTIFICATIONS`) al entrar como **residente**.
  Si lo niega, la app sigue funcionando sin avisos. A guardias no se les pide todavía, aunque ahora también reciben las
  alertas de emergencia (ver [Pendientes](#pendientes-para-producción)).
- La app crea dos canales al abrirse, para que existan aunque el push llegue con la app cerrada:

  | Canal (id) | Para qué | Cómo avisa |
  |---|---|---|
  | **Umbral** (`umbral`) | Visitas, paquetes y avisos. Es el canal por defecto de FCM en el manifest | Importancia alta |
  | **Emergencias** (`emergencias`) | Push con `tipo = "alerta"` (alertas de emergencia) | Importancia alta, vibración larga, visible en pantalla bloqueada |

  Son dos canales para que el usuario pueda silenciar los avisos normales en *Ajustes → Notificaciones* sin perder las
  emergencias. Con la app en segundo plano, Android muestra el push tal como lo manda el servidor; por eso la Edge Function
  `notificar` de Umbral-Web manda `channel_id: "emergencias"` en las alertas. Si la función no está redesplegada con esa
  versión, las alertas llegan por el canal *Umbral*.
- iOS todavía no recibe push: el código Kotlin está listo, pero falta la parte nativa en Xcode
  (ver [iosApp/PUSH_IOS.md](iosApp/PUSH_IOS.md)). Escritorio no recibe push.

## 4. Escáner de QR en la caseta (Android)

En *modo caseta*, si el dispositivo tiene cámara, el botón principal es **Escanear QR**: abre la cámara a pantalla completa con
un marco guía, vibra al leer el pase, quita el prefijo `UMBRAL:` y valida con `validar_pase` igual que al escribir el código.
**Escribir código** queda como opción secundaria.

- **Requisitos:** Android con cámara (`android.hardware.camera.any`, declarada como no obligatoria para que la app se instale
  también en equipos sin cámara). Usa CameraX y ML Kit *barcode-scanning* con el modelo incluido en la app, así que no depende
  de Google Play Services ni descarga el modelo. Versiones en `gradle/libs.versions.toml` (`camerax`, `mlkit-barcode`).
- **Permisos:** `CAMERA` (se pide al abrir el escáner, no al instalar) y `VIBRATE`. Si el guardia niega la cámara, el escáner
  explica cómo activarla con **Abrir ajustes** y deja **Escribir código**; al volver de Ajustes revisa otra vez el permiso.
- **Tableta:** con más de 600 dp de ancho, la caseta muestra el resultado y la bitácora del día en dos columnas.
- **Escritorio:** no hay escáner (`escanerQrDisponible()` devuelve `false`); se escribe el código o se usa un lector QR tipo
  teclado, que escribe en el campo.
- **iOS: pendiente.** El botón no aparece. Falta implementarlo con AVFoundation (`AVCaptureSession` +
  `AVCaptureMetadataOutput` con tipo QR) y agregar `NSCameraUsageDescription` al `Info.plist`; solo se puede compilar y probar
  en Mac (ver `composeApp/src/iosMain/kotlin/mx/umbral/app/platform/EscanerQr.ios.kt`).

El panel web de caseta también escanea con la cámara del navegador (ver Umbral-Web).

## 5. Botón de emergencia

En el inicio del residente (*Accesos*), arriba a la derecha, está **Emergencia**:

1. Mantenlo presionado **2 segundos** (se llena de rojo como barra de progreso). Si lo sueltas antes, no se envía nada y aparece
   la ayuda *"Mantén presionado 2 segundos para pedir ayuda"*. Con lector de pantalla se activa con doble toque.
2. Se abre una hoja con un mensaje editable (*"Necesito ayuda en mi domicilio"*). Pulsa **Enviar alerta**.
3. La app inserta en la tabla `alertas` con `creado_por` = el residente (`Repository.enviarAlerta`). Si falla, el error se
   muestra en la hoja y sugiere llamar a caseta.

La app no manda el push: lo hace el trigger `notificar_alerta` de la base de datos, que avisa a **todo el condominio** con la app
y sesión abierta (residentes, caseta y administración), menos a quien la envió, con la unidad al inicio del mensaje
(*"Casa 14: …"*). El panel web muestra la alerta arriba hasta que caseta o administración la marcan atendida.

**Por qué hay que mantenerlo presionado y por qué hay límite:** una alerta le llega a todo el condominio, así que un toque
accidental en el bolsillo molestaría a todos. Por eso se mantiene 2 segundos y, además, la base de datos (`limitar_alertas`) deja
a cada residente enviar como máximo 2 alertas cada 10 minutos (*"Ya enviaste una alerta hace poco; tu alerta ya llegó a caseta y al condominio"*).
La política `al_ins` rechaza la alerta si `creado_por` no es quien tiene la sesión.

Necesita el `supabase/schema.sql` actualizado de Umbral-Web y, para el canal *Emergencias* en segundo plano, la Edge Function
`notificar` redesplegada.

## Qué hace la app

Al iniciar sesión, la pantalla depende del rol (`App.kt`):

- **Residente** (barra inferior de 4 secciones, `ResidenteHome.kt`)
  - **Accesos** (inicio): botón de **emergencia**, código personal dinámico que dura 30 segundos, crear pases de una vez o
    recurrentes, ver el QR, **enviar por WhatsApp** o compartir por otra app, y cancelar.
  - **Comunidad** (si `MODULO_COMUNIDAD`): avisos, paquetes, tickets, reservaciones y encuestas.
  - **Pagos** (si `MODULO_FINANZAS`): estado de cuenta, CLABE para SPEI y reportar pago.
  - **Perfil:** datos de la cuenta y cerrar sesión.
- **Guardia** → *modo caseta*: **escanear el QR** con la cámara (Android) o escribir el código, resultado grande y bitácora del día.
- **Admin** → se le indica que use el panel web.

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
    ui/residente/Emergencia.kt ← botón de emergencia (mantener 2 s) y hoja para enviar la alerta
    ui/caseta/GuardiaScreen.kt ← modo caseta (escanear o escribir código, bitácora)
    util/Fechas.kt        ← fechas en la zona del condominio
    util/Url.kt           ← codificación de URL para wa.me
    platform/Compartir.kt ← expect de la hoja de compartir
    platform/Push.kt      ← expect del token y el permiso de push
    platform/EscanerQr.kt ← expect del escáner QR (escanerQrDisponible, EscanerQr)
  androidMain/            ← MainActivity, "compartir", escáner (CameraX + ML Kit) y push (push/: servicio FCM y canales)
  iosMain/                ← MainViewController, "compartir", puente de push y escáner vacío (pendiente)
  desktopMain/            ← main.kt, "compartir", push y escáner vacíos de escritorio
composeApp/google-services.json ← lo pones tú (Firebase); no se versiona
iosApp/iosApp/            ← código Swift de arranque para iOS
iosApp/PUSH_IOS.md        ← pasos pendientes para push en iOS
gradle/libs.versions.toml ← versiones (Kotlin, Compose, Supabase, Ktor…)
```

## Pendientes para producción

- **Pago con tarjeta:** el botón *Pagar con tarjeta* hoy solo llama a `reportarPago(cargo, "tarjeta", …)`, que queda por confirmar.
  Falta integrar una pasarela (Stripe, Conekta, Openpay…) antes de esa llamada.
- **Conciliación SPEI automática** (ver Umbral-Web).
- **Push en iOS:** falta la parte nativa en Xcode (Firebase, llave APNs y puente Swift); pasos en [iosApp/PUSH_IOS.md](iosApp/PUSH_IOS.md).
- **Escáner de QR en iOS:** falta la implementación con AVFoundation y `NSCameraUsageDescription` (ver [sección 4](#4-escáner-de-qr-en-la-caseta-android)).
- **Proyecto de Xcode** versionado en `iosApp/`.

Historial de cambios: [CHANGELOG.md](CHANGELOG.md). Contexto para agentes: [CLAUDE.md](CLAUDE.md).
