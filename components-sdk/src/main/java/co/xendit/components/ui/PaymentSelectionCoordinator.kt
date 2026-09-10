package co.xendit.components.ui

import co.xendit.components.data.model.BffChannel

internal data class PaymentSelectionResult(
  val expandedUiGroup: String?,
  val selectedChannel: BffChannel?
)

internal class PaymentSelectionCoordinator {
  fun toggleGroup(
    channels: List<BffChannel>,
    currentExpandedUiGroup: String?,
    currentSelectedChannel: BffChannel?,
    uiGroup: String,
    lastSelectedChannelCode: String?
  ): PaymentSelectionResult {
    val groups = channels.groupBy { it.uiGroup }
    val newExpandedUiGroup = if (currentExpandedUiGroup == uiGroup) null else uiGroup

    val nextSelected =
      if (newExpandedUiGroup == null) {
        currentSelectedChannel
      } else if (currentSelectedChannel?.uiGroup == newExpandedUiGroup) {
        currentSelectedChannel
      } else {
        val lastSelected =
          lastSelectedChannelCode?.let { code ->
            channels.firstOrNull { it.channelCode == code }
          }
        lastSelected ?: groups[newExpandedUiGroup]
          ?.firstOrNull()
          ?.takeIf { groups[newExpandedUiGroup]?.size == 1 }
      }

    return PaymentSelectionResult(
      expandedUiGroup = newExpandedUiGroup,
      selectedChannel = nextSelected
    )
  }

  fun selectChannel(
    channels: List<BffChannel>,
    channelCode: String
  ): BffChannel? {
    return channels.firstOrNull { it.channelCode == channelCode }
  }
}
