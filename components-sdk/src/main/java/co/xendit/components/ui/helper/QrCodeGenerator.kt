package co.xendit.components.ui.helper

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

internal object QrCodeGenerator {
  fun generateQrBitmap(
    content: String,
    sizePx: Int,
    foreground: Color,
    background: Color
  ): Bitmap {
    val hints =
      mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 0
      )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val fg = foreground.toArgbInt()
    val bg = background.toArgbInt()
    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
      val offset = y * sizePx
      for (x in 0 until sizePx) {
        pixels[offset + x] = if (matrix[x, y]) fg else bg
      }
    }
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    return bitmap
  }
}

private fun Color.toArgbInt(): Int {
  val a = (alpha * 255f + 0.5f).toInt().coerceIn(0, 255)
  val r = (red * 255f + 0.5f).toInt().coerceIn(0, 255)
  val g = (green * 255f + 0.5f).toInt().coerceIn(0, 255)
  val b = (blue * 255f + 0.5f).toInt().coerceIn(0, 255)
  return (a shl 24) or (r shl 16) or (g shl 8) or b
}

