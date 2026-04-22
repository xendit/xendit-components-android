package co.xendit.paymentsdk.ui.helper

import co.xendit.paymentsdk.data.model.ChannelFormField
import co.xendit.paymentsdk.data.model.FieldType
import co.xendit.paymentsdk.data.model.primaryChannelPropertyKey
import co.xendit.paymentsdk.ui.helper.FormCheckerUtil.isValidCardExpiry
import co.xendit.paymentsdk.ui.helper.FormCheckerUtil.isValidCreditCard
import kotlin.collections.forEach

internal object FormChecker {

  fun validateAllField(fields: List<ChannelFormField>, values: Map<String, String>): Boolean {
    fields.forEach { field ->
      if (validateField(field, values[field.primaryChannelPropertyKey()] ?: "") != null) {
        return false
      }
    }
    return true
  }

  fun validateField(field: ChannelFormField, value: String): String? {
    if (field.required && value.isBlank()) {
      return "${field.label} is required"
    }

    when (field.type) {
      is FieldType.CreditCardNumber -> {
        if (!isValidCreditCard(value)) {
          return "Card number is not valid"
        }
      }
      is FieldType.CreditCardExpiry -> {
        if (!isValidCardExpiry(value)) {
          return "Card expiry is not valid"
        }
      }
      else -> {
        if (field.type is FieldType.Text) {
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
}