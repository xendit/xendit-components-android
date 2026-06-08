package co.xendit.components.ui.action

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.Window
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.net.toUri
import co.xendit.components.R
import co.xendit.components.ui.helper.CurrencyUtil
import co.xendit.components.ui.helper.QrCodeGenerator
import co.xendit.components.ui.helper.QrNmidSearcherUtil
import co.xendit.components.ui.helper.SdkImageLoader
import co.xendit.components.ui.style.xenditAppearance
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import kotlin.coroutines.resume
import kotlin.math.roundToInt

@Composable
internal fun ActionQrUI(
  title: String?,
  channelName: String,
  channelLogoUrl: String?,
  qrString: String,
  amount: BigDecimal?,
  currency: String?,
  onClose: () -> Unit,
  onPaymentMade: () -> Unit,
  snackbarHostState: SnackbarHostState? = null,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val context = LocalContext.current
  val view = LocalView.current
  val scope = rememberCoroutineScope()
  val hostState = snackbarHostState ?: remember { SnackbarHostState() }
  val density = LocalDensity.current
  val qrSizePx = remember(density) { with(density) { 260.dp.roundToPx() } }
  var hideDownloadButton by remember { mutableStateOf(false) }
  var borderedContentBounds by remember { mutableStateOf<Rect?>(null) }
  val window: Window? =
    (view.parent as? DialogWindowProvider)?.window ?: context.findActivity()?.window
  val qrBitmap =
    remember(qrString, qrSizePx, appearance.qrForegroundColor, appearance.qrBackgroundColor) {
      runCatching {
        QrCodeGenerator.generateQrBitmap(
          content = qrString,
          sizePx = qrSizePx,
          foreground = appearance.qrForegroundColor,
          background = appearance.qrBackgroundColor
        )
      }.getOrNull()
    }
  val nmid = remember {
    QrNmidSearcherUtil.getNationalMerchantID(qrString)
  }

  val formattedAmount = remember(amount, currency) { CurrencyUtil.formatAmount(amount, currency) }

  // COPY
  val unableDownloadQrText = stringResource(R.string.sessionaction_qr_unable_download_qr)
  val qrSavedText = stringResource(R.string.sessionaction_qr_saved)
  val qrFailedSaveText = stringResource(R.string.sessionaction_qr_failed_save)

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
                .data(channelLogoUrl)
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
          text = stringResource(R.string.sessionaction_qr_scan_to_pay),
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
            )
            .onGloballyPositioned { coordinates ->
              val bounds = coordinates.boundsInWindow()
              borderedContentBounds =
                Rect(
                  bounds.left.roundToInt(),
                  bounds.top.roundToInt(),
                  bounds.right.roundToInt(),
                  bounds.bottom.roundToInt()
                )
            },
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
            Spacer(modifier = Modifier.height(8.dp))
            nmid?.let {
              Text(
                text = stringResource(R.string.sessionaction_qr_nmid, it),
                style = MaterialTheme.typography.titleMedium,
                color = appearance.colorText,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(12.dp))
            }


            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(appearance.borderRadius))
                .background(appearance.qrBackgroundColor)
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
                  text = stringResource(R.string.sessionaction_qr_unable_generate_qr),
                  style = MaterialTheme.typography.bodyMedium,
                  color = appearance.colorDanger,
                  textAlign = TextAlign.Center
                )
              }
            }

            if (!hideDownloadButton) {
              Spacer(modifier = Modifier.height(12.dp))
              Box {
                OutlinedButton(
                  onClick = {
                    if (qrBitmap == null) {
                      scope.launch { hostState.showSnackbar(unableDownloadQrText) }
                      return@OutlinedButton
                    }
                    scope.launch {
                      val uri =
                        withContext(Dispatchers.IO) {
                          runCatching { saveBitmapToGallery(context, qrBitmap) }.getOrNull()
                        }
                      if (uri != null) {
                        hostState.showSnackbar(qrSavedText)
                      } else {
                        hostState.showSnackbar(qrFailedSaveText)
                      }
                    }

//                    Just in case we need to download with frame
//                    scope.launch {
//                      val w = window
//                      if (w == null) {
//                        hostState.showSnackbar(qrFailedDownloadText)
//                        return@launch
//                      }
//
//                      val bitmap =
//                        try {
//                          hideDownloadButton = true
//                          withFrameNanos { }
//                          withFrameNanos { } // to wait button dissapear
//                          val rect = borderedContentBounds
//                          if (rect == null || rect.width() <= 0 || rect.height() <= 0) {
//                            null
//                          } else {
//                            captureBitmapFromWindow(w, rect)
//                          }
//                        } finally {
//                          hideDownloadButton = false
//                        }
//
//                      if (bitmap == null) {
//                        hostState.showSnackbar(qrFailedSaveText)
//                        return@launch
//                      }
//
//                      val uri =
//                        withContext(Dispatchers.IO) {
//                          runCatching { saveBitmapToGallery(context, bitmap) }.getOrNull()
//                        }
//                      if (uri != null) {
//                        hostState.showSnackbar(qrSavedText)
//                      } else {
//                        hostState.showSnackbar(qrFailedSaveText)
//                      }
//                    }
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
              }
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

private suspend fun captureBitmapFromWindow(
  window: Window,
  rect: Rect
): Bitmap? {
  return suspendCancellableCoroutine { cont ->
    val decorView = window.decorView
    val safeRect =
      Rect(rect).apply {
        val maxW = decorView.width
        val maxH = decorView.height
        if (maxW > 0 && maxH > 0) {
          intersect(0, 0, maxW, maxH)
        }
      }

    if (safeRect.width() <= 0 || safeRect.height() <= 0) {
      cont.resume(null)
      return@suspendCancellableCoroutine
    }

    val bitmap =
      runCatching {
        Bitmap.createBitmap(safeRect.width(), safeRect.height(), Bitmap.Config.ARGB_8888)
      }.getOrNull()

    if (bitmap == null) {
      cont.resume(null)
      return@suspendCancellableCoroutine
    }

    PixelCopy.request(
      window,
      safeRect,
      bitmap,
      { result ->
        if (cont.isActive) {
          cont.resume(if (result == PixelCopy.SUCCESS) bitmap else null)
        }
      },
      Handler(Looper.getMainLooper())
    )
  }
}

private fun Context.findActivity(): Activity? {
  var current: Context? = this
  while (current != null) {
    if (current is Activity) return current
    current = if (current is ContextWrapper) current.baseContext else null
  }
  return null
}

private fun saveBitmapToGallery(
  context: Context,
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
