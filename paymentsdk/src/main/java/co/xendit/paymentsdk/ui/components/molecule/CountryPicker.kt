package co.xendit.paymentsdk.ui.components.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.xendit.paymentsdk.data.model.Country
import co.xendit.paymentsdk.ui.style.xenditAppearance
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CountryPicker(
  selectedCountry: Country,
  onCountrySelected: (Country) -> Unit,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  var searchQuery by remember { mutableStateOf("") }

  // Use the pre-warmed countries flow from the model
  val countries by Country.countriesFlow.collectAsStateWithLifecycle()

  // Ensure warm-up is triggered if not already
  LaunchedEffect(Unit) {
    if (countries.isEmpty()) {
      withContext(Dispatchers.Default) { Country.warmUp() }
    }
  }

  // Memoize filtered list on background thread when searchQuery or countries changes
  val filteredCountries by
  remember(searchQuery, countries) {
    derivedStateOf {
      if (searchQuery.isBlank()) {
        countries
      } else {
        countries.filter {
          it.name.contains(searchQuery, ignoreCase = true) ||
              it.dialCode.contains(searchQuery) ||
              it.code.contains(searchQuery, ignoreCase = true)
        }
      }
    }
  }

  Box(modifier = modifier) {
    Row(
      modifier = Modifier
        .clickable { onExpandedChange(true) }
        .padding(start = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      FlagIcon(countryCode = selectedCountry.code)
      Text(
        text = selectedCountry.dialCode,
        style = MaterialTheme.typography.bodyMedium,
        color = appearance.colorText ?: MaterialTheme.colorScheme.onSurface
      )
      Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = "Select Country",
        tint = appearance.colorTextSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    if (expanded) {
      val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
      ModalBottomSheet(
        onDismissRequest = {
          onExpandedChange(false)
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
        containerColor = appearance.colorBackground ?: MaterialTheme.colorScheme.surface
      ) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
          color = appearance.colorBackground ?: MaterialTheme.colorScheme.surface
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Column (
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
              IconButton(
                onClick = {
                  onExpandedChange(false)
                  searchQuery = ""
                }
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = null,
                  tint = appearance.colorTextSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Text(
                text = "Select Country",
                style = MaterialTheme.typography.titleLarge,
                color = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
              )
            }

            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = {
                Text(
                  "Search country code",
                  color = appearance.colorTextPlaceholder ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
              },
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              leadingIcon = {
                Icon(
                  Icons.Default.Search,
                  contentDescription = null,
                  tint = appearance.colorTextSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
              },
              singleLine = true,
              shape = RoundedCornerShape(8.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = Color.White,
                  unfocusedContainerColor = Color(0xFFF2F2F2),
                  focusedBorderColor = (appearance.colorBorder ?: MaterialTheme.colorScheme.outline).copy(alpha = 0.25f),
                  unfocusedBorderColor = (appearance.colorBorder ?: MaterialTheme.colorScheme.outline).copy(alpha = 0.15f),
                  focusedTextColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
                  unfocusedTextColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
                  focusedLabelColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
                  unfocusedLabelColor = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
                  cursorColor = appearance.colorPrimary ?: MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "All Countries",
              style = MaterialTheme.typography.bodySmall,
              color = appearance.colorTextSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (countries.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  color = appearance.colorPrimary ?: MaterialTheme.colorScheme.primary
                )
              }
            } else {
              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f)
              ) {
                items(filteredCountries, key = { it.code }) { country ->
                  Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                          onCountrySelected(country)
                          onExpandedChange(false)
                          searchQuery = ""
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                      FlagIcon(countryCode = country.code)
                      Text(
                        text = "${country.name} +${country.dialCode}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                      )
                    }
                    HorizontalDivider(
                      modifier = Modifier.padding(start = 52.dp),
                      color = (appearance.colorBorder ?: MaterialTheme.colorScheme.outline).copy(alpha = 0.25f)
                    )
                  }
                }

                if (filteredCountries.isEmpty()) {
                  item {
                    Text(
                      text = "No countries found",
                      modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                      style = MaterialTheme.typography.bodyMedium,
                      color = appearance.colorTextSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
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
}

@Composable
internal fun FlagIcon(countryCode: String, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  // Reuse a single decoder factory instance to avoid overhead in lists
  val svgDecoderFactory = remember { SvgDecoder.Factory() }

  AsyncImage(
    model =
      remember(countryCode) {
        ImageRequest.Builder(context)
          .data(
            "https://assets.xendit.co/payment-session/flags/circle/${countryCode.lowercase()}.svg"
          )
          .decoderFactory(svgDecoderFactory)
          .crossfade(true)
          .build()
      },
    contentDescription = "Flag of $countryCode",
    modifier = modifier
      .size(24.dp)
      .clip(CircleShape),
    contentScale = ContentScale.Crop
  )
}
