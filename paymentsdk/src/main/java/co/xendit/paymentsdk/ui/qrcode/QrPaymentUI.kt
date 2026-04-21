package co.xendit.paymentsdk.ui.qrcode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.xendit.paymentsdk.R
import co.xendit.paymentsdk.data.model.BffChannel
import co.xendit.paymentsdk.data.model.ChannelFormField
import co.xendit.paymentsdk.ui.components.DynamicForm
import co.xendit.paymentsdk.ui.style.xenditAppearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QrPaymentUI(
  channels: List<BffChannel>,
  selectedChannel: BffChannel,
  onSelectChannel: (String) -> Unit,
  onFormStateChanged: (Map<String, String>, List<ChannelFormField>) -> Unit,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  var expanded by remember { mutableStateOf(false) }
  val formValues = remember { mutableStateMapOf<String, String>() }
  val visibleFields = remember { mutableStateOf<List<ChannelFormField>>(emptyList()) }

  LaunchedEffect(selectedChannel.channelCode) {
    formValues.clear()
    visibleFields.value = emptyList()
    onFormStateChanged(emptyMap(), emptyList())
  }

  Column(
    modifier = modifier
      .padding(top = 16.dp)
      .padding(horizontal = 24.dp)
      .padding(bottom = 32.dp)
  ) {
    Text(
      text = stringResource(id = R.string.sessionpayment_methods_pay_with),
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
        placeholder = {
          Text("Select a QR method")
        },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
          focusedBorderColor = appearance.colorBorder,
          unfocusedBorderColor = appearance.colorBorder,
          focusedTextColor = appearance.colorText,
          unfocusedTextColor = appearance.colorText,
          cursorColor = appearance.colorPrimary
        ),
        modifier = Modifier.menuAnchor().fillMaxWidth()
      )

      if (channels.size > 1) {
        ExposedDropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false }
        ) {
          channels.forEach { channel ->
            DropdownMenuItem(
              text = { Text(channel.brandName) },
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
          onFormStateChanged(formValues.toMap(), visibleFields.value)
        },
        onCardNumberChanged = {},
        onVisibleFieldsChanged = {
          visibleFields.value = it
          onFormStateChanged(formValues.toMap(), visibleFields.value)
        },
        bffCardInfo = null
      )
    }
  }
}
