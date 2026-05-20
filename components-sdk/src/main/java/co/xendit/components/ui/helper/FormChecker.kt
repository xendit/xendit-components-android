package co.xendit.components.ui.helper

import co.xendit.components.data.model.BffCardInfo
import co.xendit.components.data.model.CardDetails
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.FieldType
import co.xendit.components.data.model.primaryChannelPropertyKey
import co.xendit.components.ui.helper.FormCheckerUtil.isValidCardExpiry
import co.xendit.components.ui.helper.FormCheckerUtil.isValidCreditCard
import co.xendit.components.ui.helper.FormCheckerUtil.isValidEmail
import co.xendit.components.ui.helper.FormCheckerUtil.isValidPhoneNumber
import kotlin.collections.forEach

internal object FormChecker {

  private const val CARD_BRAND_NOT_SUPPORTED_MESSAGE =
    "This card brand is not supported for this transaction."

  fun validateAllField(
    fields: List<ChannelFormField>,
    values: Map<String, String>,
    cardDetails: CardDetails? = null,
    bffCardInfo: BffCardInfo? = null
  ): Boolean {
    fields.forEach { field ->
      if (
        validateField(
          field = field,
          value = values[field.primaryChannelPropertyKey()] ?: "",
          values = values,
          cardDetails = cardDetails,
          bffCardInfo = bffCardInfo
        ) != null
      ) {
        return false
      }
    }
    return true
  }

  fun validateField(
    field: ChannelFormField,
    value: String,
    values: Map<String, String>? = null,
    cardDetails: CardDetails? = null,
    bffCardInfo: BffCardInfo? = null
  ): String? {
    if (field.required && value.isBlank()) {
      return "${field.label} is required"
    }

    when (field.type) {
      is FieldType.CreditCardNumber -> {
        if (!isValidCreditCard(value)) {
          return "${field.label} is not valid"
        }
        val scheme = cardDetails?.schemes?.firstOrNull()
        if (scheme != null && !isCardBrandSupported(scheme, bffCardInfo)) {
          return CARD_BRAND_NOT_SUPPORTED_MESSAGE
        }
      }
      is FieldType.CreditCardExpiry -> {
        if (!isValidCardExpiry(value)) {
          return "${field.label} is not valid"
        }
      }
      is FieldType.PhoneNumber -> {
        if (value.isNotBlank()) {
          val propertyKey = field.primaryChannelPropertyKey()
          val countryCodeKey = "${propertyKey}_country_code"
          val regionCode = values?.get(countryCodeKey) ?: "ID"
          if (!isValidPhoneNumber(value, regionCode)) {
            return "${field.label} is not valid"
          }
        }
      }
      is FieldType.Email -> {
        if (value.isNotBlank() && !isValidEmail(value)) {
          return "${field.label} is not valid"
        }
      }
      else -> {
        if (field.type is FieldType.Text) {
          if (field.type.autocomplete == "email") {
            if (value.isNotBlank() && !isValidEmail(value)) {
              return "${field.label} is not valid"
            }
          }
          field.type.regexValidators?.forEach { validator ->
            val regex = Regex(validator.regex.removeSurrounding("/"))
            if (!regex.matches(value)) {
              return validator.message
            }
          }
        }
      }
    }
    return null
  }

  private fun isCardBrandSupported(selectedCardScheme: String, bffCardInfo: BffCardInfo?): Boolean {
    val brands = bffCardInfo?.brands.orEmpty()
    if (brands.isEmpty()) return true
    val normalizedScheme = normalizeCardBrand(selectedCardScheme)
    return brands.any { normalizeCardBrand(it.name) == normalizedScheme }
  }

  private fun normalizeCardBrand(value: String): String {
    return value.lowercase().replace(Regex("[^a-z0-9]"), "")
  }
}
