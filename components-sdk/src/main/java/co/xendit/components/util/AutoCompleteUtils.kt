package co.xendit.components.util

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import co.xendit.components.data.model.AutofillHintConstants
import co.xendit.components.data.model.FieldType

internal val FieldType.defaultAutofillHints: List<String>
  get() = when (this) {
    is FieldType.CreditCardNumber -> listOf(AutofillHintConstants.CREDIT_CARD_NUMBER)
    is FieldType.CreditCardExpiry -> listOf(AutofillHintConstants.CREDIT_CARD_EXPIRATION_DATE)
    is FieldType.CreditCardCvn -> listOf(AutofillHintConstants.CREDIT_CARD_SECURITY_CODE)
    is FieldType.PhoneNumber -> listOf(AutofillHintConstants.PHONE_NUMBER)
    is FieldType.Email -> listOf(AutofillHintConstants.EMAIL_ADDRESS)
    is FieldType.PostalCode -> listOf(AutofillHintConstants.POSTAL_CODE)
    is FieldType.Country -> listOf(AutofillHintConstants.COUNTRY_NAME)
    is FieldType.Province -> listOf(AutofillHintConstants.ADDRESS_REGION)
    is FieldType.InstallmentPlan -> emptyList()
    is FieldType.Dropdown -> emptyList()
    is FieldType.Text -> resolveTextAutofillHints(this)
  }

internal fun resolveTextKeyboardOptions(textType: FieldType.Text): KeyboardOptions {
  val autocomplete = textType.autocomplete
  val keyboardType = when {
    textType.numeric == true -> KeyboardType.Number
    autocomplete == "email" -> KeyboardType.Email
    autocomplete == "phone_number" || autocomplete == "tel" -> KeyboardType.Phone
    else -> KeyboardType.Text
  }
  val capitalization = when (autocomplete) {
    "given-name", "family-name",
    "address-line1", "address-line2", "address-line3",
    "address-level1", "address-level2",
    "street-address", "billing-address" ->
      KeyboardCapitalization.Words
    "email", "phone_number", "tel", "off" -> KeyboardCapitalization.None
    else -> {
      if (textType.numeric == true) KeyboardCapitalization.None
      else KeyboardCapitalization.Sentences
    }
  }
  return KeyboardOptions(
    keyboardType = keyboardType,
    capitalization = capitalization,
    autoCorrectEnabled = true,
    imeAction = ImeAction.Next
  )
}

private fun resolveTextAutofillHints(textType: FieldType.Text): List<String> {
  return when (textType.autocomplete) {
    "email" -> listOf(AutofillHintConstants.EMAIL_ADDRESS)
    "phone_number", "tel" -> listOf(AutofillHintConstants.PHONE_NUMBER)
    "given-name" -> listOf(AutofillHintConstants.PERSON_NAME_GIVEN)
    "family-name" -> listOf(AutofillHintConstants.PERSON_NAME_FAMILY)
    "address-line1", "address-line2", "address-line3",
    "street-address", "billing-address" ->
      listOf(AutofillHintConstants.ADDRESS_STREET)
    "address-level2" -> listOf(AutofillHintConstants.ADDRESS_LOCALITY)
    "address-level1" -> listOf(AutofillHintConstants.ADDRESS_REGION)
    else -> {
      if (textType.numeric == true) emptyList()
      else when (textType.name) {
        "phone_number" -> listOf(AutofillHintConstants.PHONE_NUMBER)
        "email" -> listOf(AutofillHintConstants.EMAIL_ADDRESS)
        "postal_code" -> listOf(AutofillHintConstants.POSTAL_CODE)
        else -> emptyList()
      }
    }
  }
}