package co.xendit.paymentsdk.ui.components.molecule

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.xendit.paymentsdk.data.model.Country
import co.xendit.paymentsdk.ui.style.XenditAppearance
import co.xendit.paymentsdk.ui.style.xenditAppearance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import co.xendit.paymentsdk.data.model.findCountryByCode

@Composable
fun CountryField(
  modifier: Modifier = Modifier,
  value: String,
  label: String? = null,
  onValueChange: (String) -> Unit,
  placeholder: String = "Select Country",
  isError: Boolean = false,
  errorMessage: String? = null,
  shape: Shape? = null,
  noBorder: Boolean = false,
) {
  var expanded by remember { mutableStateOf(false) }

  // Resolve selected country from value (code or name)
  val countries by Country.countriesFlow.collectAsStateWithLifecycle()
  val selectedCountry =
    remember(value, countries) {
      if (countries.isEmpty()) null
      else if (value.isBlank()) countries.first()
      else {
        countries.findCountryByCode(value)
          ?: countries.find { it.name == value } ?: countries.first()
      }
    }

  // Ensure warm-up is triggered if not already
  LaunchedEffect(Unit) {
    if (countries.isEmpty()) {
      withContext(Dispatchers.Default) { Country.warmUp() }
    }
  }

  XenditTextField(
    value = selectedCountry?.name ?: "",
    onValueChange = { /* Read only, changed via picker */ },
    label = label,
    placeholder = placeholder,
    modifier = modifier.fillMaxWidth(),
    readOnly = true,
    isError = isError,
    errorMessage = errorMessage,
    leadingIcon = {
      selectedCountry?.let { country ->
        CountryPicker(
          selectedCountry = country,
          onCountrySelected = { selected ->
            onValueChange(selected.code) // We emit the Alpha-2 code
            expanded = false
          },
          expanded = expanded,
          onExpandedChange = { expanded = it }
        )
      }
    },
    shape = shape,
    noBorder = noBorder
  )
}
