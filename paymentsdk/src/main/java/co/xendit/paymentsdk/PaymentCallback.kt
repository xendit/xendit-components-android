package co.xendit.paymentsdk

import co.xendit.paymentsdk.data.model.PaymentResult

/**
 * Callback interface for payment events
 */
fun interface PaymentCallback {
  /**
   * Called when the payment interaction is completed
   * @param result The result of the payment interaction (Success or Cancelled)
   */
  fun onPaymentResult(result: PaymentResult)
}
