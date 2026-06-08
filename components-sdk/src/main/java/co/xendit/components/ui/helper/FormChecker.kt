package co.xendit.components.ui.helper

import co.xendit.components.R
import co.xendit.components.data.model.BffCardInfo
import co.xendit.components.data.model.CardDetails
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.FieldType
import co.xendit.components.data.model.primaryChannelPropertyKey
import co.xendit.components.ui.components.molecule.UiText
import co.xendit.components.ui.helper.FormCheckerUtil.isValidCardExpiry
import co.xendit.components.ui.helper.FormCheckerUtil.isValidCreditCard
import co.xendit.components.ui.helper.FormCheckerUtil.isValidEmail
import co.xendit.components.ui.helper.FormCheckerUtil.isValidPhoneNumber
import kotlin.collections.forEach

internal object FormChecker {

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
  ): UiText? {
    if (field.required && value.isBlank()) {
      return UiText.StringResource(R.string.form_validation_required, field.label)
    }

    when (field.type) {
      is FieldType.CreditCardNumber -> {
        if (!isValidCreditCard(value)) {
          return UiText.StringResource(R.string.sessionvalidation_card_number_invalid)
        }
        val scheme = cardDetails?.schemes?.firstOrNull()
        if (scheme != null && !isCardBrandSupported(scheme, bffCardInfo)) {
          return UiText.StringResource(R.string.card_brand_not_supported)
        }
      }
      is FieldType.CreditCardExpiry -> {
        if (!isValidCardExpiry(value)) {
          return UiText.StringResource(R.string.sessionvalidation_card_expiry_invalid)
        }
      }
      is FieldType.PhoneNumber -> {
        if (value.isNotBlank()) {
          val propertyKey = field.primaryChannelPropertyKey()
          val countryCodeKey = "${propertyKey}_country_code"
          val regionCode = values?.get(countryCodeKey) ?: ""
          if (!isValidPhoneNumber(value, regionCode)) {
            return UiText.StringResource(R.string.form_validation_invalid, field.label)
          }
        }
      }
      is FieldType.Email -> {
        if (value.isNotBlank() && !isValidEmail(value)) {
          return UiText.StringResource(R.string.form_validation_invalid, field.label)
        }
      }
      else -> {
        if (field.type is FieldType.Text) {
          if (field.type.autocomplete == "email") {
            if (value.isNotBlank() && !isValidEmail(value)) {
              return UiText.StringResource(R.string.form_validation_invalid, field.label)
            }
          }
          field.type.regexValidators?.forEach { validator ->
            val regex = Regex(validator.regex.removeSurrounding("/"))
            if (!regex.matches(value)) {
              return UiText.DynamicString(validator.message)
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
