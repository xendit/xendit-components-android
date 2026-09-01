package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.onFillData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.xendit.components.data.model.AutofillHintConstants
import co.xendit.components.ui.style.xenditAppearance

enum class XenditTextFieldLabelPlacement {
  Above,
  Floating
}

private fun resolveSemanticsContentTypes(hints: List<String>): ContentType? {
  if (hints.isEmpty()) return null
  var result: ContentType? = null
  for (hint in hints) {
    val type = when (hint) {
      AutofillHintConstants.EMAIL_ADDRESS -> ContentType.EmailAddress
      AutofillHintConstants.PHONE_NUMBER -> ContentType.PhoneNumber
      AutofillHintConstants.POSTAL_CODE -> ContentType.PostalCode
      AutofillHintConstants.COUNTRY_NAME -> ContentType.AddressCountry
      AutofillHintConstants.ADDRESS_REGION -> ContentType.AddressRegion
      AutofillHintConstants.ADDRESS_LOCALITY -> ContentType.AddressLocality
      AutofillHintConstants.ADDRESS_STREET -> ContentType.AddressStreet
      AutofillHintConstants.CREDIT_CARD_NUMBER -> ContentType.CreditCardNumber
      AutofillHintConstants.CREDIT_CARD_EXPIRATION_DATE -> ContentType.CreditCardExpirationDate
      AutofillHintConstants.CREDIT_CARD_SECURITY_CODE -> ContentType.CreditCardSecurityCode
      AutofillHintConstants.PERSON_NAME_GIVEN -> ContentType.PersonFirstName
      AutofillHintConstants.PERSON_NAME_FAMILY -> ContentType.PersonLastName
      else -> null
    }
    result = if (result == null) type else type?.let { result + it } ?: result
  }
  return result
}

private fun Modifier.thenAutofillSemantics(
  contentType: ContentType?,
  onFill: (String) -> Unit,
): Modifier {
  if (contentType == null) return this
  return this.semantics {
    this.contentType = contentType
    onFillData { fillableData ->
      val text = fillableData.textValue?.toString()
      if (text != null) {
        onFill(text)
        true
      } else false
    }
  }
}


@Composable
internal fun XenditTextField(
  modifier: Modifier = Modifier,
  value: String,
  textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
  onValueChange: (String) -> Unit,
  label: String? = null,
  labelPlacement: XenditTextFieldLabelPlacement = XenditTextFieldLabelPlacement.Above,
  placeholder: String? = null,
  isError: Boolean = false,
  errorMessage: String? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  singleLine: Boolean = true,
  readOnly: Boolean = false,
  enabled: Boolean = true,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  shape: Shape? = null,
  maxLength: Int = Int.MAX_VALUE,
  noBorder: Boolean = false,
  leadingIcon: (@Composable (() -> Unit))? = null,
  trailingIcon: (@Composable (() -> Unit))? = null,
  disabledTextColor: Color? = null,
  autofillHints: List<String> = emptyList(),
  testTag: String = "",
) {
  val appearance = xenditAppearance
  val interactionSource = remember { MutableInteractionSource() }
  val showErrorText = isError && !errorMessage.isNullOrBlank()
  val semanticsContentType = remember(autofillHints) { resolveSemanticsContentTypes(autofillHints) }
  val onFillValue: (String) -> Unit = remember(maxLength, onValueChange) {
    { newValue -> if (newValue.length <= maxLength) onValueChange(newValue) }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .then(modifier)
  ) {
    // Label Above
    if (label != null && labelPlacement == XenditTextFieldLabelPlacement.Above && !noBorder) {
      Text(text = label, style = MaterialTheme.typography.bodyLarge, color = appearance.colorText)
      Spacer(modifier = Modifier.height(12.dp))
    }

    BasicTextField(
      value = value,
      onValueChange = { if (it.length <= maxLength) onValueChange(it) },
      modifier = Modifier
        .fillMaxWidth()
        .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
        .thenAutofillSemantics(
          contentType = semanticsContentType,
          onFill = onFillValue
        ),
      enabled = enabled,
      readOnly = readOnly,
      textStyle = textStyle.copy(
        color = if (!enabled && disabledTextColor != null) disabledTextColor else appearance.colorText
      ),
      cursorBrush = SolidColor(appearance.colorPrimary),
      interactionSource = interactionSource,
      keyboardOptions = keyboardOptions,
      visualTransformation = visualTransformation,
      singleLine = singleLine,
      decorationBox = { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
          value = value,
          innerTextField = innerTextField,
          enabled = enabled,
          singleLine = singleLine,
          visualTransformation = visualTransformation,
          interactionSource = interactionSource,
          label = if (label != null && labelPlacement == XenditTextFieldLabelPlacement.Floating) {
            { Text(label, color = appearance.colorText) }
          } else null,
          placeholder = if (placeholder != null) {
            {
              Text(
                text = placeholder,
                color = appearance.colorTextPlaceholder,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          } else null,
          leadingIcon = leadingIcon,
          trailingIcon = trailingIcon,
          isError = isError,
          supportingText = null,
          container = {
            OutlinedTextFieldDefaults.Container(
              enabled = enabled,
              isError = isError,
              interactionSource = interactionSource,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (noBorder) Color.Transparent else appearance.colorBorder,
                unfocusedBorderColor = if (noBorder) Color.Transparent else appearance.colorBorder,
                errorBorderColor = if (noBorder) Color.Transparent else appearance.colorDanger,
                disabledBorderColor = if (noBorder) Color.Transparent else MaterialTheme.colorScheme.outline
              ),
              shape = shape ?: MaterialTheme.shapes.small,
              focusedBorderThickness = 1.dp,
              unfocusedBorderThickness = 1.dp,
            )
          }
        )
      }
    )

    if (showErrorText) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = errorMessage.orEmpty(),
        style = MaterialTheme.typography.bodyMedium,
        color = appearance.colorDanger
      )
    }
  }
}
