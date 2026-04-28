package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.xendit.components.data.model.BffCardInfo
import co.xendit.components.ui.helper.SdkImageLoader
import co.xendit.components.ui.style.xenditAppearance
import co.xendit.components.util.GroupedDigitsTransformation
import coil.ImageLoader
import coil.compose.AsyncImage

/** Card number input field with visual transformation */
@Composable
internal fun CardNumberField(
  modifier: Modifier = Modifier,
  value: String,
  placeholder: String? = null,
  label: String? = null,
  onValueChange: (String) -> Unit,
  isError: Boolean = false,
  errorMessage: String? = null,
  selectedScheme: String? = null,
  bffCardInfo: BffCardInfo? = null,
  shape: Shape? = null,
  noBorder: Boolean = false,
) {
  val appearance = xenditAppearance
  val context = LocalContext.current
  val imageLoader = remember { SdkImageLoader.get(context) }
  val groupedDigitsTransformation =
    remember { GroupedDigitsTransformation(groupSize = 4, maxDigits = 16) }
  val logoUrl = if (selectedScheme != null) {
    bffCardInfo?.brands?.firstOrNull {
      it.name.equals(
        selectedScheme,
        ignoreCase = true
      )
    }?.logoUrl
  } else null

  Box(
    modifier = modifier,
    contentAlignment = Alignment.CenterEnd // Ensures everything inside stays at the right
  ) {
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
      placeholder = placeholder,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      visualTransformation = groupedDigitsTransformation,
      singleLine = true,
      isError = isError,
      errorMessage = errorMessage,
      shape = shape,
      noBorder = noBorder
    )
    Row(
      modifier = Modifier.padding(end = 12.dp), // Space from the right edge
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (logoUrl != null || value.length > 3) {
        logoUrl?.let {
          CardLogo(
            logoUrl = logoUrl,
            imageLoader = imageLoader,
          )
        }
      } else {
        bffCardInfo?.brands?.forEach { logo ->
          CardLogo(
            logoUrl = logo.logoUrl,
            imageLoader = imageLoader,
          )
        }
      }
    }
  }

}

@Composable
internal fun CardLogo(
  logoUrl: String,
  imageLoader: ImageLoader,
) {
  val appearance = xenditAppearance
  AsyncImage(
    model = logoUrl,
    imageLoader = imageLoader,
    contentDescription = "Card Brand Logo",
    modifier = Modifier
      .width(36.dp)
      .height(24.dp)
      .clip(RoundedCornerShape(4.dp))
      .background(appearance.colorBackground)
      .border(
        width = 1.dp,
        color = appearance.colorBorder,
        shape = RoundedCornerShape(4.dp)
      )
  )
}
