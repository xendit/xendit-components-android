package co.xendit.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import co.xendit.components.R
import co.xendit.components.ui.AwaitingPaymentAction.GooglePayProcessing
import co.xendit.components.ui.components.molecule.AwaitingPaymentDialog
import co.xendit.components.ui.style.XenditAppearance

@Composable
internal fun StartupErrorDialog(
  flowState: PaymentFlowState,
  onCleanup: () -> Unit
) {
  when (flowState) {
    is PaymentFlowState.StartupError -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AlertDialog(
          onDismissRequest = onCleanup,
          title = { Text(stringResource(id = R.string.sessiondefault_error_title)) },
          text = { Text(flowState.message) },
          confirmButton = {
            Button(
              onClick = onCleanup,
              modifier = Modifier.testTag(XenditTestTags.DIALOG_ERROR_CLOSE_BUTTON)
            ) {
              Text(stringResource(R.string.sessiondialog_close))
            }
          }
        )
      }
    }

    else -> Unit
  }
}

@Composable
internal fun PaymentOverlay(
  state: PaymentState,
  style: XenditAppearance,
  onClose: () -> Unit
) {
  when (val overlayState = state.overlayState) {
    is PaymentOverlayState.AwaitingAction -> {
      val awaitingPaymentAction = overlayState.action
      val resolvedChannelName = state.selectedChannel?.brandName.orEmpty().ifBlank { "payment" }
      val deeplinkTitleTemplate = stringResource(R.string.sessionaction_deeplink_instructions)
      val emptyPaymentActions =
        stringResource(R.string.sessionaction_empty_list_push_notification_subtext)

      val subtitle =
        when (awaitingPaymentAction) {
          AwaitingPaymentAction.GooglePayProcessing -> null
          AwaitingPaymentAction.Deeplink -> deeplinkTitleTemplate.replace(
            "{{channelName}}",
            resolvedChannelName
          )

          AwaitingPaymentAction.EmptyPaymentActions -> emptyPaymentActions.replace(
            "{{channelName}}",
            resolvedChannelName
          )
        }
      AwaitingPaymentDialog(
        modifier = Modifier.fillMaxSize(),
        appearance = style,
        channelLogoUrl = state.selectedChannel?.brandLogoUrl.takeIf { awaitingPaymentAction != GooglePayProcessing },
        channelLogoRes = R.drawable.ic_google_pay.takeIf { awaitingPaymentAction == GooglePayProcessing },
        onClose = onClose,
        title = stringResource(R.string.sessionaction_deeplink_title),
        subtitle = subtitle ?: ""
      )
    }

    PaymentOverlayState.Loading -> {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.08f))
          .pointerInteropFilter { true },
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
    }

    null -> Unit
  }
}
