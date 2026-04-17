package co.xendit.paymentsdk.ui.components.molecule

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.xendit.paymentsdk.data.model.Country
import co.xendit.paymentsdk.data.model.findCountryByCode
import co.xendit.paymentsdk.ui.style.xenditAppearance
import com.google.i18n.phonenumbers.PhoneNumberUtil

@Composable
fun PhoneNumberField(
  modifier: Modifier = Modifier,
  value: String,
  label: String? = null,
  onValueChange: (String) -> Unit,
  countryCode: String,
  onCountryCodeChange: (String) -> Unit,
  placeholder: String? = null,
  isError: Boolean = false,
  errorMessage: String? = null,
  shape: Shape? = null,
  noBorder: Boolean = false,
) {
  val appearance = xenditAppearance
  val phoneUtil = remember { PhoneNumberUtil.getInstance() }
  var expanded by remember { mutableStateOf(false) }

  // Use the pre-warmed countries flow from the model
  val countries by Country.countriesFlow.collectAsStateWithLifecycle()

  // Resolve selected country based on the countryCode prop
  val selectedCountry =
    remember(countryCode, countries) {
      if (countries.isEmpty()) null
      else {
        countries.findCountryByCode(countryCode) ?: countries.first()
      }
    }

  // Use the placeholder logic as recently edited by the user
  val dynamicPlaceholder =
    remember(selectedCountry) {
      if (selectedCountry == null) placeholder ?: ""
      else {
        try {
          val code = selectedCountry.code
          val example = phoneUtil.getExampleNumber(code)
          val display = phoneUtil.format(example, PhoneNumberUtil.PhoneNumberFormat.E164)
          display.removePrefix("+${selectedCountry.dialCode}")
        } catch (e: Exception) {
          placeholder ?: ""
        }
      }
    }

  Column() {
    label?.let {
      Text(
        text = it,
        style = MaterialTheme.typography.titleSmall,
        color = appearance.colorText
      )
      Spacer(modifier = Modifier.height(8.dp))
    }

    Row(
      modifier = modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {

      selectedCountry?.let { country ->
        Box(
          modifier = Modifier.border(
            width = 1.dp,
            color = if (noBorder) Color.Transparent else appearance.colorBorder,
            shape = RoundedCornerShape(xenditAppearance.borderRadius)
          )
        ) {
          CountryPicker(
            selectedCountry = country,
            onCountrySelected = { selected ->
              onCountryCodeChange(selected.code)
              expanded = false
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))

      }

      XenditTextField(
        value = value,
        onValueChange = { newValue ->
          // Format as you type
          val digitsOnly = newValue.filter { it.isDigit() }
          onValueChange(digitsOnly)
        },
        placeholder = dynamicPlaceholder,
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        errorMessage = null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        shape = shape,
        noBorder = noBorder
      )
    }
    errorMessage?.let {
      Text(
        text = it,
        style = MaterialTheme.typography.labelSmall,
        color = appearance.colorDanger,
        modifier = Modifier.padding(top = 2.dp)
      )
    }
  }

}
