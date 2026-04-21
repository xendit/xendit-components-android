package co.xendit.paymentsdk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
internal data class XenditCustomColors(
  val colorTextPlaceholder: Color,
  val colorDisabled: Color,
  val colorBorder: Color,
  val qrForegroundColor: Color,
  val qrBackgroundColor: Color
)

internal val LocalXenditCustomColors = staticCompositionLocalOf {
  XenditCustomColors(
    colorTextPlaceholder = Color.Unspecified,
    colorDisabled = Color.Unspecified,
    colorBorder = Color.Unspecified,
    qrForegroundColor = Color.Unspecified,
    qrBackgroundColor = Color.Unspecified
  )
}

internal val MaterialTheme.xenditCustomColors: XenditCustomColors
  @Composable
  get() = LocalXenditCustomColors.current
