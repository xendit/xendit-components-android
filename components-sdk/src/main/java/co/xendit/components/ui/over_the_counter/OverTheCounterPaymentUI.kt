package co.xendit.components.ui.over_the_counter

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import co.xendit.components.R
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.ui.components.PaymentChannelSelectionUI


@Composable
internal fun OverTheCounterPaymentUI(
  displayName: String,
  channels: List<BffChannel>,
  selectedChannel: BffChannel?,
  initialValues: Map<String, String> = emptyMap(),
  initialVisibleFields: List<ChannelFormField> = emptyList(),
  initialSaveChecked: Boolean = false,
  onSelectChannel: (String) -> Unit,
  onFormStateChanged: (Map<String, String>, List<ChannelFormField>, Boolean) -> Unit = { _, _, _ -> },
  showSaveCheckbox: Boolean = false,
  modifier: Modifier = Modifier
) {
  PaymentChannelSelectionUI(
    headerText = stringResource(id = R.string.sessionpayment_methods_pay_with),
    placeholderText =
      stringResource(
        id = R.string.sessionpayment_methods_select_channel_placeholder,
      ).replace("{{groupName}}", displayName),
    channels = channels,
    selectedChannel = selectedChannel,
    initialValues = initialValues,
    initialVisibleFields = initialVisibleFields,
    initialSaveChecked = initialSaveChecked,
    onSelectChannel = onSelectChannel,
    onFormStateChanged = onFormStateChanged,
    saveCheckboxText =
      if (showSaveCheckbox && selectedChannel != null) {
        stringResource(id = R.string.sessionpayment_save_checkbox_label)
      } else {
        null
      },
    modifier = modifier
  )
}