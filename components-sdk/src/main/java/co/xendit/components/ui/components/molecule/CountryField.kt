package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.xendit.components.data.model.Country
import co.xendit.components.data.model.findCountryByCode
import co.xendit.components.ui.XenditTestTags
import co.xendit.components.ui.style.xenditAppearance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun CountryField(
  modifier: Modifier = Modifier,
  value: String,
  label: String? = null,
  onValueChange: (String) -> Unit,
  placeholder: String? = null,
  isError: Boolean = false,
  errorMessage: String? = null,
  noBorder: Boolean = false,
  testTag: String = "",
) {
  var expanded by remember { mutableStateOf(false) }
  val appearance = xenditAppearance

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

  LaunchedEffect(Unit) {
    if (countries.isEmpty()) {
      withContext(Dispatchers.Default) { Country.warmUp() }
    }
  }

  selectedCountry?.let { country ->
    Column(
      modifier = Modifier
        .border(
          width = 1.dp,
          color = if (noBorder) Color.Transparent else appearance.colorBorder,
          shape = RoundedCornerShape(xenditAppearance.borderRadius)
        )
        .then(if (testTag.isNotBlank()) Modifier.testTag(XenditTestTags.FORM_DROPDOWN_PREFIX + testTag) else Modifier)
    ) {
      CountryPicker(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp),
        isShowCountryName = true,
        selectedCountry = country,
        onCountrySelected = { selected ->
          onValueChange(selected.code)
          expanded = false
        },
        expanded = expanded,
        onExpandedChange = { expanded = it }
      )
      if (isError && errorMessage != null) {
        Text(errorMessage, color = appearance.colorDanger)
      }
    }
  }
}
