package co.xendit.paymentsdk.ui.components.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
fun XenditOutlineTextField(
  modifier: Modifier = Modifier,
  value: String,
  textStyle : TextStyle = MaterialTheme.typography.bodyLarge,
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
  leadingIcon: (@Composable (() -> Unit))? = null,
  trailingIcon: (@Composable (() -> Unit))? = null
) {
  val appearance = xenditAppearance

  Column(modifier = Modifier.fillMaxWidth().then(modifier)) {
    if (label != null && labelPlacement == XenditTextFieldLabelPlacement.Above) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = appearance.colorText ?: MaterialTheme.colorScheme.onSurface
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
          { Text(label, color = appearance.colorText ?: MaterialTheme.colorScheme.onSurface) }
        } else {
          null
        },
      placeholder =
        if (placeholder != null) {
          {
            Text(
              placeholder,
              color = appearance.colorTextPlaceholder ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          null
        },
      supportingText =
        if (isError && errorMessage != null) {
          { Text(errorMessage, color = appearance.colorDanger ?: MaterialTheme.colorScheme.error) }
        } else {
          null
        },
      shape = shape ?: MaterialTheme.shapes.small,
//      colors = OutlinedTextFieldDefaults.colors(
//        focusedBorderColor = Color.Transparent,
//        unfocusedBorderColor = Color.Transparent,
//        disabledBorderColor = Color.Transparent,
//        errorBorderColor = Color.Transparent,
//      )
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedBorderColor = appearance.colorBorder ?: MaterialTheme.colorScheme.outline,
          unfocusedBorderColor = appearance.colorBorder ?: MaterialTheme.colorScheme.outline,
          errorBorderColor = appearance.colorDanger ?: MaterialTheme.colorScheme.error,
          focusedTextColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
          unfocusedTextColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
          disabledTextColor =
            (appearance.colorText ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.5f),
          errorTextColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
          focusedLabelColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
          unfocusedLabelColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
          disabledLabelColor =
            (appearance.colorText ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.5f),
          errorLabelColor = appearance.colorDanger ?: MaterialTheme.colorScheme.error,
          cursorColor = appearance.colorPrimary ?: MaterialTheme.colorScheme.primary,
          errorCursorColor = appearance.colorDanger ?: MaterialTheme.colorScheme.error
        )
    )
  }
}

@Composable
fun XenditTextField(
  modifier: Modifier = Modifier,
  value: String,
  textStyle : TextStyle = MaterialTheme.typography.bodyLarge,
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
  leadingIcon: (@Composable (() -> Unit))? = null,
  trailingIcon: (@Composable (() -> Unit))? = null
) {
  val appearance = xenditAppearance

  Column(modifier = Modifier.fillMaxWidth().then(modifier)) {
    if (label != null && labelPlacement == XenditTextFieldLabelPlacement.Above) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = appearance.colorText ?: MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(8.dp))
    }

    TextField(
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
          { Text(label, color = appearance.colorText ?: MaterialTheme.colorScheme.onSurface) }
        } else {
          null
        },
      placeholder =
        if (placeholder != null) {
          {
            Text(
              placeholder,
              color = appearance.colorTextPlaceholder ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          null
        },
      supportingText =
        if (isError && errorMessage != null) {
          { Text(errorMessage, color = appearance.colorDanger ?: MaterialTheme.colorScheme.error) }
        } else {
          null
        },
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedBorderColor = appearance.colorBorder ?: MaterialTheme.colorScheme.outline,
          unfocusedBorderColor = appearance.colorBorder ?: MaterialTheme.colorScheme.outline,
          errorBorderColor = appearance.colorDanger ?: MaterialTheme.colorScheme.error,
          focusedTextColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
          unfocusedTextColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
          disabledTextColor =
            (appearance.colorText ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.5f),
          errorTextColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
          focusedLabelColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
          unfocusedLabelColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
          disabledLabelColor =
            (appearance.colorText ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.5f),
          errorLabelColor = appearance.colorDanger ?: MaterialTheme.colorScheme.error,
          cursorColor = appearance.colorPrimary ?: MaterialTheme.colorScheme.primary,
          errorCursorColor = appearance.colorDanger ?: MaterialTheme.colorScheme.error
        )
    )
  }
}
