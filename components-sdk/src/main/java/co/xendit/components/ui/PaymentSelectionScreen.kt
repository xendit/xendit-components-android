package co.xendit.components.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.xendit.components.R
import co.xendit.components.XenditComponentsPaymentType
import co.xendit.components.core.CoreSdkComponent
import co.xendit.components.data.model.BffGooglePayAllowedMethod
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.data.model.PaymentDraft
import co.xendit.components.data.model.isAvailableForAmount
import co.xendit.components.ui.card.CardIntent
import co.xendit.components.ui.card.CardState
import co.xendit.components.ui.card.CardViewModel
import co.xendit.components.ui.components.molecule.GenericHeader
import co.xendit.components.ui.digital_wallet.GooglePaySection
import co.xendit.components.ui.helper.FormChecker.validateAllField
import co.xendit.components.ui.method.PaymentMethodsUI
import co.xendit.components.ui.style.XenditAppearance

@Composable
internal fun PaymentSelectionScreen(
  state: PaymentState,
  cardState: CardState,
  style: XenditAppearance,
  appearance: XenditAppearance,
  merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>?,
  dismiss: () -> Unit,
  viewModel: PaymentViewModel,
  cardViewModel: CardViewModel,
  onCommitAutofill: () -> Unit
) {
  val selectedPmType by rememberUpdatedState(state.selectedChannel?.pmType)
  val installmentPlans by rememberUpdatedState(cardState.installmentPlans)
  val onToggleGroup: (String) -> Unit =
    remember(viewModel) { { viewModel.dispatch(ActionIntent.ToggleUiGroup(it)) } }
  val onSelectChannel: (String) -> Unit =
    remember(viewModel) { { viewModel.dispatch(ActionIntent.SelectChannel(it)) } }
  val onCardNumberChanged: (String) -> Unit =
    remember(cardViewModel) { { cardViewModel.dispatch(CardIntent.CardNumberChanged(it)) } }
  val onFormChanged: (String?, Map<String, String>, List<ChannelFormField>, Boolean) -> Unit =
    remember(viewModel) {
      { channelCode, formValues, visibleFields, save ->
        viewModel.dispatch(
          ActionIntent.UpdatePaymentDraft(
            PaymentDraft(
              channelCode = channelCode,
              formValues = formValues,
              visibleFields = visibleFields,
              savePaymentMethod = save,
              installmentPlans =
                if (selectedPmType == XenditComponentsPaymentType.CARDS) installmentPlans else null
            )
          )
        )
      }
    }

  Column {
    GenericHeader(
      title = stringResource(id = R.string.sessionpayment_methods_select_payment_method),
      onLeftClick = dismiss
    )
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
    ) {
      GooglePayEntryPoint(
        state = state,
        merchantPreferredPaymentMethod = merchantPreferredPaymentMethod,
        viewModel = viewModel,
        modifier = Modifier.padding(top = 8.dp)
      )

      PaymentMethodsUI(
        session = state.sessionResponse?.session,
        bffBusiness = state.sessionResponse?.business,
        merchantPreferredPaymentMethod = merchantPreferredPaymentMethod,
        channels = state.channels,
        channelUiGroups = state.sessionResponse?.channelUiGroups,
        channelVariantsByDisplayCode = state.channelVariantsByDisplayCode,
        expandedUiGroup = state.expandedUiGroup,
        selectedChannel = state.selectedChannel,
        paymentDrafts = state.paymentDrafts,
        cardDetails = cardState.cardDetails,
        installmentPlans = cardState.installmentPlans,
        sessionType = state.sessionType,
        allowSavePaymentMethod = state.allowSavePaymentMethod,
        onToggleGroup = onToggleGroup,
        onSelectChannel = onSelectChannel,
        onCardNumberChanged = onCardNumberChanged,
        onFormChanged = onFormChanged,
        formWipeNonce = state.formWipeNonce
      )
    }

    PaymentSubmitButton(
      state = state,
      cardState = cardState,
      style = style,
      appearance = appearance,
      onCommitAutofill = onCommitAutofill,
      onSubmit = { channelCode, formValues, fields, savePaymentMethod, submitInstallmentPlans ->
        viewModel.dispatch(
          ActionIntent.SubmitAction(
            channelCode = channelCode,
            formValues = formValues,
            fields = fields,
            savePaymentMethod = savePaymentMethod,
            installmentPlans = submitInstallmentPlans
          )
        )
      }
    )
  }
}

@Composable
internal fun GooglePayEntryPoint(
  state: PaymentState,
  merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>?,
  viewModel: PaymentViewModel,
  modifier: Modifier = Modifier
) {
  val preferredList =
    remember(merchantPreferredPaymentMethod) {
      merchantPreferredPaymentMethod
        ?.filter { it in XenditComponentsPaymentType.SUPPORTED }
        ?: emptyList()
    }
  val googlePayConfig = state.sessionResponse?.digitalWallets?.googlePay
  val filteredGooglePayMethods: List<BffGooglePayAllowedMethod> =
    remember(
      googlePayConfig,
      state.channels,
      state.sessionResponse?.session?.amount,
      state.sessionType,
    ) {
      filterGooglePayAllowedMethodsByAmount(
        googlePay = googlePayConfig,
        channels = state.channels,
        amount = state.sessionResponse?.session?.amount,
        sessionType = state.sessionType,
      )
    }
  val shouldShowGooglePay =
    shouldRenderGooglePaySection(
      googlePay = googlePayConfig,
      merchantPreferredPaymentMethod = preferredList,
    ) && filteredGooglePayMethods.isNotEmpty()

  if (shouldShowGooglePay) {
    GooglePaySection(
      googlePay = googlePayConfig,
      businessName = state.sessionResponse?.business?.name.orEmpty(),
      paymentSessionId = state.sessionResponse?.session?.paymentSessionId,
      amount = state.sessionResponse?.session?.amount,
      currency = state.sessionResponse?.session?.currency,
      country = state.sessionResponse?.session?.country,
      filteredAllowedMethods = filteredGooglePayMethods,
      isTest = !CoreSdkComponent.isProdLive(),
      isLoading = state.awaitingPaymentAction == AwaitingPaymentAction.GooglePayProcessing,
      onTrackClick = { viewModel.trackDigitalWallet() },
      onLoadedVisible = { viewModel.trackDigitalWalletLoaded() },
      onPaymentDataReceived = { json, paymentMethodType ->
        viewModel.dispatch(
          ActionIntent.SubmitGooglePay(
            paymentDataJson = json,
            paymentMethodType = paymentMethodType
          )
        )
      },
      onPaymentFailed = { err ->
        viewModel.dispatch(
          ActionIntent.GooglePayPaymentFailed(
            code = err.code,
            title = err.title,
            message = err.message
          )
        )
      },
      modifier = modifier
    )
  }
}

@Composable
internal fun PaymentSubmitButton(
  state: PaymentState,
  cardState: CardState,
  style: XenditAppearance,
  appearance: XenditAppearance,
  onCommitAutofill: () -> Unit,
  onSubmit: (
    channelCode: String,
    formValues: Map<String, String>,
    fields: List<ChannelFormField>,
    savePaymentMethod: Boolean,
    installmentPlans: List<InstallmentPlan>?
  ) -> Unit
) {
  val selectedChannel = state.selectedChannel
  val isPaymentSelected = state.expandedUiGroup != null && selectedChannel != null
  val isSelectedChannelAvailable =
    selectedChannel?.isAvailableForAmount(
      state.sessionResponse?.session?.amount,
      state.sessionType
    ) != false
  val currentDraft =
    if (selectedChannel == null) {
      PaymentDraft()
    } else {
      state.paymentDrafts[selectedChannel.channelCode]
        ?: PaymentDraft(channelCode = selectedChannel.channelCode)
    }
  val isPayEnabled =
    isPaymentSelected && isSelectedChannelAvailable && !state.isLoading && validateAllField(
      currentDraft.visibleFields,
      currentDraft.formValues,
      cardDetails = cardState.cardDetails,
      bffCardInfo = selectedChannel.card
    )
  val payText =
    when (state.sessionType) {
      BffSessionType.SAVE ->
        stringResource(id = R.string.sessionpayment_methods_add_payment_method)

      BffSessionType.SUBSCRIPTION ->
        stringResource(id = R.string.sessionchannel_selection_confirm_subscription)

      else ->
        stringResource(id = R.string.sessionpayment_methods_submit_pay)
    }

  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
    Button(
      enabled = isPayEnabled,
      onClick = {
        val selected = state.selectedChannel ?: return@Button
        val draft = state.paymentDrafts[selected.channelCode]
          ?: PaymentDraft(channelCode = selected.channelCode)
        val installmentPlans =
          if (selected.pmType == XenditComponentsPaymentType.CARDS) {
            cardState.installmentPlans
          } else {
            draft.installmentPlans
          }
        onCommitAutofill()
        onSubmit(
          selected.channelCode,
          draft.formValues,
          draft.visibleFields,
          draft.savePaymentMethod,
          installmentPlans
        )
      },
      modifier = Modifier
        .fillMaxWidth()
        .testTag(XenditTestTags.DIALOG_SUBMIT_BUTTON),
      shape = RoundedCornerShape(appearance.borderRadius),
      colors = ButtonDefaults.buttonColors(
        containerColor = style.colorPrimary,
        contentColor = style.colorBackground
      )
    ) {
      androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = payText,
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.padding(end = 8.dp)
        )
        Icon(
          imageVector = Icons.AutoMirrored.Default.ArrowForward,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}
