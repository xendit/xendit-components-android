package co.xendit.components.ui.action

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import co.xendit.components.R
import co.xendit.components.ui.helper.CurrencyUtil
import co.xendit.components.ui.helper.SdkImageLoader
import co.xendit.components.ui.style.xenditAppearance
import co.xendit.components.ui.theme.xenditCustomColors
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

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
  snackbarHostState: SnackbarHostState? = null,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val customColors = MaterialTheme.xenditCustomColors
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val hostState = snackbarHostState ?: remember { SnackbarHostState() }
  val qrisImageUrl = "https://assets.xendit.co/payment-session/logos/QRIS.svg"
  val density = LocalDensity.current
  val qrSizePx = remember(density) { with(density) { 260.dp.roundToPx() } }
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

  val formattedAmount = remember(amount, currency) { CurrencyUtil.formatAmount(amount, currency) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.55f)),
    contentAlignment = Alignment.Center
  ) {
    if (snackbarHostState == null) {
      SnackbarHost(
        hostState = hostState,
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
      )
    }
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
        Box(modifier = Modifier.fillMaxWidth()) {
          AsyncImage(
            model =
              ImageRequest.Builder(context)
                .data(qrisImageUrl)
                .crossfade(true)
                .build(),
            imageLoader = SdkImageLoader.get(context),
            contentDescription = null,
            modifier = Modifier
              .align(Alignment.Center)
              .height(28.dp)
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
          text = "Scan To Pay",
          style = MaterialTheme.typography.titleLarge,
          color = appearance.colorText,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = 1.dp,
              color = appearance.colorBorder,
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
            Text(
              text = title ?: channelName,
              style = MaterialTheme.typography.titleMedium,
              color = appearance.colorText,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(appearance.borderRadius))
                .background(customColors.qrBackgroundColor)
                .padding(12.dp),
              contentAlignment = Alignment.Center
            ) {
              if (qrBitmap != null) {
                Image(
                  bitmap = qrBitmap.asImageBitmap(),
                  contentDescription = null,
                  modifier = Modifier.size(260.dp)
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

            OutlinedButton(
              onClick = {
                if (qrBitmap == null) {
                  scope.launch {
                    hostState.showSnackbar("Unable to download QR code")
                  }
                  return@OutlinedButton
                }
                scope.launch(Dispatchers.IO) {
                  val uri =
                    runCatching { saveBitmapToGallery(context, qrBitmap) }.getOrNull()
                  launch(Dispatchers.Main) {
                    if (uri != null) {
                      hostState.showSnackbar("QR code saved")
                    } else {
                      hostState.showSnackbar("Failed to save QR code")
                    }
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFAFAFA),
                contentColor = appearance.colorText
              ),
              shape = RoundedCornerShape(appearance.borderRadius)
            ) {
              Text(
                stringResource(R.string.sessionaction_qr_code_download_qr),
                style = MaterialTheme.typography.titleSmall
              )
            }

            if (formattedAmount.isNotBlank()) {
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleLarge,
                color = appearance.colorText
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedButton(
            onClick = onPaymentMade,
            modifier = Modifier
              .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
              containerColor = appearance.colorBackground,
              contentColor = appearance.colorText
            ),
            shape = RoundedCornerShape(appearance.borderRadius)
          ) {
            Text(
              stringResource(R.string.sessionaction_payment_made),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }

        Text(
          text = stringResource(R.string.sessionaction_payment_confirmation_instructions),
          style = MaterialTheme.typography.bodySmall,
          color = appearance.colorTextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 12.dp)
        )
      }
    }
  }
}

private fun saveBitmapToGallery(
  context: android.content.Context,
  bitmap: Bitmap
): android.net.Uri? {
  val displayName = "xendit_qr_${System.currentTimeMillis()}.png"
  val resolver = context.contentResolver
  val values =
    ContentValues().apply {
      put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
      put(MediaStore.Images.Media.MIME_TYPE, "image/png")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
      }
    }

  val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
  if (uri == null) {
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
    val file = File(dir, displayName)
    return try {
      file.outputStream().use { out ->
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
          throw IllegalStateException("Bitmap compress failed")
        }
      }
      MediaScannerConnection.scanFile(
        context,
        arrayOf(file.absolutePath),
        arrayOf("image/png"),
        null
      )
      file.toUri()
    } catch (_: Exception) {
      null
    }
  }
  return try {
    resolver.openOutputStream(uri)?.use { out ->
      if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
        throw IllegalStateException("Bitmap compress failed")
      }
    } ?: throw IllegalStateException("OutputStream is null")
    uri
  } catch (_: Exception) {
    runCatching { resolver.delete(uri, null, null) }
    null
  }
}
