package co.xendit.components.ui

import co.xendit.components.data.model.BffDigitalWallets
import co.xendit.components.data.model.PaymentRequest

internal sealed interface GooglePaySubmissionResult {
  data class ReadyToSubmit(
    val channelCode: String,
    val channelProperties: Map<String, Any>
  ) : GooglePaySubmissionResult

  data class PendingWithoutSubmission(val channelCode: String) : GooglePaySubmissionResult
  data class ResolutionError(val userMessage: String) : GooglePaySubmissionResult
}

internal class GooglePaySubmissionCoordinator {
  fun prepare(
    digitalWallets: BffDigitalWallets?,
    paymentDataJson: String,
    paymentMethodType: String?
  ): GooglePaySubmissionResult {
    val googlePay = digitalWallets?.googlePay
    val channelResolution = resolveGooglePayChannelCodeOrError(googlePay, paymentMethodType)
    val channelCode = when (channelResolution) {
      is ResolvedGooglePayChannel.Ok -> channelResolution.code
      is ResolvedGooglePayChannel.Err -> {
        return GooglePaySubmissionResult.ResolutionError(channelResolution.userMessage)
      }
    }
    val channelProperties = buildGooglePayChannelProperties(paymentDataJson, channelCode)
    return if (channelProperties.isEmpty()) {
      GooglePaySubmissionResult.PendingWithoutSubmission(channelCode)
    } else {
      GooglePaySubmissionResult.ReadyToSubmit(
        channelCode = channelCode,
        channelProperties = channelProperties
      )
    }
  }

  fun toPaymentRequest(
    executionContext: PaymentExecutionContext,
    prepared: GooglePaySubmissionResult.ReadyToSubmit
  ): PaymentRequest {
    return PaymentRequest(
      sessionId = executionContext.sessionAuthKey,
      channelCode = prepared.channelCode,
      channelProperties = prepared.channelProperties
    )
  }
}
