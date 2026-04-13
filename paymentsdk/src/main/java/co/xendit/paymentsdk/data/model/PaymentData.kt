package co.xendit.paymentsdk.data.model

import androidx.annotation.Keep

/** Sealed class representing the result of a payment interaction */
sealed class PaymentResult {
  data class Success(val result: String) : PaymentResult()
  data class Failed(val throwable: XenditError) : PaymentResult()
  data object Canceled : PaymentResult()
  data object Expired : PaymentResult()
  data object Dismissed : PaymentResult()
}

@Keep
data class XenditError(
  val code: String,           // e.g. "INSUFFICIENT_FUNDS", "NETWORK_ERROR"
  val message: String,           // localised, safe to display to the user
  val cause: Throwable? = null // original exception, for logging
)
