package co.xendit.components.ui

import co.xendit.components.XenditComponentsPaymentType
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.BffGooglePay
import co.xendit.components.data.model.BffGooglePayAllowedMethod
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.SessionResponse
import co.xendit.components.data.model.isAvailableForAmount
import java.math.BigDecimal

internal sealed interface ResolvedGooglePayChannel {
  data class Ok(val code: String) : ResolvedGooglePayChannel
  data class Err(val userMessage: String) : ResolvedGooglePayChannel
}

internal fun filterGooglePayAllowedMethodsByAmount(
  googlePay: BffGooglePay?,
  channels: List<BffChannel>,
  amount: BigDecimal?,
  sessionType: BffSessionType?,
): List<BffGooglePayAllowedMethod> {
  if (googlePay == null) return emptyList()
  val channelsByCode = channels.associateBy { it.channelCode }
  return googlePay.allowedPaymentMethods.filter { method ->
    val channel = channelsByCode[method.channelCode]
    channel != null && channel.isAvailableForAmount(amount = amount, sessionType = sessionType)
  }
}

internal fun shouldRenderGooglePaySection(
  sessionResponse: SessionResponse?,
  merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>?,
  supportedPaymentTypes: Collection<XenditComponentsPaymentType> = XenditComponentsPaymentType.SUPPORTED,
): Boolean {
  if (XenditComponentsPaymentType.GOOGLE_PAY !in supportedPaymentTypes) return false
  val preferredList = merchantPreferredPaymentMethod
    ?.filter { it in supportedPaymentTypes }
    .orEmpty()
  if (
    preferredList.isNotEmpty() &&
    XenditComponentsPaymentType.GOOGLE_PAY !in preferredList
  ) {
    return false
  }
  return sessionResponse?.digitalWallets?.googlePay != null
}

internal fun resolveGooglePayChannelCodeOrError(
  googlePay: BffGooglePay?,
  paymentMethodType: String?
): ResolvedGooglePayChannel {
  if (googlePay == null) {
    return ResolvedGooglePayChannel.Err(
      "Google Pay configuration is missing from the session response."
    )
  }
  val allowedMethods: List<BffGooglePayAllowedMethod> = googlePay.allowedPaymentMethods
  if (allowedMethods.isEmpty()) {
    return ResolvedGooglePayChannel.Err(
      "Google Pay configuration is empty (no allowed payment methods)."
    )
  }

  if (paymentMethodType.isNullOrBlank()) {
    val availableTypes = allowedMethods.mapNotNull {
      it.paymentMethodSpecification?.get("type")?.asString
    }
    return ResolvedGooglePayChannel.Err(
      "Google Pay payment method type is missing from the response. " +
        "Cannot match against ${allowedMethods.size} configured method(s). " +
        "Configured methods: " + availableTypes.ifEmpty { "(none)" }
    )
  }

  val match = allowedMethods.firstOrNull { allowed ->
    allowed.paymentMethodSpecification
      ?.get("type")
      ?.asString
      ?.equals(paymentMethodType, ignoreCase = true) == true
  }
  return if (match != null) {
    ResolvedGooglePayChannel.Ok(match.channelCode)
  } else {
    val availableTypes = allowedMethods.mapNotNull {
      it.paymentMethodSpecification?.get("type")?.asString
    }
    ResolvedGooglePayChannel.Err(
      "Google Pay payment method not supported: $paymentMethodType. " +
        "Configured methods: ${availableTypes.ifEmpty { "(none)" }}"
    )
  }
}

internal fun buildGooglePayChannelProperties(
  paymentDataJson: String,
  channelCode: String
): Map<String, Any> {
  return if (channelCode == XenditComponentsPaymentType.CARDS.value) {
    mapOf("google_pay" to paymentDataJson)
  } else {
    emptyMap()
  }
}
