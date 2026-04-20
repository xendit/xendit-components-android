package co.xendit.paymentsdk.data.model

import androidx.annotation.Keep

/** Sealed class representing the result of a payment interaction */
sealed class XenditPaymentResult {
  data class Success(
    val paymentRequestId: String?,
    val channelCode: String?
  ) : XenditPaymentResult()
  data class Failed(val error: XenditError) : XenditPaymentResult()
  data object Canceled : XenditPaymentResult()
  data object Expired : XenditPaymentResult()
  data object Dismissed : XenditPaymentResult()
}

@Keep
data class XenditError(
  val code: String,           // e.g. "INSUFFICIENT_FUNDS", "NETWORK_ERROR"
  val message: String,           // localised, safe to display to the user
  val cause: Throwable? = null // original exception, for logging
)
