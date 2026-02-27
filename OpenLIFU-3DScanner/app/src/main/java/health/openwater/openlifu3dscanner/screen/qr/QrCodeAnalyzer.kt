package health.openwater.openlifu3dscanner.screen.qr

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "QrCodeAnalyzer"
private const val SCHEME = "openlifu://"

data class QrPayload(
    val sessionId: String,
    val sessionName: String
)

internal class QrCodeAnalyzer(
    private val onResult: (QrPayload) -> Unit
) : ImageAnalysis.Analyzer {

    private val active = AtomicBoolean(true)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    fun resume() {
        active.set(true)
    }

    @ExperimentalGetImage
    override fun analyze(proxy: ImageProxy) {
        if (!active.get()) {
            proxy.close()
            return
        }
        val mediaImage = proxy.image ?: run { proxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val raw = barcodes.firstOrNull()?.rawValue ?: return@addOnSuccessListener
                val parsed = parseQrData(raw) ?: return@addOnSuccessListener
                if (active.compareAndSet(true, false)) {
                    mainHandler.post { onResult(parsed) }
                }
            }
            .addOnCompleteListener { proxy.close() }
    }
}

private fun parseQrData(raw: String): QrPayload? {
    if (!raw.startsWith(SCHEME)) {
        Log.e(TAG, "Unexpected QR format (missing scheme): $raw")
        return null
    }
    val body = raw.removePrefix(SCHEME)
    val parts = body.split("|")
    if (parts.size != 2) {
        Log.e(TAG, "Unexpected QR format (expected 2 parts separated by '|'): $raw")
        return null
    }
    return QrPayload(sessionId = parts[0], sessionName = parts[1])
}
