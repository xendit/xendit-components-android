package co.xendit.components.ui

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import co.xendit.components.core.CoreSdkComponent.globalErrorHandler
import co.xendit.components.data.model.XenditPaymentResult
import co.xendit.components.ui.card.CardIntent
import co.xendit.components.ui.card.CardViewModel
import co.xendit.components.ui.helper.FailureCodeMessageUtil

@Composable
internal fun ObservePaymentContainerSideEffects(
  viewModel: PaymentViewModel,
  cardViewModel: CardViewModel,
  snackbarHostState: SnackbarHostState,
  sessionAuthKey: String,
  publicKey: String,
  state: PaymentState,
  context: Context,
  onFinish: suspend (XenditPaymentResult) -> Unit,
  onEmitResult: (XenditPaymentResult) -> Unit,
  onCloseWebPayment: () -> Unit,
  onPendingSnackbar: (String?) -> Unit
) {
  val resultResolver =
    remember(context) {
      PaymentContainerResultResolver(
        failureMessageResolver = { failureCode ->
          FailureCodeMessageUtil.resolveFailureMessage(context, failureCode)
        }
      )
    }

  LaunchedEffect(sessionAuthKey, publicKey) {
    viewModel.dispatch(ActionIntent.Initialize(sessionAuthKey, publicKey))
  }

  LaunchedEffect(Unit) {
    globalErrorHandler.apiErrorFlow.collect { (errorCode, message) ->
      when (val effect = resultResolver.resolveApiError(errorCode, message?.asString(context))) {
        is PaymentContainerEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
        is PaymentContainerEffect.NotifyFailure -> {
          effect.snackbarMessage?.let { snackbarHostState.showSnackbar(it) }
          onEmitResult(effect.result)
        }

        is PaymentContainerEffect.Finish,
        null -> Unit
      }
    }
  }

  LaunchedEffect(state.sessionResponse) {
    val sessionResponse = state.sessionResponse ?: return@LaunchedEffect
    when (val effect = resultResolver.resolveSession(sessionResponse)) {
      is PaymentContainerEffect.Finish -> onFinish(effect.result)
      is PaymentContainerEffect.NotifyFailure -> {
        effect.snackbarMessage?.let(onPendingSnackbar)
        onEmitResult(effect.result)
        if (effect.closeWebPayment) {
          onCloseWebPayment()
        }
      }

      is PaymentContainerEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
      null -> Unit
    }
  }

  LaunchedEffect(state.pollResponse) {
    val pollResponse = state.pollResponse ?: return@LaunchedEffect
    when (val effect = resultResolver.resolvePoll(pollResponse)) {
      is PaymentContainerEffect.Finish -> onFinish(effect.result)
      is PaymentContainerEffect.NotifyFailure -> {
        effect.snackbarMessage?.let(onPendingSnackbar)
        onEmitResult(effect.result)
        if (effect.closeWebPayment) {
          onCloseWebPayment()
        }
      }

      is PaymentContainerEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
      null -> Unit
    }
  }

  LaunchedEffect(sessionAuthKey, publicKey, state.paymentSessionId) {
    val paymentSessionId = state.paymentSessionId ?: return@LaunchedEffect
    cardViewModel.dispatch(
      CardIntent.ConfigureSession(
        sessionAuthKey = sessionAuthKey,
        publicKey = publicKey,
        paymentSessionId = paymentSessionId
      )
    )
  }
}
