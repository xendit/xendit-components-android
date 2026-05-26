package co.xendit.components.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.xendit.components.BuildConfig
import co.xendit.components.R
import co.xendit.components.XenditComponents
import co.xendit.components.core.CoreSdkComponent.globalErrorHandler
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.PaymentDraft
import co.xendit.components.data.model.PaymentRequestStatus
import co.xendit.components.data.model.PaymentSessionStatus
import co.xendit.components.data.model.XenditError
import co.xendit.components.data.model.XenditPaymentResult
import co.xendit.components.internal_entry_point.CardViewModelFactory
import co.xendit.components.internal_entry_point.PaymentViewModelFactory
import co.xendit.components.ui.action.ActionQrUI
import co.xendit.components.ui.action.ActionWebViewUI
import co.xendit.components.ui.card.CardIntent
import co.xendit.components.ui.card.CardViewModel
import co.xendit.components.ui.components.molecule.AwaitingPaymentDialog
import co.xendit.components.ui.components.molecule.GenericHeader
import co.xendit.components.ui.helper.FailureCodeMessageUtil
import co.xendit.components.ui.helper.FormChecker.validateAllField
import co.xendit.components.ui.method.PaymentMethodsUI
import co.xendit.components.ui.style.XenditAppearance
import co.xendit.components.ui.style.xenditAppearance
import io.nerdythings.okhttp.modifier.settings.OkHttpProfilerSettingsActivity
import kotlinx.coroutines.launch

internal enum class PaymentContainerPresentation {
  Dialog,
  BottomSheet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentContainerHost(
  presentation: PaymentContainerPresentation,
  sessionAuthKey: String,
  publicKey: String,
  merchantPreferredPaymentMethod: List<String>?,
  style: XenditAppearance,
  onResult: (XenditPaymentResult) -> Unit,
  onCleanup: () -> Unit
) {
  val viewModel: PaymentViewModel =
    viewModel(factory = PaymentViewModelFactory(LocalContext.current))
  val mviState by viewModel.state.collectAsStateWithLifecycle()
  val cardViewModel: CardViewModel =
    viewModel(factory = CardViewModelFactory(LocalContext.current))
  val cardState by cardViewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val appearance = xenditAppearance
  var pendingSnackbarMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(
    pendingSnackbarMessage,
    mviState.paymentActionRedirect,
    mviState.presentToCustomerPaymentAction
  ) {
    val message = pendingSnackbarMessage ?: return@LaunchedEffect
    if (mviState.paymentActionRedirect != null || mviState.presentToCustomerPaymentAction != null) {
      return@LaunchedEffect
    }
    snackbarHostState.showSnackbar(message)
    pendingSnackbarMessage = null
  }

  val sheetState =
    if (presentation == PaymentContainerPresentation.BottomSheet) {
      rememberModalBottomSheetState(skipPartiallyExpanded = true)
    } else {
      null
    }

  val dismiss: () -> Unit = {
    if (presentation == PaymentContainerPresentation.BottomSheet && sheetState != null) {
      scope.launch {
        sheetState.hide()
        viewModel.resetForNewSession()
        onResult(XenditPaymentResult.Canceled)
        onCleanup()
      }
    } else {
      viewModel.resetForNewSession()
      onResult(XenditPaymentResult.Canceled)
      onCleanup()
    }
  }

  LaunchedEffect(sessionAuthKey, publicKey) {
    viewModel.dispatch(ActionIntent.Initialize(sessionAuthKey, publicKey))
  }

  LaunchedEffect(Unit) {
    globalErrorHandler.apiErrorFlow.collect { (errorCode, message) ->
      val msg = message?.asString(context) ?: return@collect
      if (errorCode == "NETWORK_ERROR") {
        snackbarHostState.showSnackbar(msg)
        onResult(
          XenditPaymentResult.Failed(
            XenditError(
              code = "NETWORK_ERROR",
              message = msg,
              cause = Throwable(msg)
            )
          )
        )
        return@collect
      }
      snackbarHostState.showSnackbar(msg)
    }
  }

  LaunchedEffect(mviState.sessionResponse) {
    val session = mviState.sessionResponse ?: return@LaunchedEffect
    val bffSession = mviState.sessionResponse?.session ?: return@LaunchedEffect
    when (bffSession.status) {
      PaymentSessionStatus.COMPLETED -> {
        viewModel.resetForNewSession()
        onResult(
          XenditPaymentResult.Success(
            paymentRequestId = bffSession.paymentSessionId,
            channelCode = session.succeededChannel?.channelCode
          )
        )
        onCleanup()
      }

      PaymentSessionStatus.CANCELED -> {
        viewModel.resetForNewSession()
        onResult(XenditPaymentResult.Canceled)
        onCleanup()
      }

      PaymentSessionStatus.EXPIRED -> {
        viewModel.markClosed()
        onResult(XenditPaymentResult.Expired)
        onCleanup()
      }

      else -> {

      }

    }
  }

  LaunchedEffect(mviState.pollResponse) {
    val poll = mviState.pollResponse ?: return@LaunchedEffect
    val sessionStatus = poll.session?.status
    val prStatus = poll.paymentRequest?.status
    val isSuccess =
      sessionStatus == PaymentSessionStatus.COMPLETED
          || prStatus == PaymentRequestStatus.SUCCEEDED
          || prStatus == PaymentRequestStatus.AUTHORIZED
          || poll.succeededChannel != null

    val isCanceled =
      sessionStatus == PaymentSessionStatus.CANCELED || prStatus == PaymentRequestStatus.CANCELED
    val isFailed = prStatus == PaymentRequestStatus.FAILED
    val isExpired =
      sessionStatus == PaymentSessionStatus.EXPIRED || prStatus == PaymentRequestStatus.EXPIRED

    when {
      isSuccess -> {
        viewModel.resetForNewSession()
        onResult(
          XenditPaymentResult.Success(
            paymentRequestId = poll.session?.paymentSessionId,
            channelCode = poll.succeededChannel?.channelCode ?: poll.paymentRequest?.channelCode
          )
        )
        onCleanup()
      }

      isCanceled -> {
        viewModel.resetForNewSession()
        onResult(XenditPaymentResult.Canceled)
        onCleanup()
      }

      isExpired -> {
        viewModel.resetForNewSession()
        onResult(XenditPaymentResult.Expired)
        onCleanup()
      }

      isFailed -> {
        val pollFailureCode = poll.paymentRequest?.failure_code
        val pollFailureMessage =
          FailureCodeMessageUtil.resolveFailureMessage(context, pollFailureCode)

        pendingSnackbarMessage = pollFailureMessage
        onResult(
          XenditPaymentResult.Failed(
            XenditError(
              code = pollFailureCode?.trim().takeIf { !it.isNullOrBlank() } ?: "UNKNOWN",
              message = pollFailureMessage,
              cause = Throwable("Payment failed Session: $sessionStatus, PR: $prStatus")
            )
          )
        )
        viewModel.dispatch(ActionIntent.CloseWebPayment)
      }
    }
  }

  LaunchedEffect(sessionAuthKey, publicKey, mviState.paymentSessionId) {
    val paymentSessionId = mviState.paymentSessionId ?: return@LaunchedEffect
    cardViewModel.dispatch(
      CardIntent.ConfigureSession(
        sessionAuthKey = sessionAuthKey,
        publicKey = publicKey,
        paymentSessionId = paymentSessionId
      )
    )
  }

  DisposableEffect(Unit) {
    onDispose {
      viewModel.resetForNewSession()
    }
  }

  val container: @Composable (@Composable () -> Unit) -> Unit = { content ->
    when (presentation) {
      PaymentContainerPresentation.Dialog -> {
        Dialog(
          onDismissRequest = dismiss,
          properties =
            DialogProperties(
              dismissOnBackPress = true,
              dismissOnClickOutside = false,
              usePlatformDefaultWidth = false
            )
        ) {
          content()
        }
      }

      PaymentContainerPresentation.BottomSheet -> {
        ModalBottomSheet(
          onDismissRequest = dismiss,
          sheetState = sheetState!!,
          containerColor = style.colorBackground
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .fillMaxHeight(0.8f)
          ) {
            content()
          }
        }
      }
    }
  }

  container {
    Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      containerColor = style.colorBackground,
      modifier = Modifier
        .fillMaxSize()
        .imePadding()
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
          if (BuildConfig.DEBUG) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(appearance.borderRadius))
                .background(Color(0xFFF7F7F7))
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Column {
                Text(
                  text = "Debug = ${BuildConfig.DEBUG}",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.Gray
                )
                Text(
                  text = mviState.sessionResponse?.session?.referenceId ?: "",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.Gray
                )
              }
            }
          }
          GenericHeader(
            title = stringResource(id = R.string.sessionpayment_methods_header),
            onLeftClick = dismiss
          )

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
          ) {
            when {
              mviState.paymentActionRedirect != null -> {
                val redirect = mviState.paymentActionRedirect!!
                val url = redirect.value.orEmpty()
                if (redirect.descriptor == "DEEPLINK_URL") {
                  LaunchedEffect(url) {
                    if (url.isNotBlank()) {
                      val didLaunch = runCatching {
                        context.startActivity(
                          Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                          }
                        )
                      }.isSuccess
                      if (didLaunch) {
                        viewModel.showLoading()
                      }
                    }
                    viewModel.dispatch(ActionIntent.ClearPaymentActionRedirect)
                  }
                } else {
                  ActionWebViewUI(
                    url = url,
                    onClose = {
                      viewModel.dispatch(ActionIntent.CloseWebPayment)
                    },
                    onChallengeCompleted = { viewModel.dispatch(ActionIntent.ChallengeCompleted(true)) },
                    iframeCapable = redirect.iframeCapable ?: true
                  )
                }
              }

              mviState.presentToCustomerPaymentAction != null -> {
                val qrAction = mviState.presentToCustomerPaymentAction!!
                val merchantName = mviState.sessionResponse?.business?.name
                ActionQrUI(
                  title = merchantName,
                  channelName = mviState.selectedChannel?.brandName ?: "QR Code",
                  channelLogoUrl = mviState.selectedChannel?.brandLogoUrl,
                  qrString = qrAction.value.orEmpty(),
                  amount = mviState.sessionResponse?.session?.amount,
                  currency = mviState.sessionResponse?.session?.currency,
                  onClose = { viewModel.markClosed() },
                  onPaymentMade = {
                    viewModel.dispatch(ActionIntent.SimulatePayment)
                    viewModel.dispatch(ActionIntent.ChallengeCompleted(true))
                    viewModel.showLoading()
                  },
                  snackbarHostState = snackbarHostState
                )
              }

              mviState.channels.isNotEmpty() -> {
                Column {
                  Column(
                    modifier = Modifier
                      .weight(1f)
                      .verticalScroll(rememberScrollState())
                  ) {
                    val selectedUiGroup by rememberUpdatedState(mviState.selectedChannel?.uiGroup)
                    val installmentPlans by rememberUpdatedState(cardState.installmentPlans)
                    val onToggleGroup: (String) -> Unit =
                      remember(viewModel) { { viewModel.dispatch(ActionIntent.ToggleUiGroup(it)) } }
                    val onSelectChannel: (String) -> Unit =
                      remember(viewModel) { { viewModel.dispatch(ActionIntent.SelectChannel(it)) } }
                    val onCardNumberChanged: (String) -> Unit =
                      remember(cardViewModel) { { cardViewModel.dispatch(CardIntent.CardNumberChanged(it)) } }
                    val onFormChanged:
                      (String?, Map<String, String>, List<ChannelFormField>, Boolean) -> Unit =
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
                                  if (selectedUiGroup == XenditComponents.UiGroup.CARDS) installmentPlans else null
                              )
                            )
                          )
                        }
                      }
                    PaymentMethodsUI(
                      session = mviState.sessionResponse?.session,
                      bffBusiness = mviState.sessionResponse?.business,
                      merchantPreferredPaymentMethod = merchantPreferredPaymentMethod,
                      channels = mviState.channels,
                      channelVariantsByDisplayCode = mviState.channelVariantsByDisplayCode,
                      expandedUiGroup = mviState.expandedUiGroup,
                      selectedChannel = mviState.selectedChannel,
                      paymentDrafts = mviState.paymentDrafts,
                      cardDetails = cardState.cardDetails,
                      installmentPlans = cardState.installmentPlans,
                      sessionType = mviState.sessionType,
                      allowSavePaymentMethod = mviState.allowSavePaymentMethod,
                      onToggleGroup = onToggleGroup,
                      onSelectChannel = onSelectChannel,
                      onCardNumberChanged = onCardNumberChanged,
                      onFormChanged = onFormChanged
                    )
                  }

                  val isPaymentSelected =
                    mviState.expandedUiGroup != null && mviState.selectedChannel != null
                  val selectedChannel = mviState.selectedChannel
                  val currentDraft = if (selectedChannel == null) PaymentDraft() else {
                    mviState.paymentDrafts[selectedChannel.channelCode]
                      ?: PaymentDraft(channelCode = selectedChannel.channelCode)
                  }
                  val isFormFilled = currentDraft.visibleFields
                  val formValue = currentDraft.formValues
                  val isPayEnabled =
                    isPaymentSelected && !mviState.isLoading && validateAllField(
                      isFormFilled,
                      formValue,
                      cardDetails = cardState.cardDetails,
                      bffCardInfo = mviState.selectedChannel?.card
                    )
                  val payText =
                    if (mviState.sessionType == BffSessionType.SAVE) {
                      stringResource(id = R.string.sessionpayment_methods_submit_add_payment_method)
                    } else {
                      val channelName = mviState.selectedChannel?.brandName ?: "Payment"
                      stringResource(id = R.string.sessionpayment_methods_submit_pay)
                    }

                  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                      enabled = isPayEnabled,
                      onClick = {
                        val selected = mviState.selectedChannel ?: return@Button
                        val draft = mviState.paymentDrafts[selected.channelCode]
                          ?: PaymentDraft(channelCode = selected.channelCode)
                        val installmentPlans =
                          if (selected.uiGroup == XenditComponents.UiGroup.CARDS) cardState.installmentPlans else draft.installmentPlans
                        viewModel.dispatch(
                          ActionIntent.SubmitAction(
                            channelCode = selected.channelCode,
                            formValues = draft.formValues,
                            fields = draft.visibleFields,
                            savePaymentMethod = draft.savePaymentMethod,
                            installmentPlans = installmentPlans
                          )
                        )
                      },
                      modifier = Modifier.fillMaxWidth(),
                      shape = RoundedCornerShape(appearance.borderRadius),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = style.colorPrimary,
                        contentColor = style.colorBackground
                      )
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically
                      ) {
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
              }
            }
          }
        }

        if (mviState.errorMessage != null && mviState.sessionResponse == null) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AlertDialog(
              onDismissRequest = { onCleanup() },
              title = { Text(stringResource(id = R.string.sessiondefault_error_title)) },
              text = { Text(mviState.errorMessage ?: "") },
              confirmButton = {
                Button(onClick = { onCleanup() }) {
                  Text(stringResource(R.string.sessiondialog_close))
                }
              }
            )
          }
        }
        if (mviState.isAwaitingPaymentAction) {
          AwaitingPaymentDialog(
            appearance = style,
            channelName = mviState.selectedChannel?.brandName.orEmpty(),
            channelLogoUrl = mviState.selectedChannel?.brandLogoUrl,
            onClose = { viewModel.dispatch(ActionIntent.CloseWebPayment) }
          )
        }
        if (mviState.isLoading) {
          Box(
            modifier = Modifier
              .matchParentSize()
              .background(Color.Black.copy(alpha = 0.08f))
              .pointerInteropFilter { true },
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator()
          }
        }
      }
    }
  }
}
