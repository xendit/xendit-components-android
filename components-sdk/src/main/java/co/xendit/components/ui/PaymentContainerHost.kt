package co.xendit.components.ui

import android.widget.Toast
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
import co.xendit.components.BuildConfig
import co.xendit.components.R
import co.xendit.components.core.CoreSdkComponent.globalErrorHandler
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
import co.xendit.components.ui.components.molecule.GenericHeader
import co.xendit.components.ui.helper.FormChecker.validateAllField
import co.xendit.components.ui.method.PaymentMethodsUI
import co.xendit.components.ui.style.XenditAppearance
import co.xendit.components.ui.style.xenditAppearance
import io.nerdythings.okhttp.modifier.settings.OkHttpProfilerSettingsActivity
import kotlinx.coroutines.launch
import java.util.Locale

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
  val pollFailureCode = mviState.pollResponse?.paymentRequest?.failure_code?.trim().orEmpty()
  val pollFailureMessage = resolveFailureMessage(failureCode = pollFailureCode)

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
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
    val isExpired = sessionStatus == PaymentSessionStatus.EXPIRED || prStatus == PaymentRequestStatus.EXPIRED

    when {
      isSuccess -> {
        viewModel.resetForNewSession()
        onResult(
          XenditPaymentResult.Success(
            paymentRequestId = poll.session?.paymentSessionId,
            channelCode = poll.succeededChannel?.channelCode
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
        viewModel.markClosed()
        viewModel.dispatch(ActionIntent.CloseWebPayment)
        val failureCode = pollFailureCode
        val message = pollFailureMessage
        onResult(
          XenditPaymentResult.Failed(
            XenditError(
              code = failureCode.ifBlank { "UNKNOWN" },
              message = message,
              cause = Throwable("Payment failed. Session: $sessionStatus, PR: $prStatus")
            )
          )
        )
        snackbarHostState.showSnackbar(message)
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
            mviState.actionRedirectUrl != null -> {
              ActionWebViewUI(
                url = mviState.actionRedirectUrl!!,
                onClose = {
                  viewModel.dispatch(ActionIntent.CloseWebPayment)
                },
                onChallengeCompleted = { viewModel.dispatch(ActionIntent.ChallengeCompleted(true)) },
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
                amount = mviState.sessionResponse?.session?.amount,
                currency = mviState.sessionResponse?.session?.currency,
                onClose = { viewModel.markClosed() },
                onPaymentMade = { viewModel.dispatch(ActionIntent.ChallengeCompleted(true)) }
              )
            }

            mviState.channels.isNotEmpty() -> {
              Column {
                Column(
                  modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                ) {
                  PaymentMethodsUI(
                    session = mviState.sessionResponse?.session,
                    merchantPreferredPaymentMethod = merchantPreferredPaymentMethod,
                    channels = mviState.channels,
                    expandedUiGroup = mviState.expandedUiGroup,
                    selectedChannel = mviState.selectedChannel,
                    paymentDrafts = mviState.paymentDrafts,
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

                val isPaymentSelected =
                  mviState.expandedUiGroup != null && mviState.selectedChannel != null
                val selectedChannelCode = mviState.selectedChannel?.channelCode
                val currentDraft = if (selectedChannelCode == null) PaymentDraft() else {
                  mviState.paymentDrafts[selectedChannelCode]
                    ?: PaymentDraft(channelCode = selectedChannelCode)
                }
                val isFormFilled = currentDraft.visibleFields
                val formValue = currentDraft.formValues
                val isPayEnabled =
                  isPaymentSelected && !mviState.isLoading && validateAllField(
                    isFormFilled,
                    formValue
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

        if (mviState.isLoading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
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
      }
    }
  }
}

@Composable
private fun resolveFailureMessage(
  failureCode: String,
): String {
  val raw = failureCode.trim()
  val normalized = raw.uppercase(Locale.US)
  val resId = failureCodeToResIdOrNull(normalized)

  return if (resId != null) {
    stringResource(id = resId)
  } else {
    val code = raw.ifBlank { "UNKNOWN" }
    stringResource(id = R.string.sessionfailure_code_unknown)
      .replace("{{failureCode}}", code)
  }
}

private fun failureCodeToResIdOrNull(
  normalizedFailureCode: String,
): Int? {
  return when (normalizedFailureCode) {
    "ACCOUNT_ACCESS_BLOCKED" -> R.string.sessionfailure_code_account_access_blocked
    "ACCOUNT_ALREADY_LINKED" -> R.string.sessionfailure_code_account_already_linked
    "ACCOUNT_NOT_ACTIVATED" -> R.string.sessionfailure_code_account_not_activated
    "AUTHENTICATION_FAILED" -> R.string.sessionfailure_code_authentication_failed
    "CAPTURE_AMOUNT_EXCEEDED" -> R.string.sessionfailure_code_capture_amount_exceeded
    "CARD_DECLINED" -> R.string.sessionfailure_code_card_declined
    "CHANNEL_UNAVAILABLE" -> R.string.sessionfailure_code_channel_unavailable
    "DECLINED_BY_ISSUER" -> R.string.sessionfailure_code_declined_by_issuer
    "DECLINED_BY_PROCESSOR" -> R.string.sessionfailure_code_declined_by_processor
    "EXPIRED_CARD" -> R.string.sessionfailure_code_expired_card
    "EXPIRED_OTP" -> R.string.sessionfailure_code_expired_otp
    "FAILURE_DETAILS_UNAVAILABLE" -> R.string.sessionfailure_code_failure_details_unavailable
    "INACTIVE_OR_UNAUTHORIZED_CARD" -> R.string.sessionfailure_code_inactive_or_unauthorized_card
    "INSUFFICIENT_BALANCE" -> R.string.sessionfailure_code_insufficient_balance
    "INVALID_ACCOUNT_DETAILS" -> R.string.sessionfailure_code_invalid_account_details
    "INVALID_CVV" -> R.string.sessionfailure_code_invalid_cvv
    "INVALID_MERCHANT_SETTINGS" -> R.string.sessionfailure_code_invalid_merchant_settings
    "INVALID_OTP" -> R.string.sessionfailure_code_invalid_otp
    "INVALID_TOKEN" -> R.string.sessionfailure_code_invalid_token
    "ISSUER_UNAVAILABLE" -> R.string.sessionfailure_code_issuer_unavailable
    "OTP_ATTEMPT_COUNTS_EXCEEDED" -> R.string.sessionfailure_code_otp_attempt_counts_exceeded
    "PARTNER_TIMEOUT_ERROR" -> R.string.sessionfailure_code_partner_timeout_error
    "PAYMENT_AMOUNT_LIMITS_EXCEEDED" -> R.string.sessionfailure_code_payment_amount_limits_exceeded
    "PAYMENT_ATTEMPT_COUNTS_EXCEEDED" -> R.string.sessionfailure_code_payment_attempt_counts_exceeded
    "PAYMENT_REQUEST_EXPIRED" -> R.string.sessionfailure_code_payment_request_expired
    "PROCESSOR_ERROR" -> R.string.sessionfailure_code_processor_error
    "SERVER_ERROR" -> R.string.sessionfailure_code_server_error
    "STOLEN_CARD" -> R.string.sessionfailure_code_stolen_card
    "SUSPECTED_FRAUDULENT" -> R.string.sessionfailure_code_suspected_fraudulent
    "TIMEOUT_ERROR" -> R.string.sessionfailure_code_timeout_error
    "USER_DECLINED_PAYMENT" -> R.string.sessionfailure_code_user_declined_payment
    "USER_DEVICE_UNREACHABLE" -> R.string.sessionfailure_code_user_device_unreachable
    "USER_DID_NOT_AUTHORIZE" -> R.string.sessionfailure_code_user_did_not_authorize
    else -> null
  }
}
