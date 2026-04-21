package co.xendit.paymentsdk.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.xendit.paymentsdk.BuildConfig
import co.xendit.paymentsdk.R
import co.xendit.paymentsdk.core.CoreSdkComponent.globalErrorHandler
import co.xendit.paymentsdk.data.model.PaymentDraft
import co.xendit.paymentsdk.data.model.XenditError
import co.xendit.paymentsdk.data.model.XenditPaymentResult
import co.xendit.paymentsdk.internal_entry_point.CardViewModelFactory
import co.xendit.paymentsdk.internal_entry_point.PaymentViewModelFactory
import co.xendit.paymentsdk.ui.action.ActionQrUI
import co.xendit.paymentsdk.ui.action.ActionWebViewUI
import co.xendit.paymentsdk.ui.card.CardIntent
import co.xendit.paymentsdk.ui.card.CardViewModel
import co.xendit.paymentsdk.ui.components.molecule.GenericHeader
import co.xendit.paymentsdk.ui.helper.FormChecker.validateAllField
import co.xendit.paymentsdk.ui.method.PaymentMethodsUI
import co.xendit.paymentsdk.ui.style.XenditAppearance
import co.xendit.paymentsdk.ui.style.xenditAppearance
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
        viewModel.markClosed()
        onResult(XenditPaymentResult.Canceled)
        onCleanup()
      }
    } else {
      viewModel.markClosed()
      onResult(XenditPaymentResult.Canceled)
      onCleanup()
    }
  }

  LaunchedEffect(sessionAuthKey, publicKey) {
    viewModel.dispatch(ActionIntent.Initialize(sessionAuthKey, publicKey))
  }

  LaunchedEffect(Unit) {
    globalErrorHandler.apiErrorFlow.collect { (_, message) ->
      message?.let { snackbarHostState.showSnackbar(it.asString(context)) }
      onResult(
        XenditPaymentResult.Failed(
          XenditError(
            code = "001",
            message = message.toString(),
            cause = Throwable(message.toString())
          )
        )
      )
    }
  }

  LaunchedEffect(mviState.pollResponse) {
    val poll = mviState.pollResponse ?: return@LaunchedEffect
    val sessionStatus = poll.session?.status
    val prStatus = poll.paymentRequest?.status
    val isSuccess =
      sessionStatus == "COMPLETED" || prStatus == "SUCCEEDED" || poll.succeededChannel != null
    val isCanceled = sessionStatus == "CANCELED" || prStatus == "CANCELED"
    val isFailed =
      sessionStatus == "EXPIRED" || sessionStatus == "FAILED" || prStatus == "FAILED" || prStatus == "EXPIRED"

    when {
      isSuccess -> {
        onResult(
          XenditPaymentResult.Success(
            paymentRequestId = poll.paymentRequest?.id,
            channelCode = poll.succeededChannel?.channelCode
          )
        )
        viewModel.markClosed()
        onCleanup()
      }

      isCanceled -> {
        onResult(XenditPaymentResult.Canceled)
        viewModel.markClosed()
        onCleanup()
      }

      isFailed -> {
        viewModel.markClosed()
        onResult(
          XenditPaymentResult.Failed(
            XenditError(
              code = "123",
              message = "Payment failed or expired. Session: $sessionStatus, PR: $prStatus",
              cause = Throwable("Payment failed or expired. Session: $sessionStatus, PR: $prStatus")
            )
          )
        )
        viewModel.markClosed()
      }
    }
  }

  LaunchedEffect(mviState.expandedUiGroup, mviState.selectedChannel?.channelCode) {
    viewModel.dispatch(
      ActionIntent.UpdatePaymentDraft(
        PaymentDraft(channelCode = mviState.selectedChannel?.channelCode)
      )
    )
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
          containerColor = style.colorBackground ?: Color.White
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
      containerColor = style.colorBackground ?: Color.White,
      modifier = Modifier
        .fillMaxSize()
        .imePadding()
    ) { paddingValues ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
      ) {
        if (BuildConfig.DEBUG) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(appearance.borderRadius))
              .background(Color(0xFFF7F7F7))
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .clickable {
                context.startActivity(OkHttpProfilerSettingsActivity.getIntent(context))
              }
          ) {
            Column {
              Text(
                text = "Debug = ${BuildConfig.DEBUG}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
              )
              Text(
                text = mviState.sessionResponse?.referenceId ?: "",
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
        val showFooter =
          mviState.actionRedirectUrl == null &&
              mviState.presentToCustomerPaymentAction == null &&
              mviState.channels.isNotEmpty()

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          when {
            mviState.actionRedirectUrl != null -> {
              ActionWebViewUI(
                url = mviState.actionRedirectUrl!!,
                onClose = dismiss,
                onChallengeCompleted = { viewModel.dispatch(ActionIntent.ChallengeCompleted) },
                iframeCapable = mviState.iframeCapable
              )
            }

            mviState.presentToCustomerPaymentAction != null -> {
              val qrAction = mviState.presentToCustomerPaymentAction!!
              ActionQrUI(
                title = qrAction.actionSubtitle ?: qrAction.actionTitle,
                channelName = mviState.selectedChannel?.brandName ?: "QR Code",
                channelLogoUrl = mviState.selectedChannel?.brandLogoUrl,
                qrString = qrAction.value.orEmpty(),
                amount = mviState.sessionResponse?.amount,
                currency = mviState.sessionResponse?.currency,
                onClose = { viewModel.markClosed() },
                onPaymentMade = { viewModel.dispatch(ActionIntent.ChallengeCompleted) }
              )
            }

            mviState.channels.isNotEmpty() -> {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState())
              ) {
                PaymentMethodsUI(
                  session = mviState.sessionResponse,
                  merchantPreferredPaymentMethod = merchantPreferredPaymentMethod,
                  channels = mviState.channels,
                  expandedUiGroup = mviState.expandedUiGroup,
                  selectedChannel = mviState.selectedChannel,
                  cardDetails = cardState.cardDetails,
                  installmentPlans = cardState.installmentPlans,
                  sessionType = mviState.sessionType,
                  allowSavePaymentMethod = mviState.allowSavePaymentMethod,
                  onToggleGroup = { viewModel.dispatch(ActionIntent.ToggleUiGroup(it)) },
                  onSelectChannel = { viewModel.dispatch(ActionIntent.SelectChannel(it)) },
                  onCardNumberChanged = { cardViewModel.dispatch(CardIntent.CardNumberChanged(it)) },
                  onFormChanged = { channelCode, formValues, visibleFields, save ->
                    viewModel.dispatch(
                      ActionIntent.UpdatePaymentDraft(
                        PaymentDraft(
                          channelCode = channelCode,
                          formValues = formValues,
                          visibleFields = visibleFields,
                          savePaymentMethod = save,
                          installmentPlans =
                            if (mviState.selectedChannel?.uiGroup == "cards") cardState.installmentPlans else null
                        )
                      )
                    )
                  }
                )
              }
            }
          }
        }

        if (showFooter) {
          val isPaymentSelected =
            mviState.expandedUiGroup != null && mviState.selectedChannel != null
          val isFormFilled = mviState.paymentDraft.visibleFields
          val formValue = mviState.paymentDraft.formValues
          val isPayEnabled =
            isPaymentSelected && !mviState.isLoading && validateAllField(isFormFilled, formValue)
          val payText =
            if (mviState.sessionType == "SAVE") {
              stringResource(id = R.string.sessionpayment_methods_submit_add_payment_method)
            } else {
              val channelName = mviState.selectedChannel?.brandName ?: "Payment"
              // TODO: need lokalise - "Pay with $channelName"
              "Pay with $channelName"
            }

          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Button(
              enabled = isPayEnabled,
              onClick = {
                val selected = mviState.selectedChannel ?: return@Button
                val draft = mviState.paymentDraft
                val installmentPlans =
                  if (selected.uiGroup == "cards") cardState.installmentPlans else draft.installmentPlans
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
                  contentDescription = "Back",
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }

        if (mviState.isLoading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }

        if (mviState.errorMessage != null) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AlertDialog(
              onDismissRequest = { onCleanup() },
              title = { Text(stringResource(id = R.string.sessiondefault_error_title)) },
              text = { Text(mviState.errorMessage ?: "") },
              confirmButton = {
                Button(onClick = { onCleanup() }) {
                  // TODO: need lokalise - "OK"
                  Text("OK")
                }
              }
            )
          }
        }
      }
    }
  }
}
