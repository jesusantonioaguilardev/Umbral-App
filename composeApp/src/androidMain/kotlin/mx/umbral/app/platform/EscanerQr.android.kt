package mx.umbral.app.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import mx.umbral.app.ui.BotonIcono
import mx.umbral.app.ui.BotonPrimario
import mx.umbral.app.ui.BotonSecundario
import mx.umbral.app.ui.Destacado
import mx.umbral.app.ui.FondoQr
import mx.umbral.app.ui.SobreDestacado
import mx.umbral.app.ui.SobreDestacadoSuave
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
actual fun escanerQrDisponible(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
}

@Composable
actual fun EscanerQr(onLeido: (String) -> Unit, onCerrar: () -> Unit) {
    val context = LocalContext.current
    fun tienePermiso() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    // null: aún no se sabe (se está pidiendo)
    var permiso by remember { mutableStateOf<Boolean?>(if (tienePermiso()) true else null) }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permiso = it }
    LaunchedEffect(Unit) { if (permiso == null) lanzador.launch(Manifest.permission.CAMERA) }

    // Al volver de Ajustes se revisa otra vez el permiso
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME && permiso == false && tienePermiso()) permiso = true
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }

    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Destacado)) {
            var error by remember { mutableStateOf<String?>(null) }
            when {
                permiso == false -> SinPermiso(context, onCerrar)
                error != null -> Mensaje("No se pudo abrir la cámara", error!!, onCerrar)
                permiso == true -> {
                    VistaCamara(onLeido = onLeido, onError = { error = it })
                    MarcoGuia()
                }
            }
            BotonIcono(Icons.Rounded.Close, "Cerrar escáner", onCerrar, Modifier.safeDrawingPadding().padding(16.dp).align(Alignment.TopEnd))
        }
    }
}

/** Cuadro guía al centro y la instrucción abajo. */
@Composable
private fun MarcoGuia() {
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Box(Modifier.size(260.dp).align(Alignment.Center).border(3.dp, FondoQr, RoundedCornerShape(18.dp)))
        Text(
            "Apunta al QR del pase",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
                .background(Destacado.copy(alpha = 0.85f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
            color = SobreDestacado, style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun SinPermiso(context: Context, onCerrar: () -> Unit) {
    Mensaje(
        "Permite el uso de la cámara",
        "Umbral necesita la cámara para leer el QR del pase. Actívala en Ajustes o escribe el código a mano.",
        onCerrar,
    ) {
        BotonPrimario("Abrir ajustes", {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }, acento = true)
    }
}

@Composable
private fun Mensaje(titulo: String, texto: String, onCerrar: () -> Unit, accion: @Composable () -> Unit = {}) {
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(titulo, color = SobreDestacado, style = MaterialTheme.typography.headlineSmall)
            Text(texto, color = SobreDestacadoSuave, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Start)
            accion()
            BotonSecundario("Escribir código", onCerrar)
        }
    }
}

@Composable
private fun VistaCamara(onLeido: (String) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val alLeer by rememberUpdatedState(onLeido)
    val alFallar by rememberUpdatedState(onError)
    // Evita lecturas dobles: ML Kit puede devolver el mismo QR en varios cuadros seguidos
    val leido = remember { AtomicBoolean(false) }
    val vista = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        val ejecutor = Executors.newSingleThreadExecutor()
        val lector = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
        val futuro = ProcessCameraProvider.getInstance(context)
        var proveedor: ProcessCameraProvider? = null
        futuro.addListener({
            runCatching {
                val p = futuro.get().also { proveedor = it }
                // Las tabletas de caseta a veces solo tienen cámara frontal
                val camara = when {
                    p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> error("Este dispositivo no tiene cámara disponible.")
                }
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(vista.surfaceProvider) }
                val analisis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analisis.setAnalyzer(ejecutor) { imagen ->
                    analizar(imagen, lector, leido) { texto -> vibrar(context); alLeer(texto) }
                }
                p.unbindAll()
                p.bindToLifecycle(lifecycleOwner, camara, preview, analisis)
            }.onFailure { alFallar(it.message ?: "Error de cámara") }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            proveedor?.unbindAll()
            lector.close()
            ejecutor.shutdown()
        }
    }

    AndroidView({ vista }, Modifier.fillMaxSize().semantics { contentDescription = "Vista de la cámara" })
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun analizar(imagen: ImageProxy, lector: BarcodeScanner, leido: AtomicBoolean, onTexto: (String) -> Unit) {
    val media = imagen.image
    if (media == null || leido.get()) { imagen.close(); return }
    // Los listeners de ML Kit corren en el hilo principal
    lector.process(InputImage.fromMediaImage(media, imagen.imageInfo.rotationDegrees))
        .addOnSuccessListener { codigos ->
            val texto = codigos.firstNotNullOfOrNull { c -> c.rawValue?.trim()?.takeIf { it.isNotEmpty() } }
            if (texto != null && leido.compareAndSet(false, true)) onTexto(texto)
        }
        .addOnCompleteListener { imagen.close() }
}

/** Vibración corta al leer; si el equipo no vibra (muchas tabletas), no pasa nada. */
private fun vibrar(context: Context) {
    val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    if (v == null || !v.hasVibrator()) return
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") v.vibrate(80)
    }
}
