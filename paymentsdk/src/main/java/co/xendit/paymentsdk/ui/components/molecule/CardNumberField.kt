package co.xendit.paymentsdk.ui.components.molecule

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.xendit.paymentsdk.ui.helper.SdkImageLoader
import co.xendit.paymentsdk.ui.style.XenditAppearance
import co.xendit.paymentsdk.ui.style.xenditAppearance
import co.xendit.paymentsdk.util.GroupedDigitsTransformation
import coil.compose.AsyncImage

/** Card number input field with visual transformation */
@Composable
fun CardNumberField(
  modifier: Modifier = Modifier,
  value: String,
  label: String? = null,
  onValueChange: (String) -> Unit,
  isError: Boolean = false,
  errorMessage: String? = null,
  logoUrl: String? = null,
  shape: Shape? = null,
  noBorder: Boolean = false,
) {
  val context = LocalContext.current
  val imageLoader = remember { SdkImageLoader.get(context) }
  val groupedDigitsTransformation =
    remember { GroupedDigitsTransformation(groupSize = 4, maxDigits = 16) }

  XenditTextField(
    modifier = modifier,
    value = value,
    onValueChange = { newValue ->
      val digitsOnly = newValue.filter { it.isDigit() }
      if (digitsOnly.length <= 16) {
        onValueChange(digitsOnly)
      }
    },
    label = label,
    placeholder = "1234 5678 9012 3456",
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    visualTransformation = groupedDigitsTransformation,
    singleLine = true,
    isError = isError,
    errorMessage = errorMessage,
    trailingIcon =
      if (logoUrl != null) {
        {
          AsyncImage(
            model = logoUrl,
            imageLoader = imageLoader,
            contentDescription = "Card Brand Logo",
            modifier = Modifier
              .width(36.dp)
              .height(24.dp)
              .padding(end = 8.dp)
          )
        }
      } else null,
    shape = shape,
    noBorder = noBorder
  )
}
