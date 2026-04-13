package co.xendit.paymentsdk.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class PaymentOptionsResponse(
  @SerializedName("channel_code") val channelCode: String?,
  @SerializedName("country") val country: String?,
  @SerializedName("amount") val amount: Long?,
  @SerializedName("currency") val currency: String?,
  @SerializedName("installment_plans") val installmentPlans: List<InstallmentPlan>?
)

@Keep
data class InstallmentPlan(
  @SerializedName("interval") val interval: String?,
  @SerializedName("interval_count") val intervalCount: Int?,
  @SerializedName("terms") val terms: Int?,
  @SerializedName("installment_amount") val installmentAmount: Long?,
  @SerializedName("total_amount") val totalAmount: Long?,
  @SerializedName("description") val description: String?,
  @SerializedName("interest_rate") val interestRate: Double?
)
