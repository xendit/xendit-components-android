package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.xendit.components.data.model.ProvinceOption
import co.xendit.components.data.model.Provinces
import co.xendit.components.ui.style.xenditAppearance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProvinceField(
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
  val scope = rememberCoroutineScope()
  var showSheet by remember { mutableStateOf(false) }

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
        .clickable { showSheet = true }
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
          tint = appearance.colorTextSecondary
        )
      },
      isError = isError,
      errorMessage = errorMessage,
      shape = shape,
      noBorder = noBorder,
      disabledTextColor = appearance.colorText
    )
  }

  if (showSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ProvincePickerSheet(
      options = options,
      sheetState = sheetState,
      onDismiss = {
        scope.launch {
          sheetState.hide()
          showSheet = false
        }
      },
      onSelected = { option ->
        scope.launch {
          sheetState.hide()
          showSheet = false
          onValueChange(option.value)
        }
      }
    )
  }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProvincePickerSheet(
  options: List<ProvinceOption>,
  sheetState: SheetState,
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

  ModalBottomSheet(
    onDismissRequest = {
      onDismiss()
      searchQuery = ""
    },
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 8.dp, bottom = 6.dp)
          .size(width = 36.dp, height = 4.dp)
          .clip(RoundedCornerShape(100.dp))
          .background(Color(0xFFD0D0D0))
      )
    },
    containerColor = appearance.colorBackground
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.medium,
      color = appearance.colorBackground
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        ) {
          Icon(
            modifier = Modifier.clickable {
              onDismiss()
              searchQuery = ""
            },
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = appearance.colorTextSecondary
          )

          Text(
            text = "Select Province",
            style = MaterialTheme.typography.headlineSmall,
            color = appearance.colorText,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier.fillMaxWidth(),
          placeholder = {
            Text(
              "Search",
              color = appearance.colorTextPlaceholder
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = appearance.colorTextSecondary
            )
          },
          singleLine = true,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color.White,
              unfocusedContainerColor = Color(0xFFF2F2F2),
              focusedBorderColor = (appearance.colorBorder).copy(alpha = 0.25f),
              unfocusedBorderColor = (appearance.colorBorder).copy(alpha = 0.15f),
              focusedTextColor = appearance.colorText,
              unfocusedTextColor = appearance.colorText,
              focusedLabelColor = appearance.colorText,
              unfocusedLabelColor = appearance.colorText,
              cursorColor = appearance.colorPrimary
            )
        )

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 12.dp),
          color = appearance.colorBorder
        )

        if (filteredOptions.isEmpty()) {
          Text(
            text = "No provinces found",
            modifier = Modifier
              .padding(24.dp)
              .fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = appearance.colorTextSecondary
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
                    color = appearance.colorText
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
