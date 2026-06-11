package co.xendit.components.ui.helper

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

internal object BarcodeGenerator {
  fun generateBarcodeBitmap(
    content: String,
    widthPx: Int,
    heightPx: Int,
    foreground: Color,
    background: Color
  ): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 0)
    val matrix =
      MultiFormatWriter().encode(content, BarcodeFormat.CODE_128, widthPx, heightPx, hints)
    val fg = foreground.toArgbInt()
    val bg = background.toArgbInt()
    val pixels = IntArray(widthPx * heightPx)
    for (y in 0 until heightPx) {
      val offset = y * widthPx
      for (x in 0 until widthPx) {
        pixels[offset + x] = if (matrix[x, y]) fg else bg
      }
    }
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, widthPx, 0, 0, widthPx, heightPx)
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

