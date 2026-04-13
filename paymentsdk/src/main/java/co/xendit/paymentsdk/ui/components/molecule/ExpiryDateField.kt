package co.xendit.paymentsdk.ui.components.molecule

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import co.xendit.paymentsdk.util.GroupedDigitsTransformation

/** Expiry date input field with visual transformation */
@Composable
fun ExpiryDateField(
  modifier: Modifier = Modifier,
  value: String,
  label: String? = null,
  onValueChange: (String) -> Unit,
  isError: Boolean = false,
  errorMessage: String? = null
) {
  XenditTextField(
    value = value,
    onValueChange = { newValue ->
      val digitsOnly = newValue.filter { it.isDigit() }
      if (digitsOnly.length <= 4) {
        onValueChange(digitsOnly)
      }
    },
    label = label,
    placeholder = "MM/YY",
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    visualTransformation = GroupedDigitsTransformation(groupSize = 2, separator = '/'),
    singleLine = true,
    isError = isError,
    modifier = modifier,
    errorMessage = errorMessage
  )
}
