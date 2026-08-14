package co.xendit.components.ui

import co.xendit.components.data.model.BffGooglePay
import co.xendit.components.data.model.BffGooglePayAllowedMethod

internal sealed interface ResolvedGooglePayChannel {
  data class Ok(val code: String) : ResolvedGooglePayChannel
  data class Err(val userMessage: String) : ResolvedGooglePayChannel
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
  return if (channelCode == "CARDS") {
    mapOf("google_pay" to paymentDataJson)
  } else {
    emptyMap()
  }
}
