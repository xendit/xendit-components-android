package co.xendit.components.ui

import co.xendit.components.data.model.PaymentRequestStatus
import co.xendit.components.data.model.PaymentResponse
import co.xendit.components.data.model.PaymentSessionStatus
import co.xendit.components.telemetry.SessionTelemetry
import co.xendit.components.telemetry.SessionTelemetryScope
import co.xendit.components.telemetry.TelemetryEvents

internal class PaymentTelemetryCoordinator(
  private val telemetry: SessionTelemetry
) {
  private var endTelemetryEmitted: Boolean = false
  private var sessionPendingTelemetryEmitted: Boolean = false
  private var loadedOrResumeTelemetryPushed: Boolean = false
  private var digitalWalletLoadedTelemetryEmitted: Boolean = false

  private var currentGroupTelemetryScope: SessionTelemetryScope? = null
  private var currentChannelTelemetryScope: SessionTelemetryScope? = null
  private var loadedTelemetryScope: SessionTelemetryScope? = null
  private var submissionTelemetryScope: SessionTelemetryScope? = null
  private var actionTelemetryScope: SessionTelemetryScope? = null
  private var digitalWalletScope: SessionTelemetryScope? = null
  private val formInputSentKeys: MutableSet<String> = mutableSetOf()
  private val attemptPushedScopes: MutableList<SessionTelemetryScope> = mutableListOf()

  fun onCleared() {
    runCatching {
      if (!telemetry.expectingRedirectAway) {
        telemetry.append(TelemetryEvents.Abandon(false))
      }
    }
    telemetry.flush()
    popAllTelemetryScopes()
  }

  fun resetRedirectExpectation() {
    telemetry.expectingRedirectAway = false
  }

  fun onSessionLoadedSuccess(
    selectableChannelCodes: List<String>,
    paymentSessionId: String?,
    sessionStatus: PaymentSessionStatus?
  ) {
    if (!loadedOrResumeTelemetryPushed) {
      loadedTelemetryScope = telemetry.appendAndPushScope(
        TelemetryEvents.Loaded(true, selectableChannelCodes)
      )
      loadedOrResumeTelemetryPushed = true
    }

    telemetry.bindSession(
      host = null,
      sessionId = paymentSessionId,
      authId = null
    )

    if (sessionStatus.isTerminalSessionStatus() && sessionStatus != null) {
      when (sessionStatus) {
        PaymentSessionStatus.CANCELED,
        PaymentSessionStatus.EXPIRED -> discardAttemptIfInFlight(
          success = false,
          failureCode = sessionStatus.name
        )

        else -> Unit
      }
      emitTerminalEndIfNeeded(sessionStatus)
    }

    if (sessionStatus == PaymentSessionStatus.PENDING) {
      emitSessionPendingOnce()
    }
  }

  fun onSessionLoadedFailure() {
    if (!loadedOrResumeTelemetryPushed) {
      telemetry.append(TelemetryEvents.Loaded(false))
      loadedOrResumeTelemetryPushed = true
    }
  }

  fun onGroupSelectionChanged(
    currentExpandedUiGroup: String?,
    newExpandedUiGroup: String?,
    uiGroup: String,
    groupChannelCodes: List<String>?
  ) {
    when {
      newExpandedUiGroup == null && currentGroupTelemetryScope != null -> {
        telemetry.popScope(currentGroupTelemetryScope)
        currentGroupTelemetryScope = null
      }

      newExpandedUiGroup != null -> {
        if (currentGroupTelemetryScope != null && currentExpandedUiGroup != null) {
          telemetry.popScope(currentGroupTelemetryScope)
        }
        currentGroupTelemetryScope =
          telemetry.appendAndPushScope(
            TelemetryEvents.ChannelGroup(true, uiGroup, groupChannelCodes)
          )
      }
    }
  }

  fun hasOpenActionScope(): Boolean = actionTelemetryScope != null

  fun onChannelSelected(channelCode: String) {
    telemetry.popScope(currentChannelTelemetryScope)
    currentChannelTelemetryScope =
      telemetry.appendAndPushScope(TelemetryEvents.Channel(true, channelCode))
  }

  fun clearSelectedChannel() {
    telemetry.popScope(currentChannelTelemetryScope)
    currentChannelTelemetryScope = null
  }

  fun onFormFieldCompleted(fieldKey: String) {
    if (formInputSentKeys.add(fieldKey)) {
      telemetry.append(TelemetryEvents.ChannelFormInput(true, fieldKey))
    }
  }

  fun onDigitalWalletStarted() {
    digitalWalletScope =
      telemetry.appendAndPushScope(TelemetryEvents.DigitalWalletBegin(true, "GOOGLE_PAY"))
  }

  fun onDigitalWalletLoaded() {
    if (digitalWalletLoadedTelemetryEmitted) return
    digitalWalletLoadedTelemetryEmitted = true
    telemetry.append(TelemetryEvents.DigitalWalletLoaded(true, "GOOGLE_PAY"))
  }

  fun closeDigitalWallet(success: Boolean, errorCode: String? = null) {
    digitalWalletScope?.let { scope ->
      runCatching {
        telemetry.append(
          TelemetryEvents.DigitalWalletClose(
            success = success,
            errorCode = errorCode
          )
        )
        telemetry.popScope(scope)
      }
    }
    digitalWalletScope = null
  }

  fun onAttemptValidationFailed(validationError: String) {
    telemetry.append(
      TelemetryEvents.AttemptBegin(
        success = false,
        validationError = validationError
      )
    )
  }

  fun onAttemptStarted() {
    submissionTelemetryScope = telemetry.appendAndPushScope(
      TelemetryEvents.AttemptBegin(
        success = true,
        validationError = null
      )
    )
  }

  fun onPaymentEntityCreated(response: PaymentResponse) {
    val attemptScope = when {
      response.id.isNotBlank() -> telemetry.appendAndPushScope(
        TelemetryEvents.Attempt_PR(true, response.id)
      )

      response.sessionTokenRequestId != null -> telemetry.appendAndPushScope(
        TelemetryEvents.Attempt_PT(true, response.sessionTokenRequestId)
      )

      else -> null
    }
    attemptScope?.let { attemptPushedScopes.add(it) }
  }

  fun onRedirectActionPresented() {
    actionTelemetryScope =
      telemetry.appendAndPushScope(TelemetryEvents.ActionBegin(true))
    closeDigitalWallet(success = true)
    telemetry.expectingRedirectAway = true
  }

  fun onPresentToCustomerActionPresented() {
    actionTelemetryScope =
      telemetry.appendAndPushScope(TelemetryEvents.ActionBegin(true))
    closeDigitalWallet(success = true)
  }

  fun onAttemptError(errorCode: String?) {
    telemetry.append(TelemetryEvents.Attempt_Error(false, errorCode = errorCode))
  }

  fun onCopyText(fieldName: String) {
    telemetry.append(TelemetryEvents.ActionCopyText(true, fieldName))
  }

  fun discardAttemptIfInFlight(success: Boolean, failureCode: String? = null) {
    val hasScope = submissionTelemetryScope != null || attemptPushedScopes.isNotEmpty()
    if (!hasScope) return
    attemptPushedScopes.reversed().forEach { telemetry.popScope(it) }
    attemptPushedScopes.clear()
    submissionTelemetryScope?.let { telemetry.popScope(it) }
    submissionTelemetryScope = null
    telemetry.append(
      TelemetryEvents.AttemptDiscard(
        success = success,
        failureCode = failureCode
      )
    )
  }

  fun onSessionPending() {
    emitSessionPendingOnce()
  }

  fun onPaymentRequestStatusWhileSessionActive(status: PaymentRequestStatus?) {
    if (status == null || submissionTelemetryScope == null) return
    when (status) {
      PaymentRequestStatus.FAILED,
      PaymentRequestStatus.CANCELED,
      PaymentRequestStatus.EXPIRED -> {
        telemetry.append(
          TelemetryEvents.AttemptDiscard(
            success = false,
            failureCode = status.name
          )
        )
      }

      else -> Unit
    }
  }

  fun onActionClosed() {
    actionTelemetryScope?.let { scope ->
      runCatching {
        telemetry.append(TelemetryEvents.ActionClose(true))
        telemetry.popScope(scope)
      }
      actionTelemetryScope = null
    }
    telemetry.expectingRedirectAway = false
  }

  fun reset() {
    endTelemetryEmitted = false
    sessionPendingTelemetryEmitted = false
    loadedOrResumeTelemetryPushed = false
    digitalWalletLoadedTelemetryEmitted = false
    popAllTelemetryScopes()
    formInputSentKeys.clear()
    telemetry.expectingRedirectAway = false
  }

  fun onTerminalSessionStatus(status: PaymentSessionStatus) {
    when (status) {
      PaymentSessionStatus.CANCELED,
      PaymentSessionStatus.EXPIRED -> discardAttemptIfInFlight(
        success = false,
        failureCode = status.name
      )

      else -> Unit
    }
    emitTerminalEndIfNeeded(status)
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

  private fun emitSessionPendingOnce() {
    if (sessionPendingTelemetryEmitted) return
    sessionPendingTelemetryEmitted = true
    telemetry.append(TelemetryEvents.Pending(true))
  }

  private fun PaymentSessionStatus?.isTerminalSessionStatus(): Boolean {
    return when (this) {
      PaymentSessionStatus.COMPLETED,
      PaymentSessionStatus.EXPIRED,
      PaymentSessionStatus.CANCELED -> true

      PaymentSessionStatus.ACTIVE,
      PaymentSessionStatus.PENDING,
      null -> false
    }
  }

  private fun emitTerminalEndIfNeeded(status: PaymentSessionStatus) {
    val success = when (status) {
      PaymentSessionStatus.COMPLETED -> true
      PaymentSessionStatus.EXPIRED,
      PaymentSessionStatus.CANCELED -> false

      PaymentSessionStatus.ACTIVE,
      PaymentSessionStatus.PENDING -> return
    }
    if (endTelemetryEmitted) return
    endTelemetryEmitted = true
    telemetry.append(
      TelemetryEvents.End(
        success = success,
        status = status.name
      )
    )
    if (success) {
      closeDigitalWallet(success = true)
    }
    telemetry.flush()
  }
}
