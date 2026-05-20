package co.xendit.components.ui.action

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.xendit.components.ui.style.xenditAppearance
import co.xendit.components.ui.theme.xenditCustomColors
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import co.xendit.components.util.AmountFormat

@Composable
internal fun ActionQrUI(
  title: String?,
  channelName: String,
  channelLogoUrl: String?,
  qrString: String,
  amount: Long?,
  currency: String?,
  onClose: () -> Unit,
  onPaymentMade: () -> Unit,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val customColors = MaterialTheme.xenditCustomColors
  val density = LocalDensity.current
  val qrSizePx = remember(density) { with(density) { 240.dp.roundToPx() } }
  val qrBitmap =
    remember(qrString, qrSizePx, customColors.qrForegroundColor, customColors.qrBackgroundColor) {
      runCatching {
        QrCodeGenerator.generateQrBitmap(
          content = qrString,
          sizePx = qrSizePx,
          foreground = customColors.qrForegroundColor,
          background = customColors.qrBackgroundColor
        )
      }.getOrNull()
    }

  val formattedAmount = remember(amount, currency) {
    AmountFormat.format(amount, currency)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.55f)),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
      shape = RoundedCornerShape(appearance.borderRadius),
      color = appearance.colorBackground,
      tonalElevation = 0.dp
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = title ?: "Complete Your Payment",
            style = MaterialTheme.typography.titleMedium,
            color = appearance.colorText,
            modifier = Modifier.weight(1f)
          )
          IconButton(onClick = onClose) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = null,
              tint = appearance.colorTextSecondary
            )
          }
        }

        if (!channelLogoUrl.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(8.dp))
          AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(channelLogoUrl).crossfade(true).build(),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
          )
        } else {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = channelName,
            style = MaterialTheme.typography.titleSmall,
            color = appearance.colorTextSecondary
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(appearance.borderRadius))
            .background(customColors.qrBackgroundColor)
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          if (qrBitmap != null) {
            Image(
              bitmap = qrBitmap.asImageBitmap(),
              contentDescription = null,
              modifier = Modifier.size(240.dp)
            )
          } else {
            Text(
              text = "Unable to generate QR code",
              style = MaterialTheme.typography.bodyMedium,
              color = appearance.colorDanger,
              textAlign = TextAlign.Center
            )
          }
        }

        if (formattedAmount.isNotBlank()) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = formattedAmount,
            style = MaterialTheme.typography.titleMedium,
            color = appearance.colorText
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          Button(
            onClick = onPaymentMade,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFEDEDED),
              contentColor = appearance.colorText
            )
          ) {
            Text("I've made this payment", style = MaterialTheme.typography.titleSmall)
          }
          Text(
            text = "Once you've paid, click the button above to get your payment confirmation.",
            style = MaterialTheme.typography.bodySmall,
            color = appearance.colorTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
          )
        }
      }
    }
  }
}
