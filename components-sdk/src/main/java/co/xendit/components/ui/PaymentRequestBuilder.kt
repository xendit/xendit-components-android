package co.xendit.components.ui

import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.FieldType
import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.data.model.PaymentRequest
import co.xendit.components.data.model.primaryChannelPropertyKey
import co.xendit.components.util.PaymentRequestMapper

internal class PaymentRequestBuilder {
  fun build(
    executionContext: PaymentExecutionContext,
    displayedChannelCode: String,
    channelVariantsByDisplayCode: Map<String, ChannelVariantChannels>,
    formValues: Map<String, String>,
    fields: List<ChannelFormField>,
    savePaymentMethod: Boolean,
    installmentPlans: List<InstallmentPlan>?
  ): PaymentRequest {
    val variantsForDisplay = channelVariantsByDisplayCode[displayedChannelCode]
    val effectiveChannel =
      variantsForDisplay?.resolve(savePaymentMethod = savePaymentMethod)
    val effectiveChannelCode = effectiveChannel?.channelCode ?: displayedChannelCode
    return buildPaymentRequest(
      sessionAuthKey = executionContext.sessionAuthKey,
      publicKey = executionContext.publicKey,
      paymentSessionId = executionContext.paymentSessionId,
      effectiveChannelCode = effectiveChannelCode,
      formValues = formValues,
      fields = fields,
      savePaymentMethod = savePaymentMethod,
      installmentPlans = installmentPlans,
      effectiveChannelForm = effectiveChannel?.form
    )
  }
}

private fun ChannelVariantChannels.resolve(savePaymentMethod: Boolean): BffChannel? {
  return when {
    savePaymentMethod && saveChannel != null -> saveChannel
    !savePaymentMethod && nonSaveChannel != null -> nonSaveChannel
    else -> null
  }
}

internal fun buildPaymentRequest(
  sessionAuthKey: String,
  publicKey: String,
  paymentSessionId: String,
  effectiveChannelCode: String,
  formValues: Map<String, String>,
  fields: List<ChannelFormField>,
  savePaymentMethod: Boolean,
  installmentPlans: List<InstallmentPlan>?,
  effectiveChannelForm: List<ChannelFormField>?
): PaymentRequest {
  val allowedKeysFromChannelForm =
    effectiveChannelForm
      ?.map { it.primaryChannelPropertyKey() }
      ?.filter { it.isNotBlank() }
      ?.toSet()
      .orEmpty()
  val shouldFilterByChannelForm = allowedKeysFromChannelForm.isNotEmpty()
  val filteredFields =
    if (shouldFilterByChannelForm) {
      fields.filter { it.primaryChannelPropertyKey() in allowedKeysFromChannelForm }
    } else {
      fields
    }
  val allowedValueKeys =
    if (shouldFilterByChannelForm) {
      mutableSetOf<String>().apply {
        addAll(allowedKeysFromChannelForm)
        filteredFields.forEach { field ->
          val primaryKey = field.primaryChannelPropertyKey()
          if (primaryKey.isBlank()) return@forEach
          if (field.type is FieldType.PhoneNumber) {
            add("${primaryKey}_country_code")
          }
        }
      }
    } else {
      null
    }
  val filteredFormValues =
    if (shouldFilterByChannelForm) {
      formValues.filterKeys { it in allowedValueKeys.orEmpty() }
    } else {
      formValues
    }

  val channelProperties =
    PaymentRequestMapper.mapFormValuesToChannelProperties(
      formValues = filteredFormValues,
      fields = filteredFields,
      publicKey = publicKey,
      sessionId = paymentSessionId,
      installmentPlans = installmentPlans
    )

  return PaymentRequest(
    sessionId = sessionAuthKey,
    channelCode = effectiveChannelCode,
    channelProperties = channelProperties,
    savePaymentMethod = if (savePaymentMethod) true else null
  )
}
