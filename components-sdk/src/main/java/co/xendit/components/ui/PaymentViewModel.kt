package co.xendit.components.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.xendit.components.XenditComponentsPaymentType
import co.xendit.components.XenditComponentsPaymentType.Companion.BLACKLISTED_CHANNEL
import co.xendit.components.core.CoreSdkComponent
import co.xendit.components.core.model.GlobalErrorHandler
import co.xendit.components.core.model.asApiError
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
import co.xendit.components.data.model.PollResponse
import co.xendit.components.data.model.SessionResponse
import co.xendit.components.data.model.SimulatePaymentRequest
import co.xendit.components.data.model.isPaySession
import co.xendit.components.data.model.primaryChannelPropertyKey
import co.xendit.components.data.model.usesPaymentTokenSubmission
import co.xendit.components.data.network.repo.session.XenditRepository
import co.xendit.components.telemetry.SessionTelemetry
import co.xendit.components.telemetry.SessionTelemetryScope
import co.xendit.components.telemetry.TelemetryEvents
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
}

internal data class ChannelVariantChannels(
  val saveChannel: BffChannel? = null,
  val nonSaveChannel: BffChannel? = null
)

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
  private val telemetry: SessionTelemetry
) : ViewModel() {

  init {
    // Warm up country data as early as possible
    viewModelScope.launch(Dispatchers.Default) { Country.warmUp() }
  }

  override fun onCleared() {
    super.onCleared()
    runCatching {
      if (!telemetry.expectingRedirectAway) {
        telemetry.append(TelemetryEvents.Abandon(false))
      }
    }
    telemetry.flush()
    popAllTelemetryScopes()
    wipeAllSensitiveData()
  }

  private val _state = MutableStateFlow(PaymentState())
  val state: StateFlow<PaymentState> = _state.asStateFlow()

  private var publicKey: String? = null
  private var sessionAuthKey: String? = null
  private var paymentSessionId: String? = null
  private var lastPaymentRequestId: String? = null
  private var lastSessionTokenRequestId: String? = null
  private var challengePollingJob: Job? = null
  private val lastSelectedChannelCodeByUiGroup: MutableMap<String, String> = mutableMapOf()

  // Telemetry scope handles — mirrors Web this.currentChannelTelemetryScope / this.telemetryScope.
  private var currentGroupTelemetryScope: SessionTelemetryScope? = null
  private var currentChannelTelemetryScope: SessionTelemetryScope? = null
  private var loadedTelemetryScope: SessionTelemetryScope? = null
  private var submissionTelemetryScope: SessionTelemetryScope? = null
  private var actionTelemetryScope: SessionTelemetryScope? = null
  private var digitalWalletScope: SessionTelemetryScope? = null
  private val formInputSentKeys: MutableSet<String> = mutableSetOf()
  private val attemptPushedScopes: MutableList<SessionTelemetryScope> = mutableListOf()

  fun dispatch(intent: ActionIntent) {
    when (intent) {
      is ActionIntent.Initialize -> {
        wipeAllSensitiveData()
        this.sessionAuthKey = intent.sessionAuthKey
        this.publicKey = intent.publicKey
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
        // the single source of truth for any action-screen closure (VA, QR, Barcode, Webview, Deeplink).
        telemetry.expectingRedirectAway = false
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
      telemetry.append(TelemetryEvents.Pending(true))
      try {
        val response = xenditRepository.getSession(sessionAuthKey)
        if (response.isSuccessful) {
          val body = response.body()
          val session = body?.session
          val channels = body?.paymentChannels.orEmpty().filter {
            !BLACKLISTED_CHANNEL.contains(it.channelCode)
          }
          val variantsByDisplayCode = combinePairedChannels(channels).variantsByDisplayCode
          this@PaymentViewModel.paymentSessionId =
            session?.paymentSessionId ?: session?.id
          val sessionType = body?.session?.sessionType
          val allowSavePaymentMethod = body?.session?.allowSavePaymentMethod

          // ===== Telemetry: bind payment_session_id now that FetchSession returned it.
          telemetry.bindSession(host = null, sessionId = this@PaymentViewModel.paymentSessionId, authId = null)
          loadedTelemetryScope = telemetry.appendAndPushScope(TelemetryEvents.Loaded(true))
          // ===== end telemetry

          if (channels.isNotEmpty()) {
            _state.update {
              it.copy(
                isLoading = false,
                channels = channels,
                channelVariantsByDisplayCode = variantsByDisplayCode,
                paymentSessionId = this@PaymentViewModel.paymentSessionId,
                sessionResponse = body,
                errorMessage = null,
                sessionType = sessionType,
                allowSavePaymentMethod = allowSavePaymentMethod
              )
            }
          } else {
            _state.update {
              it.copy(isLoading = false, sessionResponse = body)
            }
          }
        } else {
          val error = response.errorBody()?.asApiError()
          val errorMessage = error?.message ?: "Failed to fetch session"
          val errorCode = error?.errorCode
          telemetry.append(TelemetryEvents.Loaded(false))
          _state.update {
            it.copy(
              isLoading = false,
              errorMessage = if (errorCode == "NETWORK_ERROR") null else errorMessage
            )
          }
        }
      } catch (e: Exception) {
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

    // ---- TELEMETRY: ChannelGroup scope lifecycle (spec: Until group collapse) ----
    when {
      // Case 1: COLLAPSING the same group → pop scope.
      newExpandedUiGroup == null && currentGroupTelemetryScope != null -> {
        telemetry.popScope(currentGroupTelemetryScope)
        currentGroupTelemetryScope = null
      }
      // Case 2: SWITCHING groups or EXPANDING first group → pop old scope (if any),
      //         then push scope for the newly expanded group. Also emit ChannelGroup event.
      newExpandedUiGroup != null -> {
        if (currentGroupTelemetryScope != null && currentExpanded != null) {
          telemetry.popScope(currentGroupTelemetryScope)
        }
        currentGroupTelemetryScope =
          telemetry.appendAndPushScope(TelemetryEvents.ChannelGroup(true, uiGroup))
      }
    }

    val nextSelected =
      if (newExpandedUiGroup == null) {
        currentSelected
      } else if (currentSelected?.uiGroup == newExpandedUiGroup) {
        currentSelected
      } else {
        val lastSelectedCode = lastSelectedChannelCodeByUiGroup[newExpandedUiGroup]
        val lastSelected =
          lastSelectedCode?.let { code -> channels.firstOrNull { it.channelCode == code } }
        lastSelected ?: (groups[newExpandedUiGroup]?.firstOrNull()
          .takeIf { groups[newExpandedUiGroup]?.size == 1 })
      }

    if (nextSelected != null) {
      lastSelectedChannelCodeByUiGroup[nextSelected.uiGroup] = nextSelected.channelCode
      applySelectedChannelTelemetry(nextSelected.channelCode)
    } else {
      telemetry.popScope(currentChannelTelemetryScope)
      currentChannelTelemetryScope = null
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
    lastSelectedChannelCodeByUiGroup[selected.uiGroup] = selected.channelCode
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
    if (actionTelemetryScope != null || state.value.presentToCustomerPaymentAction != null) {
      markClosed()
    }
    telemetry.popScope(currentChannelTelemetryScope)
    currentChannelTelemetryScope = telemetry.appendAndPushScope(TelemetryEvents.Channel(true, channelCode))
  }

  private fun onUpdatePaymentDraft(paymentDraft: PaymentDraft) {
    val channelCode = paymentDraft.channelCode ?: return
    val nowKeys = paymentDraft.formValues.keys.filter { paymentDraft.formValues[it].isNullOrBlank().not() }.toSet()
    val newKeys = nowKeys subtract formInputSentKeys
    newKeys.forEach { key ->
      formInputSentKeys.add(key)
      telemetry.append(TelemetryEvents.ChannelFormInput(true, key))
    }
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
    digitalWalletScope?.let { scope ->
      runCatching {
        telemetry.append(TelemetryEvents.DigitalWalletClose(success = success, errorCode = errorCode))
        telemetry.popScope(scope)
      }
    }
    digitalWalletScope = null
  }

  private fun submitGooglePayInternal(
    paymentDataJson: String,
    paymentMethodType: String?
  ) {
    val googlePay = _state.value.sessionResponse?.digitalWallets?.googlePay
    val channelResolution = resolveGooglePayChannelCodeOrError(googlePay, paymentMethodType)
    val channelCode = when (channelResolution) {
      is ResolvedGooglePayChannel.Ok -> channelResolution.code
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
      closeDigitalWalletTelemetry(success = true)
      onChallengeCompletedInternal(true)
    } else {
      return submitPaymentInternal(
        errorPrefix = "Google Pay Payment",
        formValues = emptyMap(),
        fields = emptyList()
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
      // ---- Spec "CHECKOUT_ATTEMPT_BEGIN - Fail If: Validation error" -----------------------
      // Before anything (loading state update / attempt scope push), scan all ChannelFormFields
      // where required=true. If any required key is blank, emit AttemptBegin(false) with
      // metadata.validation_error = "<KEY>_REQUIRED", show the user error in UI, and return
      // WITHOUT calling the API.
      val requiredValidationError: String? = run validation@{
        fields
          .filter { it.required }
          .forEach { field ->
            val key = field.primaryChannelPropertyKey()
            val value = formValues[key]?.takeIf { it.isNotBlank() }
            if (value == null) {
              return@validation "${key.uppercase()}_REQUIRED"
            }
          }
        null
      }
      if (requiredValidationError != null) {
        telemetry.append(
          TelemetryEvents.AttemptBegin(
            success = false,
            validationError = requiredValidationError
          )
        )
        val userMessage = "Missing required field: $requiredValidationError"
        XLogger.d("submitPaymentInternal validation failed: $userMessage")
        globalErrorHandler.postError(errorMessage = UiText.DynamicString(userMessage))
        _state.update {
          it.copy(
            isLoading = false,
            awaitingPaymentAction = null,
            errorMessage = userMessage
          )
        }
        return@launch
      }

      _state.update {
        it.copy(
          isLoading = true,
          awaitingPaymentAction = null,
          errorMessage = null,
          paymentResponse = null,
          paymentActionRedirect = null,
          presentToCustomerPaymentAction = null,
          pollResponse = null
        )
      }
      // Pop any previous submission scope if user retries.
      submissionTelemetryScope?.let { telemetry.popScope(it) }
      submissionTelemetryScope = null
      attemptPushedScopes.reversed().forEach { telemetry.popScope(it) }
      attemptPushedScopes.clear()
      // AttemptBegin (validation passed → success=true)
      submissionTelemetryScope = telemetry.appendAndPushScope(
        TelemetryEvents.AttemptBegin(
          success = true,
          validationError = null
        )
      )

      try {
        val key = publicKey ?: throw IllegalStateException("Public Key not set")
        val authKey = sessionAuthKey ?: throw IllegalStateException("Session ID not set")
        val paySid = paymentSessionId ?: throw IllegalStateException("Payment Session ID not set")

        val request = buildRequest(authKey, key, paySid)

        val response =
          if (_state.value.sessionType.usesPaymentTokenSubmission()) {
            xenditRepository.createPaymentToken(request = request)
          } else {
            xenditRepository.createPaymentRequest(request = request)
          }
        if (response.isSuccessful && response.body() != null) {
          val body = response.body()!!
          lastPaymentRequestId = body.id
          lastSessionTokenRequestId = body.sessionTokenRequestId
          // Attempt scope for PR/PT id (1:1 Web order)
          val attemptScope = when {
            body.id != null -> telemetry.appendAndPushScope(TelemetryEvents.Attempt_PR(true, body.id!!))
            body.sessionTokenRequestId != null -> telemetry.appendAndPushScope(
              TelemetryEvents.Attempt_PT(true, body.sessionTokenRequestId!!)
            )
            else -> null
          }
          attemptScope?.let { attemptPushedScopes.add(it) }

          val actions = body.paymentActions.orEmpty()
          val redirect =
            actions.firstOrNull {
              it.type == "REDIRECT_CUSTOMER" &&
                  (it.descriptor == PaymentActionDescriptor.WEB_URL ||
                      it.descriptor == PaymentActionDescriptor.DEEPLINK_URL ||
                      it.descriptor == PaymentActionDescriptor.WEB_GOOGLE_PAYLINK)
            }
          if (body.status == PaymentRequestStatus.REQUIRES_ACTION) {
            val presentToCustomer =
              actions.firstOrNull {
                it.type == "PRESENT_TO_CUSTOMER" &&
                    it.value != null &&
                    (it.descriptor == PaymentActionDescriptor.VIRTUAL_ACCOUNT_NUMBER ||
                        it.descriptor == PaymentActionDescriptor.QR_STRING)
              } ?: actions.firstOrNull { it.type == "PRESENT_TO_CUSTOMER" && it.value != null }
            when {
              redirect?.value != null -> {
                // Action begin ; Digital wallet close success if Google Pay.
                actionTelemetryScope = telemetry.appendAndPushScope(TelemetryEvents.ActionBegin(true))
                closeDigitalWalletTelemetry(success = true)
                telemetry.expectingRedirectAway = true

                _state.update {
                  it.copy(
                    isLoading = false,
                    awaitingPaymentAction = null,
                    paymentActionRedirect = redirect,
                    presentToCustomerPaymentAction = null,
                    paymentResponse = null
                  )
                }
              }

              presentToCustomer != null -> {
                actionTelemetryScope = telemetry.appendAndPushScope(TelemetryEvents.ActionBegin(true))
                closeDigitalWalletTelemetry(success = true)

                _state.update {
                  it.copy(
                    isLoading = false,
                    awaitingPaymentAction = null,
                    presentToCustomerPaymentAction = presentToCustomer,
                    paymentActionRedirect = null,
                    paymentResponse = null
                  )
                }
              }

              actions.isEmpty() -> {
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

              else -> {
                emitTerminalEndIfNeeded(body.status?.name)
                _state.update {
                  it.copy(
                    isLoading = false,
                    awaitingPaymentAction = null,
                    paymentResponse = body
                  )
                }
              }
            }
          } else {
            _state.update {
              it.copy(
                isLoading = false,
                awaitingPaymentAction = null,
                paymentResponse = body
              )
            }
          }
          onChallengeCompletedInternal()
        } else {
          val error = response.errorBody()?.asApiError()
          val errorCode = error?.errorCode ?: "-1"
          telemetry.append(TelemetryEvents.Attempt_Error(false, errorCode = errorCode))
          val errorMessage = error?.errorContent?.message1 ?: error?.message ?: "$errorPrefix Failed"
          _state.update {
            it.copy(
              isLoading = false,
              awaitingPaymentAction = null,
              errorMessage = errorMessage
            )
          }
        }
      } catch (e: Exception) {
        telemetry.append(TelemetryEvents.Attempt_Error(false, errorCode = e.message))
        val errorMessage = e.message ?: "$errorPrefix Error"
        globalErrorHandler.postError(errorMessage = UiText.DynamicString(errorMessage))
        _state.update {
          it.copy(
            isLoading = false,
            awaitingPaymentAction = null,
            errorMessage = errorMessage
          )
        }
      }
    }
  }

  private fun onChallengeCompletedInternal(forceStart: Boolean = false) {
    val authKey = sessionAuthKey ?: return
    val tokenReqId = lastSessionTokenRequestId

    if (forceStart) {
      cancelChallenge()
    }

    if (challengePollingJob?.isActive == true) return

    challengePollingJob =
      viewModelScope.launch {
        try {
          var delayMs = 3000L
          while (isActive) {
            val res = xenditRepository.pollSession(authKey, tokenReqId)
            if (res.isSuccessful && res.body() != null) {
              val poll = res.body()
              if (poll != null) {
                val pollStatus = poll.session?.status.toString()
                val prStatus = poll.paymentRequest?.status.toString()
                if (pollStatus.isTerminalStatus()) {
                  emitTerminalEndIfNeeded(pollStatus)
                }
                if (prStatus.isTerminalStatus()) {
                  emitTerminalEndIfNeeded(prStatus)
                }
                _state.update {
                  it.copy(
                    pollResponse = poll,
                  )
                }
              }
            } else {
              // On unauthorized or errors, just backoff and retry within timeout
              XLogger.d("Challenge Error: ${res}")
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
    val authKey = sessionAuthKey ?: return
    viewModelScope.launch {
      val isPaySession = _state.value.sessionType.isPaySession()
      val shouldSimulate = isPaySession && !CoreSdkComponent.isProdLive()
      if (shouldSimulate) {
        val prId = lastPaymentRequestId
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
    telemetry.append(TelemetryEvents.ActionCopyText(true, fieldName))
  }

  internal fun wipeAllSensitiveData() {
    // If wipe was called while a submit attempt was in-flight (e.g. cancel/close before result),
    // emit AttemptDiscard to mirror Web discard-attempt behavior.
    if (submissionTelemetryScope != null) {
      telemetry.append(TelemetryEvents.AttemptDiscard(false, failureCode = "WIPE_SENSITIVE_DATA"))
    }
    // Close any active action screen FIRST (VA/QR/Barcode/Web) so CHECKOUT_ACTION_CLOSE is emitted
    // while actionTelemetryScope is still alive — popAllTelemetryScopes() below will null it.
    markClosed()
    popAllTelemetryScopes()

    cancelChallenge()
    challengePollingJob = null

    sessionAuthKey = null
    publicKey = null
    paymentSessionId = null
    lastPaymentRequestId = null
    lastSessionTokenRequestId = null

    lastSelectedChannelCodeByUiGroup.clear()
    formInputSentKeys.clear()
    _state.value = PaymentState()
  }

  internal fun trackDigitalWallet() {
    digitalWalletScope = telemetry.appendAndPushScope(TelemetryEvents.DigitalWalletBegin(true, "GOOGLE_PAY"))
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
    this.paymentSessionId = paymentSessionId
    if (sessionAuthKey != null) this.sessionAuthKey = sessionAuthKey
    if (publicKey != null) this.publicKey = publicKey
    if (lastSessionTokenRequestId != null) this.lastSessionTokenRequestId = lastSessionTokenRequestId
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
    // Scope doc: "Until action screen closed". Emits once, then pops the scope pushed by ActionBegin.
    actionTelemetryScope?.let { scope ->
      runCatching {
        telemetry.append(TelemetryEvents.ActionClose(true))
        telemetry.popScope(scope)
      }
      actionTelemetryScope = null
    }
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

  private fun popAllTelemetryScopes() {
    listOfNotNull(
      currentGroupTelemetryScope,
      currentChannelTelemetryScope,
      loadedTelemetryScope,
      submissionTelemetryScope,
      actionTelemetryScope,
      digitalWalletScope,
    ).plus(attemptPushedScopes.reversed()).forEach { telemetry.popScope(it) }
    attemptPushedScopes.clear()
    currentGroupTelemetryScope = null
    currentChannelTelemetryScope = null
    loadedTelemetryScope = null
    submissionTelemetryScope = null
    actionTelemetryScope = null
    digitalWalletScope = null
  }

  private fun emitTerminalEndIfNeeded(statusValue: String?) {
    val status = (statusValue ?: return).uppercase()
    val success = when (status) {
      "SUCCEEDED", "CAPTURED", "SUCCESS", "PAID" -> true
      "FAILED", "DECLINED", "REJECTED", "CANCELLED", "CANCELED", "EXPIRED" -> false
      else -> return
    }
    telemetry.append(TelemetryEvents.End(success = success, status = status))
    if (success) {
      closeDigitalWalletTelemetry(success = true)
    }
    telemetry.flush()
  }

  private fun String?.isTerminalStatus(): Boolean {
    val v = (this ?: return false).uppercase()
    return v in setOf(
      "SUCCEEDED", "CAPTURED", "SUCCESS", "PAID",
      "FAILED", "DECLINED", "REJECTED", "CANCELLED", "CANCELED", "EXPIRED"
    )
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
