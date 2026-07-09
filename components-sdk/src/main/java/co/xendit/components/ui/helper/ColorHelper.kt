package co.xendit.components.ui.helper

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

internal object ColorHelper {
  fun parseHexColorOrNull(hexColor: String?): Color? {
    if (hexColor.isNullOrBlank()) return null
    return runCatching { Color(hexColor.toColorInt()) }.getOrNull()
  }
}