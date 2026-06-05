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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.xendit.components.ui.style.xenditAppearance

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
  maxLength: Int = Int.MAX_VALUE,
  noBorder: Boolean = false,
  leadingIcon: (@Composable (() -> Unit))? = null,
  trailingIcon: (@Composable (() -> Unit))? = null,
  disabledTextColor: Color? = null,
) {
  val appearance = xenditAppearance
  val interactionSource = remember { MutableInteractionSource() }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .then(modifier)
  ) {
    // Label Above
    if (label != null && labelPlacement == XenditTextFieldLabelPlacement.Above && !noBorder) {
      Text(text = label, style = MaterialTheme.typography.bodyLarge, color = appearance.colorText)
      Spacer(modifier = Modifier.height(8.dp))
    }

    BasicTextField(
      value = value,
      onValueChange = { if (it.length <= maxLength) onValueChange(it) },
      modifier = Modifier.fillMaxWidth(),
      enabled = enabled,
      readOnly = readOnly,
      textStyle = textStyle.copy(color = appearance.colorText),
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
          supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage, color = appearance.colorDanger) }
          } else null,
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
  }
}
