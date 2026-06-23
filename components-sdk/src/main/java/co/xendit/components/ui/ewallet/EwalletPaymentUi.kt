package co.xendit.components.ui.ewallet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import co.xendit.components.R
import co.xendit.components.data.model.BffBusiness
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.BffSession
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.ui.components.PaymentChannelSelectionUI

@Composable
internal fun EwalletPaymentUI(
  channels: List<BffChannel>,
  bffBusiness: BffBusiness?,
  session: BffSession? = null,
  selectedChannel: BffChannel?,
  effectiveChannel: BffChannel? = selectedChannel,
  initialValues: Map<String, String> = emptyMap(),
  initialVisibleFields: List<ChannelFormField> = emptyList(),
  initialSaveChecked: Boolean = false,
  onSelectChannel: (String) -> Unit,
  onFormStateChanged: (Map<String, String>, List<ChannelFormField>, Boolean) -> Unit = { _, _, _ -> },
  onSaveCheck: (Boolean, Map<String, String>, List<ChannelFormField>) -> Unit = { _, _, _ -> },
  showSaveCheckbox: Boolean = false,
  modifier: Modifier = Modifier
) {
  PaymentChannelSelectionUI(
    headerText = stringResource(id = R.string.ewallet_pay_with),
    placeholderText = stringResource(id = R.string.ewallet_select_placeholder),
    channels = channels,
    session = session,
    selectedChannel = selectedChannel,
    contentChannel = effectiveChannel,
    initialValues = initialValues,
    initialVisibleFields = initialVisibleFields,
    initialSaveChecked = initialSaveChecked,
    onSelectChannel = onSelectChannel,
    onFormStateChanged = onFormStateChanged,
    onSaveCheck = onSaveCheck,
    saveCheckboxText =
      if (showSaveCheckbox && effectiveChannel != null) {
        stringResource(
          id = R.string.ewallet_link_for_future_purchase,
          effectiveChannel.brandName,
          bffBusiness?.name.orEmpty()
        )
      } else {
        null
      },
    modifier = modifier
  )
}
