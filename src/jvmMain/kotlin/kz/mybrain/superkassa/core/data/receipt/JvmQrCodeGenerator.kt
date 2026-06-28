package kz.mybrain.superkassa.core.data.receipt

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.client.j2se.MatrixToImageWriter
import kz.mybrain.superkassa.core.domain.port.QrCodeGeneratorPort
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

class JvmQrCodeGenerator : QrCodeGeneratorPort {
    init {
        System.setProperty("java.awt.headless", "true")
    }

    override fun generatePngDataUri(text: String, sizePx: Int): String? {
        if (text.isBlank()) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val image = MatrixToImageWriter.toBufferedImage(matrix)
            val baos = ByteArrayOutputStream()
            ImageIO.write(image, "PNG", baos)
            "data:image/png;base64,${Base64.getEncoder().encodeToString(baos.toByteArray())}"
        }.getOrNull()
    }
}
