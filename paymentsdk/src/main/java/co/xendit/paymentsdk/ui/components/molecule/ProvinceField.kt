package co.xendit.paymentsdk.ui.components.molecule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import co.xendit.paymentsdk.data.model.ProvinceOption
import co.xendit.paymentsdk.data.model.Provinces
import co.xendit.paymentsdk.ui.style.xenditAppearance

@Composable
fun ProvinceField(
  modifier: Modifier = Modifier,
  value: String,
  label: String? = null,
  onValueChange: (String) -> Unit,
  countryCode: String?,
  placeholder: String = "Select Province",
  isError: Boolean = false,
  errorMessage: String? = null,
  shape: Shape? = null,
  noBorder: Boolean = false,
) {
  val appearance = xenditAppearance
  val options = remember(countryCode) { Provinces.forCountry(countryCode) }
  var expanded by remember { mutableStateOf(false) }

  var previousCountryCode by remember { mutableStateOf(countryCode) }
  if (previousCountryCode != countryCode) {
    previousCountryCode = countryCode
    if (value.isNotBlank()) {
      onValueChange("")
    }
  }

  if (options == null) {
    XenditTextField(
      value = value,
      onValueChange = onValueChange,
      label = label,
      placeholder = placeholder,
      modifier = modifier.fillMaxWidth(),
      isError = isError,
      errorMessage = errorMessage,
      singleLine = true,
      shape = shape,
      noBorder = noBorder
    )
    return
  }

  val selectedTitle =
    remember(value, options) { options.firstOrNull { it.value == value }?.title.orEmpty() }

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable { expanded = true }
  ) {
    XenditTextField(
      value = selectedTitle,
      onValueChange = { },
      label = label,
      placeholder = placeholder,
      modifier = Modifier.fillMaxWidth(),
      enabled = false,
      trailingIcon = {
        Icon(
          imageVector = Icons.Default.KeyboardArrowDown,
          contentDescription = null,
          tint = appearance.colorTextSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      isError = isError,
      errorMessage = errorMessage,
      shape = shape,
      noBorder = noBorder
    )
  }

  if (expanded) {
    ProvincePickerDialog(
      options = options,
      onDismiss = { expanded = false },
      onSelected = {
        onValueChange(it.value)
        expanded = false
      }
    )
  }
}

@Composable
private fun ProvincePickerDialog(
  options: List<ProvinceOption>,
  onDismiss: () -> Unit,
  onSelected: (ProvinceOption) -> Unit
) {
  val appearance = xenditAppearance
  var searchQuery by remember { mutableStateOf("") }
  val filteredOptions by
  remember(searchQuery, options) {
    derivedStateOf {
      if (searchQuery.isBlank()) {
        options
      } else {
        options.filter { it.title.contains(searchQuery, ignoreCase = true) }
      }
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.medium,
      color = appearance.colorBackground ?: MaterialTheme.colorScheme.surface
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier.fillMaxWidth(),
          placeholder = {
            Text(
              "Search",
              color = appearance.colorTextPlaceholder ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = appearance.colorTextSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          singleLine = true,
          colors =
            androidx.compose.material3.OutlinedTextFieldDefaults.colors(
              focusedBorderColor = appearance.colorBorder ?: MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = appearance.colorBorder ?: MaterialTheme.colorScheme.outline,
              focusedTextColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
              unfocusedTextColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
              focusedLabelColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
              unfocusedLabelColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
              cursorColor = appearance.colorPrimary ?: MaterialTheme.colorScheme.primary
            )
        )

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 12.dp),
          color = appearance.colorBorder ?: MaterialTheme.colorScheme.outline
        )

        if (filteredOptions.isEmpty()) {
          Text(
            text = "No provinces found",
            modifier = Modifier
              .padding(24.dp)
              .fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = appearance.colorTextSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(filteredOptions, key = { it.value }) { option ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSelected(option) }
                  .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(modifier = Modifier.weight(1f)) {
                  Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = appearance.colorText ?: MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
