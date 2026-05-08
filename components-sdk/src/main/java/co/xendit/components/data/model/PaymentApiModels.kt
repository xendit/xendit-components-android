package co.xendit.components.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
internal data class PaymentOptionsRequest(
  @SerializedName("channel_code") val channelCode: String,
  @SerializedName("channel_properties") val channelProperties: Map<String, Any>
)

@Keep
internal data class SimulatePaymentRequest(
  @SerializedName("channel_code") val channelCode: String
)

@Keep
internal data class PaymentRequest(
  @SerializedName("session_id") val sessionId: String,
  @SerializedName("channel_code") val channelCode: String,
  @SerializedName("channel_properties") val channelProperties: Map<String, Any>,
  @SerializedName("customer") val customer: Customer? = null,
  @SerializedName("save_payment_method") val savePaymentMethod: Boolean? = null
)

@Keep
internal data class Customer(
  val id: String? = null
// Add other customer fields if needed
)

@Keep
internal data class OtpUi(
  val title: String,
  val instructions: String
)

@Keep
internal data class PaymentAction(
  val type: String,
  val descriptor: String?,
  val value: String?,
  @SerializedName("iframe_capable") val iframeCapable: Boolean? = null,
  @SerializedName("action_title") val actionTitle: String? = null,
  @SerializedName("action_subtitle") val actionSubtitle: String? = null,
  @SerializedName("action_graphic") val actionGraphic: String? = null,
  val instructions: Map<String, Any>? = null,
  val otp: OtpUi? = null
)

@Keep
internal data class PaymentResponse(
  @SerializedName(value = "id", alternate = ["payment_request_id", "payment_token_id"])
  val id: String,
  val status: PaymentRequestStatus,
  val failure_code: String? = null,
  val channel_properties: Map<String, Any>? = null,
  val payment_method: PaymentMethod? = null,
  @SerializedName("actions")
  val paymentActions: List<PaymentAction>? = null,
  @SerializedName("session_token_request_id") val sessionTokenRequestId: String? = null,
  @SerializedName("channel_code") val channelCode: String? = null,
  @SerializedName("country") val country: String? = null,
  @SerializedName("currency") val currency: String? = null,
  @SerializedName("business_id") val businessId: String? = null,
  @SerializedName("reference_id") val referenceId: String? = null,
  @SerializedName("description") val description: String? = null,
  @SerializedName("created") val created: String? = null,
  @SerializedName("updated") val updated: String? = null,
  @SerializedName("capture_method") val captureMethod: String? = null,
  @SerializedName("customer_id") val customerId: String? = null,
  @SerializedName("request_amount") val requestAmount: Long? = null,
  @SerializedName("type") val type: String? = null
)

@Keep
internal enum class PaymentRequestStatus {
  @SerializedName("ACCEPTING_PAYMENTS") ACCEPTING_PAYMENTS,
  @SerializedName("REQUIRES_ACTION") REQUIRES_ACTION,
  @SerializedName("PENDING") PENDING,
  @SerializedName("AUTHORIZED") AUTHORIZED,
  @SerializedName("CANCELED") CANCELED,
  @SerializedName("EXPIRED") EXPIRED,
  @SerializedName("SUCCEEDED") SUCCEEDED,
  @SerializedName("FAILED") FAILED
}

@Keep
internal data class PaymentMethod(val id: String, val status: PaymentRequestStatus)

@Keep
internal data class SucceededChannel(
  @SerializedName("channel_code") val channelCode: String,
  @SerializedName("logo_url") val logoUrl: String?
)

@Keep
internal data class PollResponse(
  @SerializedName("session")
  val session: BffSession?,
  @SerializedName("payment_request") val paymentRequest: PaymentResponse?,
  @SerializedName("payment_token") val paymentToken: PaymentResponse?,
  @SerializedName("succeeded_channel") val succeededChannel: SucceededChannel?
)
