package co.xendit.components.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.xendit.components.XenditComponentsPaymentType
import co.xendit.components.core.CoreSdkComponent
import co.xendit.components.core.model.GlobalErrorHandler
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.BffSessionAllowSavePaymentMethod
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.Country
import co.xendit.components.data.model.FieldType
import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.data.model.PaymentAction
import co.xendit.components.data.model.PaymentActionDescriptor
import co.xendit.components.data.model.PaymentDraft
import co.xendit.components.data.model.PaymentRequest
import co.xendit.components.data.model.PaymentRequestStatus
import co.xendit.components.data.model.PaymentResponse
import co.xendit.components.data.model.PaymentSessionStatus
import co.xendit.components.data.model.PollResponse
import co.xendit.components.data.model.SessionResponse
import co.xendit.components.data.model.SimulatePaymentRequest
import co.xendit.components.data.model.isPaySession
import co.xendit.components.data.model.primaryChannelPropertyKey
import co.xendit.components.data.network.repo.session.XenditRepository
import co.xendit.components.telemetry.SessionTelemetry
import co.xendit.components.ui.components.molecule.UiText
import co.xendit.components.util.PaymentRequestMapper
import co.xendit.components.util.XLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal data class PaymentState(
  val isLoading: Boolean = false,
  val awaitingPaymentAction: AwaitingPaymentAction? = null,
  val channels: List<BffChannel> = emptyList(),
  val channelVariantsByDisplayCode: Map<String, ChannelVariantChannels> = emptyMap(),
  val expandedUiGroup: String? = null,
  val selectedChannel: BffChannel? = null,
  val paymentSessionId: String? = null,
  val paymentActionRedirect: PaymentAction? = null,
  val presentToCustomerPaymentAction: PaymentAction? = null,
  val errorMessage: String? = null,
  val paymentResponse: PaymentResponse? = null,
  val sessionResponse: SessionResponse? = null,
  val pollResponse: PollResponse? = null,
  val sessionType: BffSessionType? = null,
  val allowSavePaymentMethod: BffSessionAllowSavePaymentMethod? = null,
  val paymentDrafts: Map<String, PaymentDraft> = emptyMap(),
  val formWipeNonce: Int = 0
)

internal sealed interface AwaitingPaymentAction {
  data object Deeplink : AwaitingPaymentAction
  data object EmptyPaymentActions : AwaitingPaymentAction
  data object GooglePayProcessing : AwaitingPaymentAction
}

internal sealed interface PaymentFlowState {
  data object LoadingSession : PaymentFlowState
  data object SelectingPaymentMethod : PaymentFlowState
  data class Redirecting(val action: PaymentAction) : PaymentFlowState
  data class PresentingCustomerAction(val action: PaymentAction) : PaymentFlowState
  data class StartupError(val message: String) : PaymentFlowState
}

internal sealed interface PaymentOverlayState {
  data object Loading : PaymentOverlayState
  data class AwaitingAction(val action: AwaitingPaymentAction) : PaymentOverlayState
}

internal data class ChannelVariantChannels(
  val saveChannel: BffChannel? = null,
  val nonSaveChannel: BffChannel? = null
)

internal val PaymentState.flowState: PaymentFlowState
  get() = when {
    paymentActionRedirect != null -> PaymentFlowState.Redirecting(paymentActionRedirect)
    presentToCustomerPaymentAction != null ->
      PaymentFlowState.PresentingCustomerAction(presentToCustomerPaymentAction)
    errorMessage != null && sessionResponse == null -> PaymentFlowState.StartupError(errorMessage)
    channels.isNotEmpty() -> PaymentFlowState.SelectingPaymentMethod
    else -> PaymentFlowState.LoadingSession
  }

internal val PaymentState.overlayState: PaymentOverlayState?
  get() = when {
    awaitingPaymentAction != null -> PaymentOverlayState.AwaitingAction(awaitingPaymentAction)
    isLoading -> PaymentOverlayState.Loading
    else -> null
  }

internal data class CombinedChannelsResult(
  val channels: List<BffChannel>,
  val variantsByDisplayCode: Map<String, ChannelVariantChannels>
)

internal fun combinePairedChannels(channels: List<BffChannel>): CombinedChannelsResult {
  if (channels.isEmpty()) return CombinedChannelsResult(emptyList(), emptyMap())

  data class ChannelCombineKey(
    val uiGroup: String,
    val brandName: String,
    val pmType: XenditComponentsPaymentType?,
    val requiresCustomerDetails: Boolean?,
  )

  fun toKey(channel: BffChannel): ChannelCombineKey {
    return ChannelCombineKey(
      uiGroup = channel.uiGroup,
      brandName = channel.brandName,
      pmType = channel.pmType,
      requiresCustomerDetails = channel.requiresCustomerDetails,
    )
  }

  val grouped = channels.groupBy(::toKey)
  val pairByKey: Map<ChannelCombineKey, Pair<BffChannel, BffChannel>> =
    grouped.mapNotNull { (key, group) ->
      if (group.size != 2) return@mapNotNull null
      val save = group.firstOrNull { it.allowSave }
      val nonSave = group.firstOrNull { !it.allowSave }
      if (save != null && nonSave != null) key to (nonSave to save) else null
    }.toMap()

  val combinedChannels = mutableListOf<BffChannel>()
  val variants = mutableMapOf<String, ChannelVariantChannels>()
  val addedDisplayCodes = mutableSetOf<String>()

  channels.forEach { channel ->
    val key = toKey(channel)
    val pair = pairByKey[key]
    if (pair == null) {
      combinedChannels.add(channel)
    } else {
      val display = pair.first
      if (addedDisplayCodes.add(display.channelCode)) {
        combinedChannels.add(display)
        variants[display.channelCode] =
          ChannelVariantChannels(saveChannel = pair.second, nonSaveChannel = pair.first)
      }
    }
  }

  return CombinedChannelsResult(combinedChannels, variants.toMap())
}

/**
 * Actions for system to update the payment state.
 */
internal sealed class ActionIntent {
  data class Initialize(val sessionAuthKey: String, val publicKey: String) : ActionIntent()
  data class FetchSession(val sessionAuthKey: String) : ActionIntent()

  /**
   * Expands or collapses a payment method category (e.g., "cards", "qr_code").
   * Also selects the first available channel in that group by default.
   */
  data class ToggleUiGroup(val uiGroup: String) : ActionIntent()

  /**
   * This is primarily used for dropdown selections where multiple channels exist in one group.
   */
  data class SelectChannel(val channelCode: String) : ActionIntent()

  /**
   * Updates the current payment draft with form values and field visibility.
   */
  data class UpdatePaymentDraft(val paymentDraft: PaymentDraft) : ActionIntent()

  /**
   * Triggers the actual payment processing or card saving.
   */
  data class SubmitAction(
    val channelCode: String,
    val formValues: Map<String, String>,
    val fields: List<ChannelFormField>,
    val savePaymentMethod: Boolean,
    val installmentPlans: List<InstallmentPlan>? = null
  ) : ActionIntent()

  /**
   * Submits a Google Pay payment using the signed payment data JSON from Google.
   *
   * [paymentMethodType] is the raw `paymentMethodData.type` extracted from Google's JSON
   * (e.g. "CARD", "PAYPAL"). It is resolved against `digitalWallets.google_pay.allowed_payment_methods[i].payment_method_specification.type`
   * inside this ViewModel — resolution errors are propagated to `_state.errorMessage` and
   * `globalErrorHandler` instead of crashing.
   */
  data class SubmitGooglePay(
    val paymentDataJson: String,
    val paymentMethodType: String? = null
  ) : ActionIntent()

  /**
   * Raises a Google Pay UI error to the MVI state without a payment submission attempt.
   * This is triggered by SDK-level failures such as canceled resolutions, buyer account
   * errors, developer configuration errors, temporary internal errors, or unknown
   * failures from loadPaymentData / its resolution activity result.
   */
  data class GooglePayPaymentFailed(
    val code: String,
    val title: String,
    val message: String
  ) : ActionIntent()

  /**
   * Calls the simulate endpoint to advance the payment state before polling for the result.
   * This is typically used for non-production and QR-based payment flows.
   */
  data object SimulatePayment : ActionIntent()

  /**
   * This triggers a status check to verify the final result.
   */
  data class ChallengeCompleted(val forceStart: Boolean = false) : ActionIntent()

  /**
   * Close Webview
   */
  data object CloseWebPayment : ActionIntent()

  data object ClearPaymentActionRedirect : ActionIntent()
  data class NotifyCopyText(val fieldName: String) : ActionIntent()

}

internal class PaymentViewModel(
  private val xenditRepository: XenditRepository,
  private val globalErrorHandler: GlobalErrorHandler,
  private val telemetry: SessionTelemetry,
  private val sessionRuntime: PaymentSessionRuntime = PaymentSessionRuntime(),
  private val sessionLoader: PaymentSessionLoader = PaymentSessionLoader(xenditRepository),
  private val submissionCoordinator: PaymentSubmissionCoordinator = PaymentSubmissionCoordinator(xenditRepository),
  private val pollingCoordinator: PaymentPollingCoordinator = PaymentPollingCoordinator(xenditRepository)
) : ViewModel() {
  private val telemetryCoordinator = PaymentTelemetryCoordinator(telemetry)

  init {
    // Warm up country data as early as possible
    viewModelScope.launch(Dispatchers.Default) { Country.warmUp() }
  }

  override fun onCleared() {
    super.onCleared()
    telemetryCoordinator.onCleared()
    wipeAllSensitiveData()
  }

  private val _state = MutableStateFlow(PaymentState())
  val state: StateFlow<PaymentState> = _state.asStateFlow()

  private var challengePollingJob: Job? = null

  fun dispatch(intent: ActionIntent) {
    when (intent) {
      is ActionIntent.Initialize -> {
        wipeAllSensitiveData()
        sessionRuntime.initialize(
          sessionAuthKey = intent.sessionAuthKey,
          publicKey = intent.publicKey
        )
        dispatch(ActionIntent.FetchSession(intent.sessionAuthKey))
      }

      is ActionIntent.FetchSession -> fetchSessionInternal(intent.sessionAuthKey)
      is ActionIntent.ToggleUiGroup -> toggleUiGroupInternal(intent.uiGroup)
      is ActionIntent.SelectChannel -> selectChannelInternal(intent.channelCode)
      is ActionIntent.SubmitAction ->
        processPaymentInternal(
          intent.channelCode,
          intent.formValues,
          intent.fields,
          intent.savePaymentMethod,
          intent.installmentPlans
        )

      is ActionIntent.SubmitGooglePay ->
        submitGooglePayInternal(
          paymentDataJson = intent.paymentDataJson,
          paymentMethodType = intent.paymentMethodType
        )

      is ActionIntent.GooglePayPaymentFailed ->
        onGooglePayPaymentFailedInternal(
          code = intent.code,
          title = intent.title,
          message = intent.message
        )

      is ActionIntent.UpdatePaymentDraft -> onUpdatePaymentDraft(intent.paymentDraft)
      is ActionIntent.SimulatePayment -> onSimulatePayment()
      is ActionIntent.ChallengeCompleted -> onChallengeCompletedInternal(intent.forceStart)
      is ActionIntent.CloseWebPayment -> {
        // Delegate state reset + CHECKOUT_ACTION_CLOSE + action scope pop to markClosed(), which is now
        telemetryCoordinator.resetRedirectExpectation()
        markClosed()
        _state.update {
          it.copy(
            paymentResponse = null,
            pollResponse = null,
          )
        }
      }

      is ActionIntent.ClearPaymentActionRedirect -> {
        _state.update { it.copy(paymentActionRedirect = null) }
      }

      is ActionIntent.NotifyCopyText -> notifyCopyText(intent.fieldName)
    }
  }

  private fun fetchSessionInternal(sessionAuthKey: String) {
    viewModelScope.launch {
      _state.update { it.copy(isLoading = true, errorMessage = null) }
      try {
        when (val result = sessionLoader.load(sessionAuthKey)) {
          is PaymentSessionLoadResult.Success -> {
            val data = result.data
            sessionRuntime.updatePaymentSessionId(data.paymentSessionId)

            telemetryCoordinator.onSessionLoadedSuccess(
              selectableChannelCodes = data.selectableChannelCodes,
              paymentSessionId = data.paymentSessionId,
              sessionStatus = data.sessionStatus
            )

            if (data.channels.isNotEmpty()) {
              _state.update {
                it.copy(
                  isLoading = false,
                  channels = data.channels,
                  channelVariantsByDisplayCode = data.channelVariantsByDisplayCode,
                  paymentSessionId = data.paymentSessionId,
                  sessionResponse = data.sessionResponse,
                  errorMessage = null,
                  sessionType = data.sessionType,
                  allowSavePaymentMethod = data.allowSavePaymentMethod
                )
              }
            } else {
              _state.update { it.copy(isLoading = false, sessionResponse = data.sessionResponse) }
            }
          }

          is PaymentSessionLoadResult.Failure -> {
            telemetryCoordinator.onSessionLoadedFailure()
            _state.update {
              it.copy(
                isLoading = false,
                errorMessage = if (result.errorCode == "NETWORK_ERROR") null else result.message
              )
            }
          }
        }
      } catch (e: Exception) {
        telemetryCoordinator.onSessionLoadedFailure()
        globalErrorHandler.postError(
          errorMessage = UiText.DynamicString(e.message ?: "Failed to fetch session")
        )
          _state.update {
          it.copy(isLoading = false, errorMessage = e.message ?: "Failed to fetch session")
        }
      }
    }
  }

  private fun toggleUiGroupInternal(uiGroup: String) {
    val channels = _state.value.channels
    val groups = channels.groupBy { it.uiGroup }
    val currentExpanded = _state.value.expandedUiGroup
    val newExpandedUiGroup = if (currentExpanded == uiGroup) null else uiGroup
    val currentSelected = _state.value.selectedChannel
    val groupChannelCodes = groups[uiGroup]?.map { it.channelCode }

    // ---- TELEMETRY: ChannelGroup scope lifecycle (spec: Until group collapse) ----
    telemetryCoordinator.onGroupSelectionChanged(
      currentExpandedUiGroup = currentExpanded,
      newExpandedUiGroup = newExpandedUiGroup,
      uiGroup = uiGroup,
      groupChannelCodes = groupChannelCodes
    )

    val nextSelected =
      if (newExpandedUiGroup == null) {
        currentSelected
      } else if (currentSelected?.uiGroup == newExpandedUiGroup) {
        currentSelected
      } else {
        val lastSelectedCode = sessionRuntime.lastSelectedChannel(newExpandedUiGroup)
        val lastSelected =
          lastSelectedCode?.let { code -> channels.firstOrNull { it.channelCode == code } }
        lastSelected ?: (groups[newExpandedUiGroup]?.firstOrNull()
          .takeIf { groups[newExpandedUiGroup]?.size == 1 })
      }

    if (nextSelected != null) {
      sessionRuntime.rememberSelectedChannel(nextSelected.uiGroup, nextSelected.channelCode)
      applySelectedChannelTelemetry(nextSelected.channelCode)
    } else {
      telemetryCoordinator.clearSelectedChannel()
    }

    _state.update {
      it.copy(
        expandedUiGroup = newExpandedUiGroup,
        selectedChannel = nextSelected,
        paymentActionRedirect = null,
        presentToCustomerPaymentAction = null,
        paymentResponse = null,
        pollResponse = null,
        errorMessage = null
      )
    }
  }

  private fun selectChannelInternal(channelCode: String) {
    val selected = _state.value.channels.firstOrNull { it.channelCode == channelCode } ?: return
    sessionRuntime.rememberSelectedChannel(selected.uiGroup, selected.channelCode)
    applySelectedChannelTelemetry(channelCode)
    _state.update {
      it.copy(
        selectedChannel = selected,
        paymentActionRedirect = null,
        presentToCustomerPaymentAction = null,
        paymentResponse = null,
        pollResponse = null,
        errorMessage = null
      )
    }
  }

  private fun applySelectedChannelTelemetry(channelCode: String) {
    // If user is currently viewing an action screen (VA number, QR, barcode) and switches channel,
    // the previous action screen must be closed first so CHECKOUT_ACTION_CLOSE is emitted before
    // we push the new CHECKOUT_CHANNEL scope.
    if (telemetryCoordinator.hasOpenActionScope() || state.value.presentToCustomerPaymentAction != null) {
      markClosed()
    }
    telemetryCoordinator.onChannelSelected(channelCode)
  }

  private fun onUpdatePaymentDraft(paymentDraft: PaymentDraft) {
    val channelCode = paymentDraft.channelCode ?: return
    val nowKeys =
      paymentDraft.formValues.keys.filter { paymentDraft.formValues[it].isNullOrBlank().not() }
        .toSet()
    nowKeys.forEach(telemetryCoordinator::onFormFieldCompleted)
    _state.update {
      it.copy(
        paymentDrafts = it.paymentDrafts.toMutableMap().apply {
          put(channelCode, paymentDraft)
        }
      )
    }
  }

  private fun processPaymentInternal(
    channelCode: String,
    formValues: Map<String, String>,
    fields: List<ChannelFormField>,
    savePaymentMethod: Boolean,
    installmentPlans: List<InstallmentPlan>?
  ) = submitPaymentInternal(
    errorPrefix = "Payment",
    formValues = formValues,
    fields = fields
  ) { authKey, key, paySid ->
    val variantsForDisplay = _state.value.channelVariantsByDisplayCode[channelCode]
    val effectiveChannel =
      variantsForDisplay?.let { variants ->
        when {
          savePaymentMethod && variants.saveChannel != null -> variants.saveChannel
          !savePaymentMethod && variants.nonSaveChannel != null -> variants.nonSaveChannel
          else -> null
        }
      }
    val effectiveChannelCode = effectiveChannel?.channelCode ?: channelCode
    buildPaymentRequest(
      sessionAuthKey = authKey,
      publicKey = key,
      paymentSessionId = paySid,
      effectiveChannelCode = effectiveChannelCode,
      formValues = formValues,
      fields = fields,
      savePaymentMethod = savePaymentMethod,
      installmentPlans = installmentPlans,
      effectiveChannelForm = effectiveChannel?.form
    )
  }

  /**
   * Single helper to close the digital-wallet telemetry scope if it's still alive.
   * Encapsulates all 3 repeated lines (append Close + popScope + null ref) so every exit point
   * (resolution error, google pay failure, redirect, PTC, terminal success, empty channel props)
   * behaves identically. Cases that need a failure error_code (RESOLUTION / PAYMENT_FAILED) pass
   * it in; success paths leave it null.
   */
  private fun closeDigitalWalletTelemetry(success: Boolean, errorCode: String? = null) {
    telemetryCoordinator.closeDigitalWallet(success = success, errorCode = errorCode)
  }

  private fun submitGooglePayInternal(
    paymentDataJson: String,
    paymentMethodType: String?
  ) {
    val googlePay = _state.value.sessionResponse?.digitalWallets?.googlePay
    val channelResolution = resolveGooglePayChannelCodeOrError(googlePay, paymentMethodType)
    val channelCode = when (channelResolution) {
      is ResolvedGooglePayChannel.Ok -> {
        closeDigitalWalletTelemetry(success = true)
        channelResolution.code
      }

      is ResolvedGooglePayChannel.Err -> {
        val userMessage = channelResolution.userMessage
        globalErrorHandler.postError(errorMessage = UiText.DynamicString(userMessage))
        closeDigitalWalletTelemetry(success = false, errorCode = "GOOGLE_PAY_RESOLUTION")

        _state.update {
          it.copy(
            isLoading = false,
            errorMessage = userMessage
          )
        }
        return
      }
    }
    val channelProperties = buildGooglePayChannelProperties(paymentDataJson, channelCode)
    if (channelProperties.isEmpty()) {
      onChallengeCompletedInternal(true)
    } else {
      return submitPaymentInternal(
        isGooglePay = true,
        errorPrefix = "Google Pay Payment"
      ) { authKey, _key, _paySid ->
        PaymentRequest(
          sessionId = authKey,
          channelCode = channelCode,
          channelProperties = channelProperties
        )
      }
    }
  }

  private fun onGooglePayPaymentFailedInternal(
    code: String,
    title: String,
    message: String
  ) {
    val userMessage = if (title.isNotBlank() && message.isNotBlank()) {
      "$title. $message"
    } else if (title.isNotBlank()) {
      title
    } else {
      message.ifBlank { code }
    }
    XLogger.d("Google Pay failed with code=$code title=$title message=$message")
    closeDigitalWalletTelemetry(success = false, errorCode = code)

    globalErrorHandler.postError(errorMessage = UiText.DynamicString(userMessage))
    _state.update {
      it.copy(
        isLoading = false,
        errorMessage = userMessage
      )
    }
  }

  private inline fun submitPaymentInternal(
    isGooglePay: Boolean = false,
    errorPrefix: String,
    formValues: Map<String, String> = emptyMap(),
    fields: List<ChannelFormField> = emptyList(),
    crossinline buildRequest: suspend (
      sessionAuthKey: String,
      publicKey: String,
      paymentSessionId: String
    ) -> PaymentRequest
  ) {
    viewModelScope.launch {
      _state.update {
        it.copy(
          isLoading = if (isGooglePay) false else true,
          awaitingPaymentAction = if (isGooglePay) AwaitingPaymentAction.GooglePayProcessing else null,
          errorMessage = null,
          paymentResponse = null,
          paymentActionRedirect = null,
          presentToCustomerPaymentAction = null,
          pollResponse = null
        )
      }
      telemetryCoordinator.onAttemptStarted()

      val executionContext = sessionRuntime.executionContext()
      if (executionContext == null) {
        telemetryCoordinator.onAttemptError("MISSING_SESSION_CONTEXT")
        _state.update {
          it.copy(
            isLoading = false,
            awaitingPaymentAction = null,
            errorMessage = "$errorPrefix Error"
          )
        }
        return@launch
      }

      when (
        val result = submissionCoordinator.submit(
          sessionType = _state.value.sessionType,
          context = executionContext,
          formValues = formValues,
          fields = fields,
        ) { context ->
          buildRequest(
            context.sessionAuthKey,
            context.publicKey,
            context.paymentSessionId
          )
        }
      ) {
        is PaymentSubmissionResult.ValidationFailure -> {
          telemetryCoordinator.onAttemptValidationFailed(result.validationError)
          XLogger.d("submitPaymentInternal validation failed: ${result.userMessage}")
          globalErrorHandler.postError(errorMessage = UiText.DynamicString(result.userMessage))
          _state.update {
            it.copy(
              isLoading = false,
              awaitingPaymentAction = null,
              errorMessage = result.userMessage
            )
          }
        }

        is PaymentSubmissionResult.ApiFailure -> {
          telemetryCoordinator.onAttemptError(result.errorCode)
          _state.update {
            it.copy(
              isLoading = false,
              awaitingPaymentAction = null,
              errorMessage = result.errorMessage
            )
          }
        }

        is PaymentSubmissionResult.UnexpectedFailure -> {
          telemetryCoordinator.onAttemptError(result.errorMessage)
          globalErrorHandler.postError(errorMessage = UiText.DynamicString(result.errorMessage))
          _state.update {
            it.copy(
              isLoading = false,
              awaitingPaymentAction = null,
              errorMessage = result.errorMessage
            )
          }
        }

        is PaymentSubmissionResult.Success -> {
          val body = result.data.response
          sessionRuntime.recordSubmittedEntity(
            paymentRequestId = body.id,
            sessionTokenRequestId = body.sessionTokenRequestId
          )
          telemetryCoordinator.onPaymentEntityCreated(body)

          when (val presentation = result.data.presentationTarget) {
            is PaymentPresentationTarget.Redirect -> {
              telemetryCoordinator.onRedirectActionPresented()
              _state.update {
                it.copy(
                  isLoading = false,
                  awaitingPaymentAction = null,
                  paymentActionRedirect = presentation.action,
                  presentToCustomerPaymentAction = null,
                  paymentResponse = null
                )
              }
            }

            is PaymentPresentationTarget.PresentToCustomer -> {
              telemetryCoordinator.onPresentToCustomerActionPresented()
              _state.update {
                it.copy(
                  isLoading = false,
                  awaitingPaymentAction = null,
                  presentToCustomerPaymentAction = presentation.action,
                  paymentActionRedirect = null,
                  paymentResponse = null
                )
              }
            }

            PaymentPresentationTarget.AwaitingActionList -> {
              _state.update {
                it.copy(
                  isLoading = false,
                  awaitingPaymentAction = AwaitingPaymentAction.EmptyPaymentActions,
                  paymentActionRedirect = null,
                  presentToCustomerPaymentAction = null,
                  paymentResponse = body
                )
              }
            }

            null -> {
              if (body.status == PaymentRequestStatus.FAILED ||
                body.status == PaymentRequestStatus.CANCELED ||
                body.status == PaymentRequestStatus.EXPIRED
              ) {
                telemetryCoordinator.discardAttemptIfInFlight(
                  success = false,
                  failureCode = body.status.name
                )
              }
              _state.update {
                it.copy(
                  isLoading = false,
                  awaitingPaymentAction = null,
                  paymentResponse = body
                )
              }
            }
          }
          onChallengeCompletedInternal()
        }
      }
    }
  }

  private fun onChallengeCompletedInternal(forceStart: Boolean = false) {
    val authKey = sessionRuntime.sessionAuthKey ?: return
    val tokenReqId = sessionRuntime.lastSessionTokenRequestId

    if (forceStart) {
      cancelChallenge()
    }

    if (challengePollingJob?.isActive == true) return

    challengePollingJob =
      viewModelScope.launch {
        try {
          var delayMs = 3000L
          while (isActive) {
            when (val result = pollingCoordinator.poll(authKey, tokenReqId)) {
              is PaymentPollingResult.Success -> {
                val snapshot = result.snapshot
                if (snapshot.sessionPending) {
                  telemetryCoordinator.onSessionPending()
                }

                if (snapshot.hasTerminalEntity && telemetryCoordinator.hasOpenActionScope()) {
                  markClosed()
                }
                if (snapshot.sessionStatus.isTerminalSessionStatus() && snapshot.sessionStatus != null) {
                  telemetryCoordinator.onTerminalSessionStatus(snapshot.sessionStatus)
                }

                val sessionNotTerminal = snapshot.sessionStatus.isTerminalSessionStatus().not()
                if (sessionNotTerminal) {
                  telemetryCoordinator.onPaymentRequestStatusWhileSessionActive(snapshot.paymentRequestStatus)
                }
                _state.update {
                  it.copy(
                    pollResponse = snapshot.poll,
                  )
                }
              }

              is PaymentPollingResult.Failure -> {
                XLogger.d("Challenge Error: ${result.reason}")
              }
            }
            delay(delayMs)
            delayMs = minOf((delayMs * 1.2).toLong(), 10_000L)
          }
        } catch (e: TimeoutCancellationException) {
          globalErrorHandler.postError(
            errorMessage = UiText.DynamicString("Payment status polling timeout")
          )
          _state.update {
            it.copy(errorMessage = "Payment status polling timeout", isLoading = false)
          }
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          XLogger.e("Polling Error: ${e.message}")
        }
      }
  }

  private fun onSimulatePayment() {
    val authKey = sessionRuntime.sessionAuthKey ?: return
    viewModelScope.launch {
      val isPaySession = _state.value.sessionType.isPaySession()
      val shouldSimulate = isPaySession && !CoreSdkComponent.isProdLive()
      if (shouldSimulate) {
        val prId = sessionRuntime.lastPaymentRequestId
        val channelCode = _state.value.selectedChannel?.channelCode
        if (!prId.isNullOrBlank() && !channelCode.isNullOrBlank()) {
          runCatching {
            xenditRepository.simulatePaymentRequest(
              sessionId = authKey,
              paymentRequestId = prId,
              request = SimulatePaymentRequest(channelCode = channelCode)
            )
          }.onFailure { e ->
            XLogger.d("Simulate Payment failed: ${e.message}")
          }
        }
      }
    }
  }

  internal fun runFormWipeNonce() {
    _state.update { it.copy(formWipeNonce = it.formWipeNonce + 1) }
  }

  internal fun notifyCopyText(fieldName: String) {
    telemetryCoordinator.onCopyText(fieldName)
  }

  internal fun wipeAllSensitiveData() {
    markClosed()
    telemetryCoordinator.reset()

    cancelChallenge()
    challengePollingJob = null

    sessionRuntime.clear()
    _state.value = PaymentState()
  }

  internal fun trackDigitalWallet() {
    telemetryCoordinator.onDigitalWalletStarted()
  }

  internal fun trackDigitalWalletLoaded() {
    telemetryCoordinator.onDigitalWalletLoaded()
  }

  @VisibleForTesting
  internal fun injectSessionState(
    sessionResponse: SessionResponse,
    sessionType: BffSessionType,
    paymentSessionId: String,
    sessionAuthKey: String? = null,
    publicKey: String? = null,
    lastSessionTokenRequestId: String? = null
  ) {
    this.sessionRuntime.updatePaymentSessionId(paymentSessionId)
    if (sessionAuthKey != null && publicKey != null) {
      this.sessionRuntime.initialize(sessionAuthKey = sessionAuthKey, publicKey = publicKey)
      this.sessionRuntime.updatePaymentSessionId(paymentSessionId)
    }
    if (lastSessionTokenRequestId != null) {
      this.sessionRuntime.recordSubmittedEntity(
        paymentRequestId = sessionRuntime.lastPaymentRequestId,
        sessionTokenRequestId = lastSessionTokenRequestId
      )
    }
    _state.update {
      it.copy(
        sessionResponse = sessionResponse,
        sessionType = sessionType,
        paymentSessionId = paymentSessionId
      )
    }
  }

  fun showLoadingWithAction() {
    _state.update {
      it.copy(
        awaitingPaymentAction = AwaitingPaymentAction.Deeplink,
      )
    }
  }

  fun showLoading() {
    _state.update {
      it.copy(
        isLoading = true,
      )
    }
  }

  fun stopLoading() {
    _state.update {
      it.copy(
        isLoading = false,
      )
    }
  }

  fun markClosed() {
    // CHECKOUT_ACTION_CLOSE: fires for every action screen close (VA, QR, Barcode, OTC, Webview, Deeplink return).
    telemetryCoordinator.onActionClosed()
    _state.update {
      it.copy(
        presentToCustomerPaymentAction = null,
        paymentActionRedirect = null,
        isLoading = false,
        awaitingPaymentAction = null
      )
    }
    cancelChallenge()
  }

  private fun cancelChallenge() {
    challengePollingJob?.cancel()
  }
}

internal fun buildPaymentRequest(
  sessionAuthKey: String,
  publicKey: String,
  paymentSessionId: String,
  effectiveChannelCode: String,
  formValues: Map<String, String>,
  fields: List<ChannelFormField>,
  savePaymentMethod: Boolean,
  installmentPlans: List<InstallmentPlan>?,
  effectiveChannelForm: List<ChannelFormField>?
): PaymentRequest {
  val allowedKeysFromChannelForm =
    effectiveChannelForm
      ?.map { it.primaryChannelPropertyKey() }
      ?.filter { it.isNotBlank() }
      ?.toSet()
      .orEmpty()
  val shouldFilterByChannelForm = allowedKeysFromChannelForm.isNotEmpty()
  val filteredFields =
    if (shouldFilterByChannelForm) {
      fields.filter { it.primaryChannelPropertyKey() in allowedKeysFromChannelForm }
    } else {
      fields
    }
  val allowedValueKeys =
    if (shouldFilterByChannelForm) {
      mutableSetOf<String>().apply {
        addAll(allowedKeysFromChannelForm)
        filteredFields.forEach { field ->
          val primaryKey = field.primaryChannelPropertyKey()
          if (primaryKey.isBlank()) return@forEach
          if (field.type is FieldType.PhoneNumber) {
            add("${primaryKey}_country_code")
          }
        }
      }
    } else {
      null
    }
  val filteredFormValues =
    if (shouldFilterByChannelForm) {
      formValues.filterKeys { it in allowedValueKeys.orEmpty() }
    } else {
      formValues
    }

  val channelProperties =
    PaymentRequestMapper.mapFormValuesToChannelProperties(
      formValues = filteredFormValues,
      fields = filteredFields,
      publicKey = publicKey,
      sessionId = paymentSessionId,
      installmentPlans = installmentPlans
    )

  return PaymentRequest(
    sessionId = sessionAuthKey,
    channelCode = effectiveChannelCode,
    channelProperties = channelProperties,
    savePaymentMethod = if (savePaymentMethod) true else null
  )
}
