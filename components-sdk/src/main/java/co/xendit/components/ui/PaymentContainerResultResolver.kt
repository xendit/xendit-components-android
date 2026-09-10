package co.xendit.components.ui

import co.xendit.components.data.model.PaymentRequestStatus
import co.xendit.components.data.model.PaymentSessionStatus
import co.xendit.components.data.model.PollResponse
import co.xendit.components.data.model.SessionResponse
import co.xendit.components.data.model.XenditError
import co.xendit.components.data.model.XenditPaymentResult

internal sealed interface PaymentContainerEffect {
  data class Finish(val result: XenditPaymentResult) : PaymentContainerEffect
  data class ShowSnackbar(val message: String) : PaymentContainerEffect
  data class NotifyFailure(
    val result: XenditPaymentResult.Failed,
    val snackbarMessage: String? = null,
    val closeWebPayment: Boolean = false
  ) : PaymentContainerEffect
}

internal class PaymentContainerResultResolver(
  private val failureMessageResolver: (String?) -> String
) {
  fun resolveApiError(
    errorCode: String?,
    message: String?
  ): PaymentContainerEffect? {
    val resolvedMessage = message?.takeIf { it.isNotBlank() } ?: return null
    return if (errorCode == "NETWORK_ERROR") {
      PaymentContainerEffect.NotifyFailure(
        result = XenditPaymentResult.Failed(
          XenditError(
            code = "NETWORK_ERROR",
            message = resolvedMessage,
            cause = Throwable(resolvedMessage)
          )
        ),
        snackbarMessage = resolvedMessage
      )
    } else {
      PaymentContainerEffect.ShowSnackbar(resolvedMessage)
    }
  }

  fun resolveSession(sessionResponse: SessionResponse): PaymentContainerEffect? {
    val session = sessionResponse.session ?: return null
    return when (session.status) {
      PaymentSessionStatus.COMPLETED ->
        PaymentContainerEffect.Finish(
          XenditPaymentResult.Success(
            paymentRequestId = session.paymentSessionId,
            channelCode = sessionResponse.succeededChannel?.channelCode
          )
        )

      PaymentSessionStatus.CANCELED ->
        PaymentContainerEffect.Finish(XenditPaymentResult.Canceled)

      PaymentSessionStatus.EXPIRED ->
        PaymentContainerEffect.Finish(XenditPaymentResult.Expired)

      PaymentSessionStatus.ACTIVE,
      PaymentSessionStatus.PENDING,
      null -> null
    }
  }

  fun resolvePoll(poll: PollResponse): PaymentContainerEffect? {
    val sessionStatus = poll.session?.status
    val requestStatus = poll.paymentRequest?.status
    val isSuccess =
      sessionStatus == PaymentSessionStatus.COMPLETED ||
        requestStatus == PaymentRequestStatus.SUCCEEDED ||
        requestStatus == PaymentRequestStatus.AUTHORIZED ||
        poll.succeededChannel != null
    val isCanceled =
      sessionStatus == PaymentSessionStatus.CANCELED ||
        requestStatus == PaymentRequestStatus.CANCELED
    val isFailed = requestStatus == PaymentRequestStatus.FAILED
    val isExpired =
      sessionStatus == PaymentSessionStatus.EXPIRED ||
        requestStatus == PaymentRequestStatus.EXPIRED

    return when {
      isSuccess ->
        PaymentContainerEffect.Finish(
          XenditPaymentResult.Success(
            paymentRequestId = poll.session?.paymentSessionId,
            channelCode = poll.succeededChannel?.channelCode ?: poll.paymentRequest?.channelCode
          )
        )

      isCanceled ->
        PaymentContainerEffect.Finish(XenditPaymentResult.Canceled)

      isExpired ->
        PaymentContainerEffect.Finish(XenditPaymentResult.Expired)

      isFailed -> {
        val failureCode = poll.paymentRequest?.failure_code
        val failureMessage = failureMessageResolver(failureCode)
          val normalizedFailureCode = failureCode?.trim()?.takeIf { it.isNotBlank() } ?: "UNKNOWN"
        PaymentContainerEffect.NotifyFailure(
          result = XenditPaymentResult.Failed(
            XenditError(
                code = normalizedFailureCode,
              message = failureMessage,
              cause = Throwable("Payment failed Session: $sessionStatus, PR: $requestStatus")
            )
          ),
          snackbarMessage = failureMessage,
          closeWebPayment = true
        )
      }

      else -> null
    }
  }
}
