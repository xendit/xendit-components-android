package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

/** CVC input field (3-4 digits) */
@Composable
internal fun CVCField(
  modifier: Modifier = Modifier,
  value: String,
  label: String? = null,
  onValueChange: (String) -> Unit,
  isError: Boolean = false,
  errorMessage: String? = null,
  shape: Shape? = null,
  noBorder: Boolean = false,
  testTag: String = "",
) {
  XenditTextField(
    value = value,
    onValueChange = { newValue ->
      if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
        onValueChange(newValue)
      }
    },
    label = label,
    placeholder = "123",
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    visualTransformation = PasswordVisualTransformation(),
    singleLine = true,
    isError = isError,
    modifier = modifier,
    errorMessage = errorMessage,
    shape = shape,
    noBorder = noBorder,
    testTag = testTag
  )
}
