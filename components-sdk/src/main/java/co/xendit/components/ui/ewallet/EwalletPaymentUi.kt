package co.xendit.components.ui.ewallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.ui.components.DynamicForm
import co.xendit.components.ui.helper.SdkImageLoader
import co.xendit.components.ui.style.xenditAppearance
import coil.ImageLoader
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EwalletPaymentUI(
  channels: List<BffChannel>,
  selectedChannel: BffChannel,
  onSelectChannel: (String) -> Unit,
  onFormStateChanged: (Map<String, String>, List<ChannelFormField>, Boolean) -> Unit = { _, _, _ -> },
  showSaveCheckbox: Boolean = false,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val appearance = xenditAppearance
  var expanded by remember { mutableStateOf(false) }
  val formValues = remember { mutableStateMapOf<String, String>() }
  val visibleFields = remember { mutableStateOf<List<ChannelFormField>>(emptyList()) }
  val imageLoader = remember { SdkImageLoader.get(context) }
  val isSaveChecked = remember { mutableStateOf(false) }

  LaunchedEffect(selectedChannel.channelCode) {
    formValues.clear()
    visibleFields.value = emptyList()
    onFormStateChanged(emptyMap(), emptyList(), false)
  }

  Column(
    modifier = modifier
      .padding(top = 16.dp)
      .padding(horizontal = 24.dp)
      .padding(bottom = 32.dp)
  ) {
    Text(
      text = "Pay with",
      style = MaterialTheme.typography.titleSmall,
      color = appearance.colorTextSecondary
    )
    Spacer(modifier = Modifier.height(8.dp))

    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { if (channels.size > 1) expanded = !expanded },
      modifier = Modifier.fillMaxWidth()
    ) {
      OutlinedTextField(
        value = selectedChannel.brandName,
        onValueChange = {},
        readOnly = true,
        placeholder = { Text("Select an E-Wallet") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
          focusedBorderColor = appearance.colorBorder,
          unfocusedBorderColor = appearance.colorBorder,
          focusedTextColor = appearance.colorText,
          unfocusedTextColor = appearance.colorText,
          cursorColor = appearance.colorPrimary
        ),
        modifier = Modifier
          .menuAnchor()
          .fillMaxWidth()
      )

      if (channels.size > 1) {
        ExposedDropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false }
        ) {
          channels.forEach { channel ->
            DropdownMenuItem(
              text = {
                Row() {
                  if (channel.brandLogoUrl != null) {
                    EwalletLogo(
                      logoUrl = channel.brandLogoUrl,
                      imageLoader = imageLoader,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                  }
                  Text(channel.brandName)
                }

              },
              onClick = {
                onSelectChannel(channel.channelCode)
                expanded = false
              }
            )
          }
        }
      }
    }

    selectedChannel.instructions?.forEach { instruction ->
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = instruction,
        style = MaterialTheme.typography.bodySmall,
        color = appearance.colorTextSecondary
      )
    }

    val fields = selectedChannel.form.orEmpty()
    if (fields.isNotEmpty()) {
      Spacer(modifier = Modifier.height(12.dp))
      DynamicForm(
        fields = fields,
        cardDetails = null,
        installmentPlans = null,
        onValuesChanged = { updated ->
          formValues.clear()
          formValues.putAll(updated)
          onFormStateChanged(formValues.toMap(), visibleFields.value, isSaveChecked.value)
        },
        onCardNumberChanged = {},
        onVisibleFieldsChanged = {
          visibleFields.value = it
          onFormStateChanged(formValues.toMap(), visibleFields.value, isSaveChecked.value)
        },
        bffCardInfo = null
      )

      if (showSaveCheckbox) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              isSaveChecked.value = !isSaveChecked.value
              onFormStateChanged(formValues.toMap(), visibleFields.value, isSaveChecked.value)
            }
        ) {
          Checkbox(
            checked = isSaveChecked.value,
            onCheckedChange = {}
          )
          Text(
            text = "Save card information for future use",
            style = MaterialTheme.typography.bodyMedium,
            color = appearance.colorText,
            modifier = Modifier.padding(start = 8.dp)
          )
        }
      }
    }
  }
}

@Composable
internal fun EwalletLogo(
  logoUrl: String,
  imageLoader: ImageLoader,
) {
  val appearance = xenditAppearance

  Box(
    modifier = Modifier
      .size(width = 36.dp, height = 24.dp)
      // 1. Apply border and background to the container
      .border(
        width = 1.dp,
        color = appearance.colorBorder,
        shape = RoundedCornerShape(4.dp)
      )
      .background(
        color = appearance.colorBackground,
        shape = RoundedCornerShape(4.dp)
      )
      .clip(RoundedCornerShape(4.dp)),
    contentAlignment = Alignment.Center
  ) {
    AsyncImage(
      model = logoUrl,
      imageLoader = imageLoader,
      contentDescription = "E-wallet Logo",
      // 2. Use Fit or Inside to prevent "trimming"
      contentScale = ContentScale.Fit,
      modifier = Modifier
        // 3. Add small padding so the logo doesn't touch the border
        .padding(4.dp)
    )
  }
}
