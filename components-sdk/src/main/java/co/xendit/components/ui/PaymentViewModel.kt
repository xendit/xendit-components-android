package co.xendit.components.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.xendit.components.core.model.GlobalErrorHandler
import co.xendit.components.core.model.asApiError
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.BffSession
import co.xendit.components.data.model.BffSessionAllowSavePaymentMethod
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.Country
import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.data.model.PaymentAction
import co.xendit.components.data.model.PaymentDraft
import co.xendit.components.data.model.PaymentRequest
import co.xendit.components.data.model.PaymentResponse
import co.xendit.components.data.model.PaymentRequestStatus
import co.xendit.components.data.model.PollResponse
import co.xendit.components.data.network.repo.session.XenditRepository
import co.xendit.components.ui.components.molecule.UiText
import co.xendit.components.util.PaymentRequestMapper
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
  val expandedUiGroup: String? = null,
  val selectedChannel: BffChannel? = null,
  val paymentSessionId: String? = null,
  val actionRedirectUrl: String? = null,
  val presentToCustomerPaymentAction: PaymentAction? = null,
  val iframeCapable: Boolean = true,
  val errorMessage: String? = null,
  val paymentResponse: PaymentResponse? = null,
  val sessionResponse: BffSession? = null,
  val pollResponse: PollResponse? = null,
  val sessionType: BffSessionType? = null,
  val allowSavePaymentMethod: BffSessionAllowSavePaymentMethod? = null,
  val paymentDraft: PaymentDraft = PaymentDraft()
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
   * This triggers a status check to verify the final result.
   */
  data object ChallengeCompleted : ActionIntent()

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
      is ActionIntent.ChallengeCompleted -> onChallengeCompletedInternal()
      is ActionIntent.CloseWebPayment -> {
        _state.update { it.copy(actionRedirectUrl = null) }
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
          this@PaymentViewModel.paymentSessionId =
            session?.paymentSessionId ?: session?.id
          val sessionType = body?.session?.sessionType
          val allowSavePaymentMethod = body?.session?.allowSavePaymentMethod

          if (channels.isNotEmpty()) {
            _state.update {
              it.copy(
                isLoading = false,
                channels = channels,
                paymentSessionId = this@PaymentViewModel.paymentSessionId,
                sessionResponse = session,
                errorMessage = null,
                sessionType = sessionType,
                allowSavePaymentMethod = allowSavePaymentMethod
              )
            }
          } else {
            _state.update { it.copy(isLoading = false, errorMessage = "No payment channels found") }
          }
        } else {
          _state.update {
            it.copy(isLoading = false, errorMessage = "Error API +${response.toString()}")
          }
        }
      } catch (e: Exception) {
        _state.update {
          it.copy(isLoading = false, errorMessage = e.message ?: "Failed to fetch session")
        }
      }
    }
  }

  private fun toggleUiGroupInternal(uiGroup: String) {
    val channels = _state.value.channels
    val newExpanded = if (_state.value.expandedUiGroup == uiGroup) null else uiGroup
    val existingSelected = _state.value.selectedChannel
    val nextSelected =
      if (newExpanded == null) {
        existingSelected
      } else if (existingSelected?.uiGroup == newExpanded) {
        existingSelected
      } else {
        channels.firstOrNull { it.uiGroup == newExpanded }
      }

    _state.update {
      it.copy(
        expandedUiGroup = newExpanded,
        selectedChannel = nextSelected,
        actionRedirectUrl = null,
        presentToCustomerPaymentAction = null,
        paymentResponse = null,
        pollResponse = null
      )
    }
  }

  private fun selectChannelInternal(channelCode: String) {
    val selected = _state.value.channels.firstOrNull { it.channelCode == channelCode } ?: return
    _state.update {
      it.copy(
        selectedChannel = selected,
        actionRedirectUrl = null,
        presentToCustomerPaymentAction = null,
        paymentResponse = null,
        pollResponse = null
      )
    }
  }

  private fun onUpdatePaymentDraft(paymentDraft: PaymentDraft) {
    _state.update {
      it.copy(
        paymentDraft = paymentDraft,
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

        val channelProperties =
          PaymentRequestMapper.mapFormValuesToChannelProperties(
            formValues = formValues,
            fields = fields,
            publicKey = key,
            sessionId = paySid, // Use paymentSessionId for encryption
            installmentPlans = installmentPlans
          )

        val request =
          PaymentRequest(
            sessionId = authKey, // Use sessionAuthKey for the request sessionId
            channelCode = channelCode,
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
          val errorMessage = error?.errorContent?.message1 ?: "Payment Failed"
          globalErrorHandler.postError(errorMessage = UiText.DynamicString(errorMessage))
          _state.update { it.copy(isLoading = false, errorMessage = errorMessage) }
        }
      } catch (e: Exception) {
        val errorMessage = e.message ?: "Payment Error"
        globalErrorHandler.postError(errorMessage = UiText.DynamicString(errorMessage))
        _state.update { it.copy(isLoading = false, errorMessage = errorMessage) }
      }
    }
  }

  private fun onChallengeCompletedInternal() {
    val authKey = sessionAuthKey ?: return
    val tokenReqId = lastSessionTokenRequestId
    if (challengePollingJob?.isActive == true) return
    challengePollingJob =
      viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
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
                    isLoading = false,
                  )
                }
                Log.d(
                  "Challenge",
                  "successResponse: ${poll.paymentRequest ?: poll.paymentToken}"
                )
              }
            } else {
              // On unauthorized or errors, just backoff and retry within timeout
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
          Log.d("Polling", "Error: ${e.message}")
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

  fun markClosed() {
    challengePollingJob?.cancel()
    challengePollingJob = null
  }
}
