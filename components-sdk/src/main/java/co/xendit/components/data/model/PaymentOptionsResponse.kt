package co.xendit.components.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

@Keep
internal data class PaymentOptionsResponse(
  @SerializedName("channel_code") val channelCode: String?,
  @SerializedName("country") val country: String?,
  @SerializedName("amount") val amount: BigDecimal?,
  @SerializedName("currency") val currency: String?,
  @SerializedName("installment_plans") val installmentPlans: List<InstallmentPlan>?
)

@Keep
internal data class InstallmentPlan(
  @SerializedName("interval") val interval: String?,
  @SerializedName("interval_count") val intervalCount: Int?,
  @SerializedName("terms") val terms: Int?,
  @SerializedName("installment_amount") val installmentAmount: BigDecimal?,
  @SerializedName("total_amount") val totalAmount: BigDecimal?,
  @SerializedName("description") val description: String?,
  @SerializedName("interest_rate") val interestRate: Double?
)
