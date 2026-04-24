package co.xendit.components.data.model

import androidx.annotation.Keep

@Keep
internal data class PaymentDraft(
  val channelCode: String? = null,
  val formValues: Map<String, String> = emptyMap(),
  val visibleFields: List<ChannelFormField> = emptyList(),
  val savePaymentMethod: Boolean = false,
  val installmentPlans: List<InstallmentPlan>? = null
)