package co.xendit.components.ui

import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.SessionResponse

internal data class PaymentMethodSelectionUi(
  val availableChannels: List<BffChannel>,
  val selectedChannel: BffChannel?,
  val expandedGroup: String?,
  val sessionResponse: SessionResponse?
)

internal val PaymentState.selectionUi: PaymentMethodSelectionUi
  get() = PaymentMethodSelectionUi(
    availableChannels = channels,
    selectedChannel = selectedChannel,
    expandedGroup = expandedUiGroup,
    sessionResponse = sessionResponse
  )
