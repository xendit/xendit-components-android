package co.xendit.components.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.xendit.components.core.model.GlobalErrorHandler
import co.xendit.components.core.model.asApiError
import co.xendit.components.core.CoreSdkComponent
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.BffSessionAllowSavePaymentMethod
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.Country
import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.data.model.PaymentAction
import co.xendit.components.data.model.PaymentDraft
import co.xendit.components.data.model.PaymentRequest
import co.xendit.components.data.model.PaymentRequestStatus
import co.xendit.components.data.model.PaymentResponse
import co.xendit.components.data.model.PollResponse
import co.xendit.components.data.model.SessionResponse
import co.xendit.components.data.model.SimulatePaymentRequest
import co.xendit.components.data.model.primaryChannelPropertyKey
import co.xendit.components.data.network.repo.session.XenditRepository
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
  val channels: List<BffChannel> = emptyList(),
  val channelVariantsByDisplayCode: Map<String, ChannelVariantChannels> = emptyMap(),
  val expandedUiGroup: String? = null,
  val selectedChannel: BffChannel? = null,
  val lastSelectedChannelCodeByUiGroup: Map<String, String> = emptyMap(),
  val paymentSessionId: String? = null,
  val actionRedirectUrl: String? = null,
  val presentToCustomerPaymentAction: PaymentAction? = null,
  val iframeCapable: Boolean = true,
  val errorMessage: String? = null,
  val paymentResponse: PaymentResponse? = null,
  val sessionResponse: SessionResponse? = null,
  val pollResponse: PollResponse? = null,
  val sessionType: BffSessionType? = null,
  val allowSavePaymentMethod: BffSessionAllowSavePaymentMethod? = null,
  val paymentDrafts: Map<String, PaymentDraft> = emptyMap()
)

internal data class ChannelVariantChannels(
  val saveChannel: BffChannel? = null,
  val nonSaveChannel: BffChannel? = null
)

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
}

internal class PaymentViewModel(
  private val xenditRepository: XenditRepository,
  private val globalErrorHandler: GlobalErrorHandler
) : ViewModel() {

  init {
    // Warm up country data as early as possible
    viewModelScope.launch(Dispatchers.Default) { Country.warmUp() }
  }

  private val _state = MutableStateFlow(PaymentState())
  val state: StateFlow<PaymentState> = _state.asStateFlow()

  private var publicKey: String? = null
  private var sessionAuthKey: String? = null
  private var paymentSessionId: String? = null
  private var lastPaymentRequestId: String? = null
  private var lastSessionTokenRequestId: String? = null
  private var challengePollingJob: Job? = null

  fun dispatch(intent: ActionIntent) {
    when (intent) {
      is ActionIntent.Initialize -> {
        this.sessionAuthKey = intent.sessionAuthKey
        this.publicKey = intent.publicKey
        resetForNewSession()
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

      is ActionIntent.UpdatePaymentDraft -> onUpdatePaymentDraft(intent.paymentDraft)
      is ActionIntent.SimulatePayment -> onSimulatePayment()
      is ActionIntent.ChallengeCompleted -> onChallengeCompletedInternal(intent.forceStart)
      is ActionIntent.CloseWebPayment -> {
        _state.update {
          it.copy(
            actionRedirectUrl = null,
            presentToCustomerPaymentAction = null,
            paymentResponse = null,
            pollResponse = null
          )
        }
        markClosed()
      }
    }
  }

  private fun fetchSessionInternal(sessionAuthKey: String) {
    viewModelScope.launch {
      _state.update { it.copy(isLoading = true, errorMessage = null) }
      try {
        val response = xenditRepository.getSession(sessionAuthKey)
        if (response.isSuccessful) {
          val body = response.body()
          val session = body?.session
          val channels = body?.paymentChannels.orEmpty()
          val combined = combinePairedChannels(channels)
          this@PaymentViewModel.paymentSessionId =
            session?.paymentSessionId ?: session?.id
          val sessionType = body?.session?.sessionType
          val allowSavePaymentMethod = body?.session?.allowSavePaymentMethod

          if (combined.channels.isNotEmpty()) {
            _state.update {
              it.copy(
                isLoading = false,
                channels = combined.channels,
                channelVariantsByDisplayCode = combined.variantsByDisplayCode,
                paymentSessionId = this@PaymentViewModel.paymentSessionId,
                sessionResponse = body,
                errorMessage = null,
                sessionType = sessionType,
                allowSavePaymentMethod = allowSavePaymentMethod
              )
            }
          } else {
            _state.update {
              it.copy(isLoading = false, errorMessage = "No payment channels found")
            }
          }
        } else {
          val error = response.errorBody()?.asApiError()
          val errorMessage = error?.message ?: "Failed to fetch session"
          val errorCode = error?.errorCode
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

  private data class CombinedChannelsResult(
    val channels: List<BffChannel>,
    val variantsByDisplayCode: Map<String, ChannelVariantChannels>
  )

  private fun combinePairedChannels(channels: List<BffChannel>): CombinedChannelsResult {
    if (channels.isEmpty()) return CombinedChannelsResult(emptyList(), emptyMap())

    data class ChannelCombineKey(
      val uiGroup: String,
      val brandName: String,
      val pmType: String?,
      val allowPayWithoutSave: Boolean,
      val minAmount: Long?,
      val maxAmount: Long?,
      val requiresCustomerDetails: Boolean?,
      val brandColor: String
    )

    fun toKey(channel: BffChannel): ChannelCombineKey {
      return ChannelCombineKey(
        uiGroup = channel.uiGroup,
        brandName = channel.brandName,
        pmType = channel.pmType,
        allowPayWithoutSave = channel.allowPayWithoutSave,
        minAmount = channel.minAmount,
        maxAmount = channel.maxAmount,
        requiresCustomerDetails = channel.requiresCustomerDetails,
        brandColor = channel.brandColor
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

  private fun toggleUiGroupInternal(uiGroup: String) {
    val channels = _state.value.channels
    val groups = channels.groupBy { it.uiGroup }
    val newExpandedUiGroup = if (_state.value.expandedUiGroup == uiGroup) null else uiGroup
    val currentSelected = _state.value.selectedChannel
    val nextSelected =
      if (newExpandedUiGroup == null) {
        currentSelected
      } else if (currentSelected?.uiGroup == newExpandedUiGroup) {
        currentSelected
      } else {
        val lastSelectedCode = _state.value.lastSelectedChannelCodeByUiGroup[newExpandedUiGroup]
        val lastSelected = lastSelectedCode?.let { code -> channels.firstOrNull { it.channelCode == code } }
        lastSelected ?: (groups[newExpandedUiGroup]?.firstOrNull().takeIf { groups[newExpandedUiGroup]?.size == 1 })
      }

    _state.update {
      it.copy(
        expandedUiGroup = newExpandedUiGroup,
        selectedChannel = nextSelected,
        lastSelectedChannelCodeByUiGroup =
          it.lastSelectedChannelCodeByUiGroup.toMutableMap().apply {
            if (nextSelected != null) put(nextSelected.uiGroup, nextSelected.channelCode)
          },
        actionRedirectUrl = null,
        presentToCustomerPaymentAction = null,
        paymentResponse = null,
        pollResponse = null,
        errorMessage = null
      )
    }
  }

  private fun selectChannelInternal(channelCode: String) {
    val selected = _state.value.channels.firstOrNull { it.channelCode == channelCode } ?: return
    _state.update {
      it.copy(
        selectedChannel = selected,
        lastSelectedChannelCodeByUiGroup =
          it.lastSelectedChannelCodeByUiGroup.toMutableMap().apply {
            put(selected.uiGroup, selected.channelCode)
          },
        actionRedirectUrl = null,
        presentToCustomerPaymentAction = null,
        paymentResponse = null,
        pollResponse = null,
        errorMessage = null
      )
    }
  }

  private fun onUpdatePaymentDraft(paymentDraft: PaymentDraft) {
    val channelCode = paymentDraft.channelCode ?: return
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
  ) {
    viewModelScope.launch {
      _state.update {
        it.copy(
          isLoading = true,
          errorMessage = null,
          paymentResponse = null,
          actionRedirectUrl = null,
          presentToCustomerPaymentAction = null,
          pollResponse = null
        )
      }
      try {
        val key = publicKey ?: throw IllegalStateException("Public Key not set")
        val authKey = sessionAuthKey ?: throw IllegalStateException("Session ID not set")
        val paySid = paymentSessionId ?: throw IllegalStateException("Payment Session ID not set")
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
        val allowedKeys = effectiveChannel?.form.orEmpty().map { it.primaryChannelPropertyKey() }.toSet()
        val filteredFields =
          if (allowedKeys.isEmpty()) emptyList() else fields.filter { it.primaryChannelPropertyKey() in allowedKeys }
        val filteredFormValues =
          if (allowedKeys.isEmpty()) emptyMap() else formValues.filterKeys { it in allowedKeys }

        val channelProperties =
          PaymentRequestMapper.mapFormValuesToChannelProperties(
            formValues = filteredFormValues,
            fields = filteredFields,
            publicKey = key,
            sessionId = paySid, // Use paymentSessionId for encryption
            installmentPlans = installmentPlans
          )

        val request =
          PaymentRequest(
            sessionId = authKey, // Use sessionAuthKey for the request sessionId
            channelCode = effectiveChannelCode,
            channelProperties = channelProperties,
            savePaymentMethod = if (savePaymentMethod) true else null
          )

        val response =
          if (_state.value.sessionType == BffSessionType.SAVE) {
            xenditRepository.createPaymentToken(request = request)
          } else {
            xenditRepository.createPaymentRequest(request = request)
          }
        if (response.isSuccessful && response.body() != null) {
          val body = response.body()!!
          lastPaymentRequestId = body.id
          lastSessionTokenRequestId = body.sessionTokenRequestId
          val actions = body.paymentActions.orEmpty()
          val redirect =
            actions.firstOrNull {
              it.type == "REDIRECT_CUSTOMER" &&
                  (it.descriptor == "WEB_URL" ||
                      it.descriptor == "DEEPLINK_URL" ||
                      it.descriptor == "WEB_GOOGLE_PAYLINK")
            }
          if (body.status == PaymentRequestStatus.REQUIRES_ACTION && redirect?.value != null) {
            _state.update {
              it.copy(
                isLoading = false,
                actionRedirectUrl = redirect.value,
                iframeCapable = redirect.iframeCapable ?: true
              )
            }
          } else if (body.status == PaymentRequestStatus.REQUIRES_ACTION) {
            val presentToCustomer =
              actions.firstOrNull { it.type == "PRESENT_TO_CUSTOMER" && it.value != null }
            if (presentToCustomer != null) {
              _state.update {
                it.copy(
                  isLoading = false,
                  presentToCustomerPaymentAction = presentToCustomer,
                  actionRedirectUrl = null
                )
              }
            } else {
              _state.update { it.copy(isLoading = false, paymentResponse = body) }
            }
          } else {
            _state.update { it.copy(isLoading = false, paymentResponse = body) }
          }
          onChallengeCompletedInternal() // start pooling here
        } else {
          val error = response.errorBody()?.asApiError()
          val errorMessage = error?.errorContent?.message1 ?: error?.message ?: "Payment Failed"
          _state.update { it.copy(isLoading = false, errorMessage = errorMessage) }
        }
      } catch (e: Exception) {
        val errorMessage = e.message ?: "Payment Error"
        globalErrorHandler.postError(errorMessage = UiText.DynamicString(errorMessage))
        _state.update { it.copy(isLoading = false, errorMessage = errorMessage) }
      }
    }
  }

  private fun onChallengeCompletedInternal(forceStart: Boolean = false) {
    val authKey = sessionAuthKey ?: return
    val tokenReqId = lastSessionTokenRequestId

    if (forceStart) { cancelChallenge() }

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
      val isPaySession = _state.value.sessionType != BffSessionType.SAVE
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

  fun resetForNewSession() {
    markClosed()
    paymentSessionId = null
    lastPaymentRequestId = null
    lastSessionTokenRequestId = null
    _state.value = PaymentState()
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
    _state.update {
      it.copy(presentToCustomerPaymentAction = null, isLoading = false)
    }
    cancelChallenge()
  }

  private fun cancelChallenge() {
    challengePollingJob?.cancel()
    challengePollingJob = null
  }

}
