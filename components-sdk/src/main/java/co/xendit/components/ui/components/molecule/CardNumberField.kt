package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
  cardSchemes: List<String>? = null,
  bffCardInfo: BffCardInfo? = null,
  shape: Shape? = null,
  noBorder: Boolean = false,
  testTag: String = "",
) {
  val appearance = xenditAppearance
  val context = LocalContext.current
  val imageLoader = remember { SdkImageLoader.get(context) }
  val groupedDigitsTransformation =
    remember { GroupedDigitsTransformation(groupSize = 4, maxDigits = 19) }
  val brands = bffCardInfo?.brands.orEmpty()
  val selectedCardScheme = cardSchemes?.firstOrNull()
  val candidateLogoUrl = if (selectedCardScheme != null) {
    brands.firstOrNull {
      it.name.equals(
        selectedCardScheme,
        ignoreCase = true
      )
    }?.logoUrl
  } else null

  val minDigitsToResolveScheme = 6
  var lastResolvedLogoUrl by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(candidateLogoUrl) {
    if (candidateLogoUrl != null) {
      lastResolvedLogoUrl = candidateLogoUrl
    }
  }

  LaunchedEffect(value.length) {
    if (value.length < minDigitsToResolveScheme) {
      lastResolvedLogoUrl = null
    }
  }

  val displayLogoUrl = candidateLogoUrl ?: lastResolvedLogoUrl
  val shouldShowSupportedBrands = cardSchemes?.isEmpty() == true && brands.isNotEmpty()
  val showInitialLogos =
    displayLogoUrl == null &&
      value.length < minDigitsToResolveScheme &&
      brands.isNotEmpty()

  XenditTextField(
    modifier = modifier,
    value = value,
    onValueChange = { newValue ->
      val digitsOnly = newValue.filter { it.isDigit() }
      if (digitsOnly.length <= 19) {
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
    noBorder = noBorder,
    testTag = testTag,
    trailingIcon = {
      Row(
        modifier = Modifier.padding(end = 12.dp), // Space from the right edge
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (shouldShowSupportedBrands || showInitialLogos) {
          brands.forEach { logo ->
            CardLogo(
              logoUrl = logo.logoUrl,
              imageLoader = imageLoader,
            )
          }
        } else if (displayLogoUrl != null) {
          CardLogo(
            logoUrl = displayLogoUrl,
            imageLoader = imageLoader,
          )
        }
      }
    }
  )
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
