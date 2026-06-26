package co.xendit.components.ui.action

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.xendit.components.R
import co.xendit.components.core.CoreSdkComponent
import co.xendit.components.data.model.PaymentInstructionTab
import co.xendit.components.ui.helper.BarcodeGenerator
import co.xendit.components.ui.helper.CurrencyUtil
import co.xendit.components.ui.helper.SdkImageLoader
import co.xendit.components.ui.style.xenditAppearance
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.math.BigDecimal
import java.util.UUID

@Composable
internal fun ActionBarcodeUI(
  title: String?,
  subtitle: String?,
  channelName: String,
  channelLogoUrl: String?,
  paymentCode: String,
  merchantName: String?,
  amount: BigDecimal?,
  currency: String?,
  instructions: List<PaymentInstructionTab>?,
  onClose: () -> Unit,
  onPaymentMade: () -> Unit,
  snackbarHostState: SnackbarHostState? = null,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val imageLoader = remember { SdkImageLoader.get(context) }
  val formattedAmount = remember(amount, currency) { CurrencyUtil.formatAmount(amount, currency) }
  val barcodeWidthPx = 900
  val barcodeHeightPx = 260
  val barcodeBitmap =
    remember(paymentCode, barcodeHeightPx, barcodeWidthPx) {
      runCatching {
        BarcodeGenerator.generateBarcodeBitmap(
          content = paymentCode,
          widthPx = barcodeWidthPx,
          heightPx = barcodeHeightPx,
          foreground = appearance.qrForegroundColor,
          background = appearance.qrBackgroundColor
        )
      }.getOrNull()
    }

  val textDownloadError = stringResource(R.string.sessionaction_image_download_error)

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = appearance.colorBackground,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
          model = channelLogoUrl,
          imageLoader = imageLoader,
          contentDescription = "toplogo",
          contentScale = ContentScale.Fit,
          modifier = Modifier.align(Alignment.Center).height(28.dp)
        )
        IconButton(
          onClick = onClose,
          modifier = Modifier.align(Alignment.CenterEnd)
        ) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = appearance.colorTextSecondary
          )
        }
      }

      Text(
        text = title?.takeIf { it.isNotBlank() } ?: channelName,
        style = MaterialTheme.typography.titleLarge,
        color = appearance.colorText,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )

      if (!subtitle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = appearance.colorTextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = 2.dp,
            color = appearance.colorPrimary,
            shape = RoundedCornerShape(appearance.borderRadius)
          ),
        shape = RoundedCornerShape(appearance.borderRadius),
        color = appearance.colorBackground,
        tonalElevation = 0.dp
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          if (barcodeBitmap != null) {
            Image(
              bitmap = barcodeBitmap.asImageBitmap(),
              contentDescription = null,
              modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
            )
          } else {
            Text(
              text = stringResource(R.string.sessionaction_barcode_unable_to_generate),
              style = MaterialTheme.typography.bodyMedium,
              color = appearance.colorDanger,
              textAlign = TextAlign.Center
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = paymentCode,
            style = MaterialTheme.typography.titleMedium,
            color = appearance.colorText
          )

          Spacer(modifier = Modifier.height(12.dp))
          OutlinedButton(
            enabled = barcodeBitmap != null,
            onClick = {
              val bitmap = barcodeBitmap ?: return@OutlinedButton
              scope.launch {
                val success = saveBarcodeToGallery(context = context, bitmap = bitmap)
                if (!success) {
                  snackbarHostState?.showSnackbar(textDownloadError)
                }
              }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
              containerColor = appearance.colorBackground,
              contentColor = appearance.colorText
            ),
            shape = RoundedCornerShape(999.dp)
          ) {
            Text(
              text = stringResource(R.string.sessionaction_barcode_download_barcode),
              style = MaterialTheme.typography.titleSmall
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          if (formattedAmount.isNotBlank()) {
            Column (
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = stringResource(R.string.sessionaction_barcode_amount_to_pay),
                style = MaterialTheme.typography.bodySmall,
                color = appearance.colorTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
              )
              Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleMedium,
                color = appearance.colorText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.sessionaction_barcode_payment_code),
                style = MaterialTheme.typography.bodySmall,
                color = appearance.colorTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = paymentCode,
                style = MaterialTheme.typography.titleSmall,
                color = appearance.colorText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
              )
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.sessionaction_barcode_seller),
                style = MaterialTheme.typography.bodySmall,
                color = appearance.colorTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = merchantName.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = appearance.colorText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          if (!CoreSdkComponent.isProdLive()) {
            OutlinedButton(
              onClick = onPaymentMade,
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(
                containerColor = appearance.colorBackground,
                contentColor = appearance.colorText
              ),
              shape = RoundedCornerShape(appearance.borderRadius)
            ) {
              Text(
                text = stringResource(R.string.sessionaction_simulate_payment),
                style = MaterialTheme.typography.titleSmall
              )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.sessionaction_simulate_payment_instructions),
              style = MaterialTheme.typography.bodySmall,
              color = appearance.colorTextSecondary,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }

      if (!instructions.isNullOrEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        PaymentInstructionsContent(instructions = instructions)
      }
    }
  }

}

private suspend fun saveBarcodeToGallery(context: android.content.Context, bitmap: Bitmap): Boolean {
  return withContext(Dispatchers.IO) {
    runCatching {
      val displayName = "xendit_barcode_${UUID.randomUUID()}.png"
      val values =
        ContentValues().apply {
          put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
          put(MediaStore.Images.Media.MIME_TYPE, "image/png")
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
          }
        }
      val resolver = context.contentResolver
      val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
      resolver.openOutputStream(uri).use { out: OutputStream? ->
        if (out == null) return@runCatching false
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
      }
      true
    }.getOrDefault(false)
  }
}