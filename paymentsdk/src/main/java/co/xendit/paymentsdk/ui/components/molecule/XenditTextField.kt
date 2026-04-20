package co.xendit.paymentsdk.ui.components.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import co.xendit.paymentsdk.ui.style.xenditAppearance

enum class XenditTextFieldLabelPlacement {
  Above,
  Floating
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
  noBorder: Boolean = false,
  leadingIcon: (@Composable (() -> Unit))? = null,
  trailingIcon: (@Composable (() -> Unit))? = null
) {
  val appearance = xenditAppearance

  Column(modifier = Modifier
    .fillMaxWidth()
    .then(modifier)) {
    if (label != null && labelPlacement == XenditTextFieldLabelPlacement.Above && !noBorder) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = appearance.colorText
      )
      Spacer(modifier = Modifier.height(8.dp))
    }

    OutlinedTextField(
      modifier = Modifier.fillMaxWidth(),
      value = value,
      textStyle = textStyle,
      onValueChange = onValueChange,
      readOnly = readOnly,
      enabled = enabled,
      isError = isError,
      singleLine = singleLine,
      keyboardOptions = keyboardOptions,
      visualTransformation = visualTransformation,
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon,
      label =
        if (label != null && labelPlacement == XenditTextFieldLabelPlacement.Floating) {
          { Text(label, color = appearance.colorText) }
        } else {
          null
        },
      placeholder =
        if (placeholder != null) {
          {
            Text(
              placeholder,
              color = appearance.colorTextPlaceholder
            )
          }
        } else {
          null
        },
      supportingText =
        if (isError && errorMessage != null) {
          { Text(errorMessage, color = appearance.colorDanger) }
        } else {
          null
        },
      shape = shape ?: MaterialTheme.shapes.small,
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedBorderColor = if (noBorder) Color.Transparent else appearance.colorBorder,
          unfocusedBorderColor = if (noBorder) Color.Transparent else appearance.colorBorder,
          errorBorderColor = if (noBorder) Color.Transparent else appearance.colorDanger,
          focusedTextColor = appearance.colorText,
          unfocusedTextColor = appearance.colorText,
          disabledTextColor = (appearance.colorText).copy(alpha = 0.5f),
          errorTextColor = appearance.colorText,
          focusedLabelColor = appearance.colorText,
          unfocusedLabelColor = appearance.colorText,
          disabledLabelColor =
            (appearance.colorText).copy(alpha = 0.5f),
          disabledBorderColor = if (noBorder) Color.Transparent else MaterialTheme.colorScheme.outline,
          errorLabelColor = appearance.colorDanger,
          cursorColor = appearance.colorPrimary,
          errorCursorColor = appearance.colorDanger
        )
    )
  }
}
