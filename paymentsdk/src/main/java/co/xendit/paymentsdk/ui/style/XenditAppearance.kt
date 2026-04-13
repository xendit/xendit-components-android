package co.xendit.paymentsdk.ui.style

import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Customization options for the Xendit Mobile SDK. Partners can configure these values to match
 * their brand identity.
 */
@Keep
data class XenditAppearance(
  val fontFamily: FontFamily? = null,
  val colorPrimary: Color? = Color(0xFF0052FF),
  val colorText: Color? = Color(0xFF1A1C1E),
  val colorTextSecondary: Color? = Color(0xFF6B7280),
  val colorTextPlaceholder: Color? = Color(0xFF9CA3AF),
  val colorDisabled: Color? = Color(0xFFE5E7EB),
  val colorDanger: Color? = Color(0xFFBA1A1A),
  val colorBorder: Color? = Color(0xFFE6E6E6),
  val colorBackground: Color? = Color(0xFFFFFFFF),
  val qrForegroundColor: Color? = Color(0xFF000000),
  val qrBackgroundColor: Color? = Color(0xFFFFFFFF),
  val borderRadius: Dp? = 8.dp
)

internal val LocalXenditAppearance = staticCompositionLocalOf { XenditAppearance() }

val xenditAppearance: XenditAppearance
  @Composable get() = LocalXenditAppearance.current
