package co.xendit.paymentsdk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.xendit.paymentsdk.data.model.BffCardInfo
import co.xendit.paymentsdk.data.model.CardDetails
import co.xendit.paymentsdk.data.model.ChannelFormField
import co.xendit.paymentsdk.data.model.Country
import co.xendit.paymentsdk.data.model.InstallmentPlan
import co.xendit.paymentsdk.data.model.primaryChannelPropertyKey
import co.xendit.paymentsdk.ui.components.molecule.CVCField
import co.xendit.paymentsdk.ui.components.molecule.CardNumberField
import co.xendit.paymentsdk.ui.components.molecule.CountryField
import co.xendit.paymentsdk.ui.components.molecule.ExpiryDateField
import co.xendit.paymentsdk.ui.components.molecule.InstallmentPlanField
import co.xendit.paymentsdk.ui.components.molecule.PhoneNumberField
import co.xendit.paymentsdk.ui.components.molecule.ProvinceField
import co.xendit.paymentsdk.ui.components.molecule.XenditTextField
import co.xendit.paymentsdk.ui.helper.FormChecker.validateField
import co.xendit.paymentsdk.ui.helper.toLabelDisplay
import co.xendit.paymentsdk.ui.style.xenditAppearance
import co.xendit.paymentsdk.ui.ui_util.CustomShape.customCornersShapeLeft
import co.xendit.paymentsdk.ui.ui_util.CustomShape.customCornersShapeRight

@Composable
fun DynamicForm(
  fields: List<ChannelFormField>,
  cardDetails: CardDetails?,
  onValuesChanged: (Map<String, String>) -> Unit,
  onCardNumberChanged: (String) -> Unit,
  onVisibleFieldsChanged: (List<ChannelFormField>) -> Unit,
  bffCardInfo: BffCardInfo? = null,
  installmentPlans: List<InstallmentPlan>? = null,
  mockData: Map<String, String> = mapOf()
) {
  val appearance = xenditAppearance

  val formValues = remember { mutableStateMapOf<String, String>() }
  val formErrors = remember { mutableStateMapOf<String, String?>() }

  LaunchedEffect(mockData) {
    formValues.clear()
    formValues.putAll(mockData)
  }
  // Filter fields based on card details and form values
  val filteredFields =
    remember(fields, cardDetails, formValues.toMap(), installmentPlans) {
      filterFormFields(fields, cardDetails, formValues, installmentPlans)
    }

  // Handle cardDetails changes to update country and phone number
  LaunchedEffect(cardDetails) {
    if (cardDetails != null && cardDetails.countryCodes.isNotEmpty()) {
      val resolvedCountryCode = cardDetails.countryCodes.first()
      val resolvedCountry = Country.fromCode(resolvedCountryCode)

      if (resolvedCountry != null) {
        fields.forEach { field ->
          val propertyKey = field.primaryChannelPropertyKey()

          if (propertyKey.isNotEmpty()) {
            when (field.type.name) {
              "country" -> {
                // Update country if card resolves it
                formValues[propertyKey] = resolvedCountry.code
                onValuesChanged(formValues.toMap())
              }

              "phone_number" -> {
                // Update phone number country code ONLY if user hasn't typed local number yet
                val currentPhone = formValues[propertyKey] ?: ""
                val countryCodeKey = "${propertyKey}_country_code"

                // Check if current phone is empty
                val isEffectivelyEmpty = currentPhone.isBlank()

                if (isEffectivelyEmpty) {
                  formValues[countryCodeKey] = resolvedCountry.code
                  onValuesChanged(formValues.toMap())
                }
              }
            }
          }
        }
      }
    }
  }

  // Initialize form values
  LaunchedEffect(filteredFields, installmentPlans) {
    onVisibleFieldsChanged(filteredFields)
    filteredFields.forEach { field ->
      val propertyKey = field.primaryChannelPropertyKey()
      if (propertyKey.isNotEmpty()) {
        if (field.type.name == "installment_plan" && !installmentPlans.isNullOrEmpty()) {
          val currentVal = formValues[propertyKey]
          if (currentVal.isNullOrEmpty() || installmentPlans.none { it.terms.toString() == currentVal }) {
            formValues[propertyKey] = installmentPlans.first().terms.toString()
          }
        } else if (!formValues.containsKey(propertyKey)) {
          formValues[propertyKey] = field.initialValue ?: ""
        }

        if (field.type.name == "phone_number") {
          val countryCodeKey = "${propertyKey}_country_code"
          if (!formValues.containsKey(countryCodeKey)) {
            // Default to ID or first country if no initial country code
            formValues[countryCodeKey] =
              Country.fromCode("ID")?.code ?: Country.countries.first().code
          }
        }
        if (!formErrors.containsKey(propertyKey)) {
          formErrors[propertyKey] = null
        }
      }
    }

    onValuesChanged(formValues.toMap())
  }

  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    var i = 0
    while (i < filteredFields.size) {
      val field = filteredFields[i]

      // Handle group label
      if (field.groupLabel != null) {
        Text(
          text = field.groupLabel,
          style = MaterialTheme.typography.titleMedium,
          color = appearance.colorText ?: MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
      }

      // Detect if we can combine two fields in one row (both span 1 and consecutive)
      if (field.span == 1 && i + 1 < filteredFields.size && filteredFields[i + 1].span == 1) {
        Row(
          modifier = Modifier.fillMaxWidth(),
        ) {
          val field1 = filteredFields[i]
          val field2 = filteredFields[i + 1]

          Box(modifier = Modifier.weight(1f)) {
            FormFieldItem(
              field = field1,
              allFields = filteredFields,
              values = formValues,
              errors = formErrors,
              onValueChange = { key, value ->
                formValues[key] = value
                formErrors[key] = validateField(field1, value)
                onValuesChanged(formValues.toMap())
                // Trigger card number change callback for card number fields
                if (field1.type.name == "credit_card_number") {
                  onCardNumberChanged(value)
                }
              },
              cardDetails = cardDetails,
              bffCardInfo = bffCardInfo,
              installmentPlans = installmentPlans,
              shape = customCornersShapeLeft
            )
          }
          Box(modifier = Modifier.weight(1f)) {
            FormFieldItem(
              field = field2,
              allFields = filteredFields,
              values = formValues,
              errors = formErrors,
              onValueChange = { key, value ->
                formValues[key] = value
                formErrors[key] = validateField(field2, value)
                onValuesChanged(formValues.toMap())
                // Trigger card number change callback for card number fields
                if (field2.type.name == "credit_card_number") {
                  onCardNumberChanged(value)
                }
              },
              cardDetails = cardDetails,
              bffCardInfo = bffCardInfo,
              installmentPlans = installmentPlans,
              shape = customCornersShapeRight
            )
          }
        }
        i += 2
      } else {
        FormFieldItem(
          field = field,
          allFields = filteredFields,
          values = formValues,
          errors = formErrors,
          onValueChange = { key, value ->
            formValues[key] = value
            formErrors[key] = validateField(field, value)
            onValuesChanged(formValues.toMap())
            // Trigger card number change callback for card number fields
            if (field.type.name == "credit_card_number") {
              onCardNumberChanged(value)
            }
          },
          cardDetails = cardDetails,
          bffCardInfo = bffCardInfo,
          installmentPlans = installmentPlans
        )
        i++
      }
    }
  }
}

/**
 * Filters form fields based on card details and form values.
 * Fields with flags.require_billing_information = true are only shown when showBillingDetailsFields is true.
 * Fields with display_if conditions are filtered based on the current form values.
 */
fun filterFormFields(
  fields: List<ChannelFormField>,
  cardDetails: CardDetails?,
  formValues: Map<String, String>,
  installmentPlans: List<InstallmentPlan>? = null
): List<ChannelFormField> {
  val showBillingDetailsFields = cardDetails?.requireBillingInformation ?: false
  return fields.filter { field ->
    // Check billing information requirement
    val requiresBillingInfo = field.flags?.get("require_billing_information") as? Boolean ?: false
    if (requiresBillingInfo && !showBillingDetailsFields) {
      return@filter false
    }

    // Check installment plan logic
    if (field.type.name == "installment_plan") {
      if (installmentPlans.isNullOrEmpty()) {
        return@filter false
      }
    }

    // Check display_if conditions
    val displayIf = field.displayIf
    if (!displayIf.isNullOrEmpty()) {
      for (condition in displayIf) {
        if (condition.size < 3) continue
        val property = condition[0]
        val operator = condition[1]
        val expectedValue = condition[2]

        val actualValue = formValues[property] ?: ""

        when (operator) {
          "equals" -> if (actualValue != expectedValue) return@filter false
          "not_equals" -> if (actualValue == expectedValue) return@filter false
        }
      }
    }

    true
  }
}

@Composable
fun FormFieldItem(
  field: ChannelFormField,
  allFields: List<ChannelFormField>,
  values: Map<String, String>,
  errors: Map<String, String?>,
  onValueChange: (String, String) -> Unit,
  cardDetails: CardDetails? = null,
  bffCardInfo: BffCardInfo? = null,
  installmentPlans: List<InstallmentPlan>? = null,
  shape: Shape? = null,
) {
  val appearance = xenditAppearance
  val propertyKey = field.primaryChannelPropertyKey()

  val currentValue = values[propertyKey] ?: ""
  val errorMessage = errors[propertyKey]
  val isError = errorMessage != null
  val labelDisplay = if (field.span == 2) {
    field.label
  } else {
    null
  }

  when (field.type.name) {
    "credit_card_number" -> {
      val selectedScheme = cardDetails?.schemes?.firstOrNull()
      val logoUrl = if (selectedScheme != null) {
        bffCardInfo?.brands?.firstOrNull {
          it.name.equals(
            selectedScheme,
            ignoreCase = true
          )
        }?.logoUrl
      } else null

      CardNumberField(
        value = currentValue,
        onValueChange = { onValueChange(propertyKey, it) },
        isError = isError,
        errorMessage = errorMessage,
        logoUrl = logoUrl,
        modifier = Modifier.fillMaxWidth(),
        shape = shape
      )
    }

    "credit_card_expiry" -> {
      ExpiryDateField(
        value = currentValue,
        label = labelDisplay,
        onValueChange = { onValueChange(propertyKey, it) },
        isError = isError,
        errorMessage = errorMessage,
        modifier = Modifier.fillMaxWidth(),
        shape = shape
      )
    }

    "credit_card_cvn" -> {
      CVCField(
        value = currentValue,
        label = labelDisplay,
        onValueChange = { onValueChange(propertyKey, it) },
        isError = isError,
        errorMessage = errorMessage,
        modifier = Modifier.fillMaxWidth(),
        shape = shape
      )
    }

    "phone_number" -> {
      val countryCodeKey = "${propertyKey}_country_code"
      val countryCode = values[countryCodeKey] ?: "ID" // Default fallback

      PhoneNumberField(
        value = currentValue,
        label = labelDisplay,
        onValueChange = { onValueChange(propertyKey, it) },
        countryCode = countryCode,
        onCountryCodeChange = { onValueChange(countryCodeKey, it) },
        placeholder = field.placeholder,
        isError = isError,
        errorMessage = errorMessage,
        modifier = Modifier.fillMaxWidth(),
        shape = shape
      )
    }

    "country" -> {
      CountryField(
        value = currentValue,
        label = labelDisplay,
        onValueChange = { onValueChange(propertyKey, it) },
        placeholder = field.placeholder,
        isError = isError,
        errorMessage = errorMessage,
        modifier = Modifier.fillMaxWidth(),
        shape = shape
      )
    }

    "province", "province_state", "state" -> {
      val countryCode =
        getBestCountryForProvinceField(
          thisField = field,
          allFields = allFields,
          values = values
        )
      ProvinceField(
        value = currentValue,
        label = labelDisplay,
        onValueChange = { onValueChange(propertyKey, it) },
        countryCode = countryCode,
        placeholder = field.placeholder,
        isError = isError,
        errorMessage = errorMessage,
        modifier = Modifier.fillMaxWidth(),
        shape = shape
      )
    }

    "installment_plan" -> {
      val selectedPlan = installmentPlans?.find { it.terms.toString() == currentValue }

      InstallmentPlanField(
        plans = installmentPlans ?: emptyList(),
        selectedPlanDesc = selectedPlan?.toLabelDisplay()?.asString() ?: "",
        onPlanSelected = { plan ->
          onValueChange(propertyKey, plan.terms.toString())
        },
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        errorMessage = errorMessage,
        shape = shape
      )
    }

    else -> {
      XenditTextField(
        value = currentValue,
        label = labelDisplay,
        onValueChange = { onValueChange(propertyKey, it) },
        placeholder = field.placeholder,
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        errorMessage = errorMessage,
        keyboardOptions =
          when (field.type.name) {
            "phone_number" -> KeyboardOptions(keyboardType = KeyboardType.Phone)
            else ->
              if (field.type.numeric == true) {
                KeyboardOptions(keyboardType = KeyboardType.Number)
              } else {
                KeyboardOptions.Default
              }
          },
        singleLine = true,
        shape = shape
      )
    }
  }
}

private fun getBestCountryForProvinceField(
  thisField: ChannelFormField,
  allFields: List<ChannelFormField>,
  values: Map<String, String>
): String? {
  val thisIndex = allFields.indexOf(thisField)
  if (thisIndex > 0) {
    val previousField = allFields[thisIndex - 1]
    if (previousField.type.name == "country") {
      val key = previousField.primaryChannelPropertyKey()
      val v = values[key]
      if (!v.isNullOrBlank()) return v
    }
  }
  val anyCountryField = allFields.firstOrNull { it.type.name == "country" }
  if (anyCountryField != null) {
    val key = anyCountryField.primaryChannelPropertyKey()
    val v = values[key]
    if (!v.isNullOrBlank()) return v
  }
  return null
}
