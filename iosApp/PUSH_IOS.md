# Notificaciones push en iOS (pendiente, requiere Mac)

En Android ya funcionan con Firebase Cloud Messaging. En iOS el código Kotlin está listo
(`composeApp/src/iosMain/.../platform/Push.ios.kt`): `tokenPush()` devuelve el último token que
Swift entregue con `recibirTokenPush(token)`, y `App.kt` lo registra con la RPC
`registrar_dispositivo(p_token, 'ios')`. Falta la parte nativa, que solo se hace en Xcode.

## 1. Requisitos
- Mac con Xcode 15 o superior.
- Cuenta de Apple Developer de pago (APNs no funciona con cuenta gratuita).
- iPhone físico: el simulador no recibe push remotos de APNs de forma confiable.

## 2. App de iOS en Firebase
1. Consola de Firebase → proyecto de Umbral → **Agregar app → iOS**.
2. Bundle ID: el mismo que tiene el target `iosApp` en Xcode (Signing & Capabilities). Sugerido `mx.umbral.app`.
3. Descarga `GoogleService-Info.plist` y arrástralo a `iosApp/iosApp/` en Xcode, con el target `iosApp` marcado.
   Ya está en `.gitignore`: no lo subas al repo.

## 3. Llave APNs
1. developer.apple.com → Certificates, IDs & Profiles → **Keys → +** → marca **Apple Push Notifications service (APNs)**.
2. Descarga el `.p8` (solo se puede una vez) y anota el **Key ID** y tu **Team ID**.
3. Consola de Firebase → Configuración del proyecto → **Cloud Messaging** → app de iOS →
   **Clave de autenticación de APNs → Subir**: el `.p8`, Key ID y Team ID.

## 4. Capacidades en Xcode
Target `iosApp` → **Signing & Capabilities → + Capability**:
- **Push Notifications**
- **Background Modes** → marca **Remote notifications**

## 5. SDK de Firebase por Swift Package Manager
1. Xcode → **File → Add Package Dependencies…**
2. URL: `https://github.com/firebase/firebase-ios-sdk` (regla: Up to Next Major, versión vigente).
3. Agrega solo el producto **FirebaseMessaging** al target `iosApp`.

## 6. Código Swift (puente Swift→Kotlin)
Reemplaza `iosApp/iosApp/iOSApp.swift` por:

```swift
import SwiftUI
import UIKit
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
        application.registerForRemoteNotifications()
        return true
    }

    // APNs entrega su token; Firebase lo cambia por el token de FCM.
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    // Token de FCM nuevo o renovado → Kotlin (lo registra App.kt si hay sesión).
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        Push_iosKt.recibirTokenPush(token: token)
    }

    // Mostrar la notificación también con la app en primer plano.
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .list])
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView().ignoresSafeArea(.keyboard)
        }
    }
}
```

Notas:
- `Push_iosKt` es el nombre que Kotlin/Native da a las funciones de nivel superior de
  `composeApp/src/iosMain/kotlin/mx/umbral/app/platform/Push.ios.kt`: toma el nombre del archivo, cambia el punto por `_` y
  agrega `Kt` (por eso no es `PushIosKt`). Si renombras el archivo, cambia también esta llamada. Si Xcode no lo encuentra,
  busca `recibirTokenPush` en el header generado `ComposeApp.h` del framework.
- Si `Info.plist` tiene `FirebaseAppDelegateProxyEnabled = NO`, hay que pasar a mano el token de APNs
  (como arriba) y los mensajes a `Messaging.messaging().appDidReceiveMessage(_:)`. Con el valor por
  defecto (sí) no hace falta.

## 7. Probar
1. Compila en un iPhone físico, inicia sesión como residente y acepta el permiso.
2. En Supabase revisa que exista una fila en `dispositivos` con `plataforma = 'ios'`.
3. Consola de Firebase → **Messaging → Nueva campaña → Mensaje de prueba**, pegando el token
   (aparece en la fila de `dispositivos`), o dispara un acceso/paquete/aviso real desde el panel.
