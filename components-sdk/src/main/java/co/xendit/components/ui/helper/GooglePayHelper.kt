package co.xendit.components.ui.helper

import android.app.Activity
import co.xendit.components.data.model.BffGooglePay
import co.xendit.components.data.model.BffGooglePayAllowedMethod
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

internal object GooglePayHelper {

  private val gson = Gson()

  fun createPaymentsClient(activity: Activity, isTest: Boolean): PaymentsClient {
    val walletOptions = Wallet.WalletOptions.Builder()
      .setEnvironment(
        if (isTest) WalletConstants.ENVIRONMENT_TEST
        else WalletConstants.ENVIRONMENT_PRODUCTION
      )
      .build()

    return Wallet.getPaymentsClient(activity, walletOptions)
  }

  private fun allowedPaymentMethodsJsonArray(
    allowedPaymentMethods: List<BffGooglePayAllowedMethod>
  ): JSONArray {
    val allowedMethods = JSONArray()
    allowedPaymentMethods.forEach { method ->
      allowedMethods.put(JSONObject(gson.toJson(method.paymentMethodSpecification)))
    }
    return allowedMethods
  }

  fun createIsReadyToPayRequest(
    googlePay: BffGooglePay,
    allowedPaymentMethods: List<BffGooglePayAllowedMethod> = googlePay.allowedPaymentMethods,
  ): IsReadyToPayRequest {
    val json = JSONObject()
      .put("apiVersion", 2)
      .put("apiVersionMinor", 0)
      .put("allowedPaymentMethods", allowedPaymentMethodsJsonArray(allowedPaymentMethods))

    return requireNotNull(IsReadyToPayRequest.fromJson(json.toString()))
  }

  fun createPaymentDataRequest(
    googlePay: BffGooglePay,
    businessName: String,
    paymentSessionId: String,
    amount: BigDecimal,
    currency: String,
    country: String,
    allowedPaymentMethods: List<BffGooglePayAllowedMethod> = googlePay.allowedPaymentMethods,
  ): PaymentDataRequest {
    val json = JSONObject()
      .put("apiVersion", 2)
      .put("apiVersionMinor", 0)
      .put("allowedPaymentMethods", allowedPaymentMethodsJsonArray(allowedPaymentMethods))
      .put("emailRequired", true)
      .put(
        "merchantInfo",
        JSONObject()
          .put("merchantId", googlePay.merchantId)
          .put("merchantName", businessName)
      )
      .put(
        "transactionInfo",
        JSONObject()
          .put("transactionId", paymentSessionId)
          .put("totalPriceStatus", "FINAL")
          .put("totalPrice", amount.toPlainString())
          .put("currencyCode", currency)
          .put("countryCode", country)
      )

    return requireNotNull(PaymentDataRequest.fromJson(json.toString()))
  }
}
