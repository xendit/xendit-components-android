package co.xendit.components.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.xendit.components.data.model.BffCardInfo
import co.xendit.components.data.model.CardDetails
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.Country
import co.xendit.components.data.model.FieldType
import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.data.model.primaryChannelPropertyKey
import co.xendit.components.ui.components.molecule.CVCField
import co.xendit.components.ui.components.molecule.CardNumberField
import co.xendit.components.ui.components.molecule.CountryField
import co.xendit.components.ui.components.molecule.ExpiryDateField
import co.xendit.components.ui.components.molecule.InstallmentPlanField
import co.xendit.components.ui.components.molecule.PhoneNumberField
import co.xendit.components.ui.components.molecule.ProvinceField
import co.xendit.components.ui.components.molecule.XenditTextField
import co.xendit.components.ui.helper.FormChecker.validateField
import co.xendit.components.ui.helper.toLabelDisplay
import co.xendit.components.ui.style.xenditAppearance

@Composable
internal fun DynamicForm(
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
  val onValuesChangedRef = rememberUpdatedState(onValuesChanged)
  val onCardNumberChangedRef = rememberUpdatedState(onCardNumberChanged)
  val onVisibleFieldsChangedRef = rememberUpdatedState(onVisibleFieldsChanged)

  val formValues = remember { mutableStateMapOf<String, String>() }
  val formErrors = remember { mutableStateMapOf<String, String?>() }

  LaunchedEffect(mockData) {
    formValues.clear()
    formValues.putAll(mockData)
  }

  val filteredFields =
    remember(fields, cardDetails, formValues.toMap(), installmentPlans) {
      filterFormFields(fields, cardDetails, formValues, installmentPlans)
    }

  LaunchedEffect(cardDetails) {
    if (cardDetails != null && cardDetails.countryCodes.isNotEmpty()) {
      val resolvedCountryCode = cardDetails.countryCodes.first()
      val resolvedCountry = Country.fromCode(resolvedCountryCode)

      if (resolvedCountry != null) {
        fields.forEach { field ->
          val propertyKey = field.primaryChannelPropertyKey()

          if (propertyKey.isNotEmpty()) {
            when (field.type) {
              is FieldType.Country -> {
                formValues[propertyKey] = resolvedCountry.code
                onValuesChangedRef.value(formValues.toMap())
              }

              is FieldType.PhoneNumber -> {
                val currentPhone = formValues[propertyKey] ?: ""
                val countryCodeKey = "${propertyKey}_country_code"

                val isEffectivelyEmpty = currentPhone.isBlank()

                if (isEffectivelyEmpty) {
                  formValues[countryCodeKey] = resolvedCountry.code
                  onValuesChangedRef.value(formValues.toMap())
                }
              }

              else -> {}
            }
          }
        }
      }
    }
  }

  // Initialize form values
  LaunchedEffect(filteredFields, installmentPlans) {
    onVisibleFieldsChangedRef.value(filteredFields)
    filteredFields.forEach { field ->
      val propertyKey = field.primaryChannelPropertyKey()
      if (propertyKey.isNotEmpty()) {
        if (field.type is FieldType.InstallmentPlan && !installmentPlans.isNullOrEmpty()) {
          val currentVal = formValues[propertyKey]
          if (currentVal.isNullOrEmpty() || installmentPlans.none { it.terms.toString() == currentVal }) {
            formValues[propertyKey] = installmentPlans.first().terms.toString()
          }
        } else if (!formValues.containsKey(propertyKey)) {
          formValues[propertyKey] = field.initialValue ?: ""
        }

        if (field.type is FieldType.PhoneNumber) {
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

    onValuesChangedRef.value(formValues.toMap())
  }

  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val handleValueChange =
      remember(formValues, formErrors, onValuesChangedRef, onCardNumberChangedRef) {
        { changedField: ChannelFormField, key: String, value: String ->
          formValues[key] = value
          formErrors[key] = validateField(changedField, value)
          onValuesChangedRef.value(formValues.toMap())
          if (changedField.type is FieldType.CreditCardNumber) {
            onCardNumberChangedRef.value(value)
          }
        }
      }
    val renderContext =
      remember(filteredFields, cardDetails, bffCardInfo, installmentPlans, appearance, handleValueChange) {
        DynamicFormRenderContext(
          allFields = filteredFields,
          values = formValues,
          errors = formErrors,
          cardDetails = cardDetails,
          bffCardInfo = bffCardInfo,
          installmentPlans = installmentPlans,
          appearance = appearance,
          onFieldValueChange = handleValueChange
        )
      }

    var fieldIndex = 0
    while (fieldIndex < filteredFields.size) {
      val startField = filteredFields[fieldIndex]
      val startsGroup = startField.groupLabel != null

      if (startsGroup) {
        Text(
          text = startField.groupLabel.orEmpty(),
          style = MaterialTheme.typography.titleMedium,
          color = appearance.colorText,
          modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        val (groupFields, nextFieldIndex) = collectDynamicFormGroupFields(filteredFields, fieldIndex)
        val listPropertyKey = groupFields.map { it.primaryChannelPropertyKey() }
        val filteredFormError = formErrors.filterKeys { it in listPropertyKey }
        val groupHaveError = filteredFormError.any { !it.value.isNullOrEmpty() }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = if (groupHaveError) 2.dp else 1.dp,
              color = if (groupHaveError) appearance.colorDanger else appearance.colorBorder,
              shape = RoundedCornerShape(appearance.borderRadius)
            )
            .background(appearance.colorBackground)
        ) {
          var groupFieldIndex = 0
          while (groupFieldIndex < groupFields.size) {
            groupFieldIndex +=
              renderDynamicFormFieldOrTwoColumnRow(
                fields = groupFields,
                index = groupFieldIndex,
                rowModifier = Modifier
                  .fillMaxWidth()
                  .height(IntrinsicSize.Min),
                singleNoBorder = true,
                isDisplayError = false,
                context = renderContext
              )

            if (groupFieldIndex < groupFields.size) {
              HorizontalDivider(
                thickness = 1.dp,
                color = appearance.colorBorder
              )
            }
          }
        }
        DynamicFormErrorDisplay(
          modifier = Modifier.padding(start = 8.dp),
          filteredFormError = filteredFormError,
          appearance = appearance
        )
        fieldIndex = nextFieldIndex
      } else {
        fieldIndex +=
          renderDynamicFormFieldOrTwoColumnRow(
            fields = filteredFields,
            index = fieldIndex,
            rowModifier = Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min)
              .border(
                width = 1.dp,
                color = appearance.colorBorder,
                shape = RoundedCornerShape(appearance.borderRadius)
              ),
            singleNoBorder = false,
            isDisplayError = true,
            context = renderContext
          )
      }
    }
  }
}

private fun canRenderDynamicFormAsTwoColumnRow(fields: List<ChannelFormField>, index: Int): Boolean {
  return fields[index].span == 1 && index + 1 < fields.size && fields[index + 1].span == 1
}

private data class DynamicFormRenderContext(
  val allFields: List<ChannelFormField>,
  val values: Map<String, String>,
  val errors: Map<String, String?>,
  val cardDetails: CardDetails?,
  val bffCardInfo: BffCardInfo?,
  val installmentPlans: List<InstallmentPlan>?,
  val appearance: co.xendit.components.ui.style.XenditAppearance,
  val onFieldValueChange: (ChannelFormField, String, String) -> Unit
)

private fun collectDynamicFormGroupFields(
  allFields: List<ChannelFormField>,
  groupStartIndex: Int
): Pair<List<ChannelFormField>, Int> {
  val startField = allFields[groupStartIndex]
  val groupLabel = startField.groupLabel

  val collected = mutableListOf<ChannelFormField>()
  var scanIndex = groupStartIndex
  while (scanIndex < allFields.size) {
    val candidate = allFields[scanIndex]
    if (scanIndex > groupStartIndex) {
      val startsNewGroup = candidate.groupLabel != null && candidate.groupLabel != groupLabel

      val prevIndex = scanIndex - 1
      val isFollowPrevField = if (prevIndex >= 0) {
        candidate.span == 1 && allFields[prevIndex].groupLabel != null
      } else false

      val isJoinContinuation = candidate.join == true || isFollowPrevField

      if (startsNewGroup || (!isJoinContinuation && candidate.groupLabel == null)) {
        break
      }
    }
    collected.add(candidate)
    scanIndex++
  }

  return collected to scanIndex
}

@Composable
private fun DynamicFormErrorDisplay(
  modifier: Modifier,
  filteredFormError: Map<String, String?>,
  appearance: co.xendit.components.ui.style.XenditAppearance
) {
  Column(modifier = modifier) {
    val firstError = filteredFormError.values.firstOrNull { !it.isNullOrEmpty() }
    if (firstError != null) {
      Text(
        text = firstError,
        style = MaterialTheme.typography.labelSmall,
        color = appearance.colorDanger,
        modifier = modifier.padding(top = 2.dp)
      )
    }
  }
}

@Composable
private fun DynamicFormFieldItem(
  field: ChannelFormField,
  context: DynamicFormRenderContext,
  noBorder: Boolean,
  isDisplayError: Boolean,
) {
  FormFieldItem(
    field = field,
    allFields = context.allFields,
    values = context.values,
    errors = if (isDisplayError) context.errors else emptyMap(),
    onValueChange = { key, value -> context.onFieldValueChange(field, key, value) },
    cardDetails = context.cardDetails,
    bffCardInfo = context.bffCardInfo,
    installmentPlans = context.installmentPlans,
    noBorder = noBorder
  )
}

@Composable
private fun DynamicFormTwoColumnRow(
  leftField: ChannelFormField,
  rightField: ChannelFormField,
  modifier: Modifier,
  dividerThickness: Dp,
  context: DynamicFormRenderContext,
  isDisplayError: Boolean,
) {
  Row(modifier = modifier) {
    Box(modifier = Modifier.weight(1f)) {
      DynamicFormFieldItem(
        field = leftField,
        context = context,
        noBorder = true,
        isDisplayError = isDisplayError
      )
    }
    VerticalDivider(
      thickness = dividerThickness,
      color = context.appearance.colorBorder
    )
    Box(modifier = Modifier.weight(1f)) {
      DynamicFormFieldItem(
        field = rightField,
        context = context,
        noBorder = true,
        isDisplayError = isDisplayError
      )
    }
  }
}

@Composable
private fun renderDynamicFormFieldOrTwoColumnRow(
  fields: List<ChannelFormField>,
  index: Int,
  rowModifier: Modifier,
  singleNoBorder: Boolean,
  isDisplayError: Boolean,
  context: DynamicFormRenderContext
): Int {
  return if (canRenderDynamicFormAsTwoColumnRow(fields, index)) {
    DynamicFormTwoColumnRow(
      leftField = fields[index],
      rightField = fields[index + 1],
      modifier = rowModifier,
      dividerThickness = 1.dp,
      context = context,
      isDisplayError = isDisplayError
    )
    2
  } else {
    DynamicFormFieldItem(
      field = fields[index],
      context = context,
      noBorder = singleNoBorder,
      isDisplayError = isDisplayError
    )
    1
  }
}

/**
 * Filters form fields based on card details and form values.
 * Fields with flags.require_billing_information = true are only shown when showBillingDetailsFields is true.
 */
private fun filterFormFields(
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
    if (field.type is FieldType.InstallmentPlan) {
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
private fun FormFieldItem(
  field: ChannelFormField,
  allFields: List<ChannelFormField>,
  values: Map<String, String>,
  errors: Map<String, String?>,
  onValueChange: (String, String) -> Unit,
  cardDetails: CardDetails? = null,
  bffCardInfo: BffCardInfo? = null,
  installmentPlans: List<InstallmentPlan>? = null,
  shape: Shape? = null,
  noBorder: Boolean = false
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

  when (val fieldType = field.type) {
    is FieldType.CreditCardNumber -> {
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
        shape = shape,
        noBorder = noBorder
      )
    }

    is FieldType.CreditCardExpiry -> {
      ExpiryDateField(
        value = currentValue,
        placeholder = field.placeholder,
        onValueChange = { onValueChange(propertyKey, it) },
        label = labelDisplay,
        isError = isError,
        errorMessage = errorMessage,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        noBorder = noBorder
      )
    }

    is FieldType.CreditCardCvn -> {
      CVCField(
        value = currentValue,
        onValueChange = { onValueChange(propertyKey, it) },
        label = labelDisplay,
        isError = isError,
        errorMessage = errorMessage,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        noBorder = noBorder
      )
    }

    is FieldType.PhoneNumber -> {
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
        shape = shape,
        noBorder = noBorder
      )
    }

    is FieldType.Country -> {
      CountryField(
        value = currentValue,
        label = labelDisplay,
        onValueChange = { onValueChange(propertyKey, it) },
        placeholder = field.placeholder,
        isError = isError,
        errorMessage = errorMessage,
        modifier = Modifier.fillMaxWidth(),
        noBorder = noBorder
      )
    }

    is FieldType.Province -> {
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
        shape = shape,
        noBorder = noBorder
      )
    }

    is FieldType.InstallmentPlan -> {
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
        shape = shape,
        noBorder = noBorder
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
          when (field.type) {
            is FieldType.Text -> {
              if (field.type.numeric == true) {
                KeyboardOptions(keyboardType = KeyboardType.Number)
              } else {
                KeyboardOptions.Default
              }
            }
            else -> {
              KeyboardOptions.Default
            }
          },
        singleLine = true,
        shape = shape,
        noBorder = noBorder
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
    if (previousField.type is FieldType.Country) {
      val key = previousField.primaryChannelPropertyKey()
      val v = values[key]
      if (!v.isNullOrBlank()) return v
    }
  }
  val anyCountryField = allFields.firstOrNull { it.type is FieldType.Country }
  if (anyCountryField != null) {
    val key = anyCountryField.primaryChannelPropertyKey()
    val v = values[key]
    if (!v.isNullOrBlank()) return v
  }
  return null
}
