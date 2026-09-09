package co.xendit.components.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import co.xendit.components.XenditComponentsPaymentType
import co.xendit.components.data.model.PaymentAction
import co.xendit.components.data.model.PaymentActionDescriptor
import co.xendit.components.ui.action.ActionBarcodeUI
import co.xendit.components.ui.action.ActionQrUI
import co.xendit.components.ui.action.ActionVirtualAccountUI
import co.xendit.components.ui.action.ActionWebViewUI
import co.xendit.components.ui.card.CardState
import co.xendit.components.ui.card.CardViewModel
import co.xendit.components.ui.style.XenditAppearance

@Composable
internal fun PaymentFlowContent(
  state: PaymentState,
  cardState: CardState,
  style: XenditAppearance,
  appearance: XenditAppearance,
  merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>?,
  snackbarHostState: SnackbarHostState,
  dismiss: () -> Unit,
  viewModel: PaymentViewModel,
  cardViewModel: CardViewModel,
  onCommitAutofill: () -> Unit,
  context: android.content.Context
) {
  when (val flowState = state.flowState) {
    is PaymentFlowState.Redirecting -> {
      RedirectPaymentScreen(
        action = flowState.action,
        viewModel = viewModel,
        context = context
      )
    }

    is PaymentFlowState.PresentingCustomerAction -> {
      CustomerActionScreen(
        state = state,
        action = flowState.action,
        snackbarHostState = snackbarHostState,
        onClose = { viewModel.markClosed() },
        onPaymentMade = {
          viewModel.dispatch(ActionIntent.SimulatePayment)
          viewModel.dispatch(ActionIntent.ChallengeCompleted(true))
          viewModel.showLoading()
        }
      )
    }

    PaymentFlowState.SelectingPaymentMethod -> {
      PaymentSelectionScreen(
        state = state,
        cardState = cardState,
        style = style,
        appearance = appearance,
        merchantPreferredPaymentMethod = merchantPreferredPaymentMethod,
        dismiss = dismiss,
        viewModel = viewModel,
        cardViewModel = cardViewModel,
        onCommitAutofill = onCommitAutofill
      )
    }

    PaymentFlowState.LoadingSession,
    is PaymentFlowState.StartupError -> Unit
  }
}

@Composable
internal fun RedirectPaymentScreen(
  action: PaymentAction,
  viewModel: PaymentViewModel,
  context: android.content.Context
) {
  val url = action.value.orEmpty()
  if (action.descriptor == PaymentActionDescriptor.DEEPLINK_URL) {
    LaunchedEffect(url) {
      if (url.isNotBlank()) {
        val didLaunch = runCatching {
          context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
          )
        }.isSuccess
        if (didLaunch) {
          viewModel.showLoadingWithAction()
        }
      }
      viewModel.dispatch(ActionIntent.ClearPaymentActionRedirect)
    }
  } else {
    ActionWebViewUI(
      url = url,
      onClose = { viewModel.dispatch(ActionIntent.CloseWebPayment) },
      onChallengeCompleted = { viewModel.dispatch(ActionIntent.ChallengeCompleted(true)) },
      iframeCapable = action.iframeCapable ?: true
    )
  }
}

@Composable
internal fun CustomerActionScreen(
  state: PaymentState,
  action: PaymentAction,
  snackbarHostState: SnackbarHostState,
  onClose: () -> Unit,
  onPaymentMade: () -> Unit
) {
  val actionUi = state.customerActionUi ?: return

  when (action.descriptor) {
    PaymentActionDescriptor.VIRTUAL_ACCOUNT_NUMBER -> {
      ActionVirtualAccountUI(
        title = action.actionTitle,
        subtitle = action.actionSubtitle,
        channelName = actionUi.channelName.ifBlank { "Virtual Account" },
        channelLogoUrl = actionUi.channelLogoUrl,
        channelBrandColor = actionUi.channelBrandColor,
        virtualAccountNumber = action.value.orEmpty(),
        merchantName = actionUi.merchantName,
        amount = actionUi.amount,
        currency = actionUi.currency,
        instructions = action.instructions,
        onClose = onClose,
        onPaymentMade = onPaymentMade,
        snackbarHostState = snackbarHostState
      )
    }

    PaymentActionDescriptor.QR_STRING -> {
      ActionQrUI(
        title = action.actionTitle,
        merchantName = actionUi.merchantName,
        channelName = actionUi.channelName.ifBlank { "QR Code" },
        channelLogoUrl = actionUi.channelLogoUrl,
        qrString = action.value.orEmpty(),
        amount = actionUi.amount,
        currency = actionUi.currency,
        onClose = onClose,
        onPaymentMade = onPaymentMade,
        snackbarHostState = snackbarHostState
      )
    }

    PaymentActionDescriptor.PAYMENT_CODE -> {
      ActionBarcodeUI(
        title = action.actionTitle,
        subtitle = action.actionSubtitle,
        channelName = actionUi.channelName.ifBlank { "Payment Code" },
        channelLogoUrl = actionUi.channelLogoUrl,
        channelBrandColor = actionUi.channelBrandColor,
        paymentCode = action.value.orEmpty(),
        merchantName = actionUi.merchantName,
        amount = actionUi.amount,
        currency = actionUi.currency,
        instructions = action.instructions,
        onClose = onClose,
        onPaymentMade = onPaymentMade,
        snackbarHostState = snackbarHostState
      )
    }

    else -> onClose()
  }
}
