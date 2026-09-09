package co.xendit.components.ui

import co.xendit.components.R
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.PaymentAction
import co.xendit.components.data.model.PaymentDraft
import co.xendit.components.data.model.SessionResponse
import co.xendit.components.ui.components.molecule.UiText

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

internal data class PaymentActionUi(
  val action: PaymentAction,
  val merchantName: String?,
  val channelName: String,
  val channelLogoUrl: String?,
  val channelBrandColor: String?,
  val amount: java.math.BigDecimal?,
  val currency: String?
)

internal val PaymentState.customerActionUi: PaymentActionUi?
  get() = presentToCustomerPaymentAction?.let { action ->
    PaymentActionUi(
      action = action,
      merchantName = sessionResponse?.business?.name,
      channelName = selectedChannel?.brandName.orEmpty(),
      channelLogoUrl = selectedChannel?.brandLogoUrl,
      channelBrandColor = selectedChannel?.brandColor,
      amount = sessionResponse?.session?.amount,
      currency = sessionResponse?.session?.currency
    )
  }

internal data class PaymentSubmitUi(
  val selectedChannel: BffChannel?,
  val currentDraft: PaymentDraft,
  val isEnabled: Boolean,
  val buttonText: UiText
)

internal fun PaymentState.toSubmitUi(isEnabled: Boolean): PaymentSubmitUi {
  val selected = selectedChannel
  val draft = if (selected == null) {
    PaymentDraft()
  } else {
    paymentDrafts[selected.channelCode] ?: PaymentDraft(channelCode = selected.channelCode)
  }
  val buttonText = when (sessionType) {
    BffSessionType.SAVE -> UiText.StringResource(R.string.sessionpayment_methods_add_payment_method)
    BffSessionType.SUBSCRIPTION ->
      UiText.StringResource(R.string.sessionchannel_selection_confirm_subscription)

    else -> UiText.StringResource(R.string.sessionpayment_methods_submit_pay)
  }
  return PaymentSubmitUi(
    selectedChannel = selected,
    currentDraft = draft,
    isEnabled = isEnabled,
    buttonText = buttonText
  )
}
