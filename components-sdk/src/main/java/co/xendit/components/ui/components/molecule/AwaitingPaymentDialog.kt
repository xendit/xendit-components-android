package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import co.xendit.components.R
import co.xendit.components.ui.helper.SdkImageLoader
import co.xendit.components.ui.style.XenditAppearance
import coil.compose.AsyncImage

@Composable
internal fun AwaitingPaymentDialog(
  appearance: XenditAppearance,
  channelName: String,
  channelLogoUrl: String?,
  onClose: () -> Unit
) {
  val context = LocalContext.current
  val imageLoader = remember { SdkImageLoader.get(context) }
  val title = stringResource(id = R.string.sessionaction_empty_list_push_notification_title)
  val subtextTemplate =
    stringResource(id = R.string.sessionaction_empty_list_push_notification_subtext)
  val resolvedChannelName = channelName.ifBlank { "payment" }
  val subtext = subtextTemplate.replace("{{channelName}}", resolvedChannelName)

  Dialog(
    onDismissRequest = onClose,
    properties =
      DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false
      )
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.5f))
          .pointerInteropFilter { true },
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier =
          Modifier
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF3F4F6))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = appearance.colorTextSecondary,
            modifier =
              Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .clickable { onClose() }
          )
        }

        Box(
          modifier =
            Modifier
              .size(92.dp)
              .clip(CircleShape)
              .background(Color.White),
          contentAlignment = Alignment.Center
        ) {
          if (!channelLogoUrl.isNullOrBlank()) {
            val contentDescriptionTemplate = stringResource(R.string.sessionimage_alt_channel_logo)
            AsyncImage(
              model = channelLogoUrl,
              imageLoader = imageLoader,
              contentDescription =
                contentDescriptionTemplate.replace("{{channelName}}", resolvedChannelName),
              contentScale = ContentScale.Fit,
              modifier = Modifier.size(width = 64.dp, height = 40.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = appearance.colorText,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = subtext,
          style = MaterialTheme.typography.bodyMedium,
          color = appearance.colorTextSecondary,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}
