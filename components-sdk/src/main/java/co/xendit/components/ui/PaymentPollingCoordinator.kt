package co.xendit.components.ui

import co.xendit.components.data.model.PaymentRequestStatus
import co.xendit.components.data.model.PaymentSessionStatus
import co.xendit.components.data.model.PollResponse
import co.xendit.components.data.network.repo.session.XenditRepository
import co.xendit.components.data.network.repo.session.XenditRepositoryResult

internal data class PaymentPollingSnapshot(
  val poll: PollResponse,
  val sessionStatus: PaymentSessionStatus?,
  val paymentRequestStatus: PaymentRequestStatus?,
  val sessionPending: Boolean,
  val hasTerminalEntity: Boolean
)

internal sealed interface PaymentPollingResult {
  data class Success(val snapshot: PaymentPollingSnapshot) : PaymentPollingResult
  data class Failure(val reason: String) : PaymentPollingResult
}

internal class PaymentPollingCoordinator(
  private val repository: XenditRepository
) {
  suspend fun poll(
    sessionAuthKey: String,
    tokenRequestId: String?
  ): PaymentPollingResult {
    return when (val result = repository.pollSession(sessionAuthKey, tokenRequestId)) {
      is XenditRepositoryResult.Success -> {
        val poll = result.data
        val sessionStatus = poll.session?.status
        val paymentRequestStatus = poll.paymentRequest?.status
        PaymentPollingResult.Success(
          PaymentPollingSnapshot(
            poll = poll,
            sessionStatus = sessionStatus,
            paymentRequestStatus = paymentRequestStatus,
            sessionPending = sessionStatus == PaymentSessionStatus.PENDING,
            hasTerminalEntity = sessionStatus.isTerminalSessionStatus() ||
              paymentRequestStatus.isTerminalPaymentEntityStatus()
          )
        )
      }

      is XenditRepositoryResult.Failure -> {
        PaymentPollingResult.Failure(
          reason = result.errorCode ?: result.message ?: "Poll failed"
        )
      }

      is XenditRepositoryResult.EmptyBody -> {
        PaymentPollingResult.Failure(reason = "Empty poll response")
      }
    }
  }
}

internal fun PaymentSessionStatus?.isTerminalSessionStatus(): Boolean {
  return when (this) {
    PaymentSessionStatus.COMPLETED,
    PaymentSessionStatus.EXPIRED,
    PaymentSessionStatus.CANCELED -> true

    PaymentSessionStatus.ACTIVE,
    PaymentSessionStatus.PENDING,
    null -> false
  }
}

internal fun PaymentRequestStatus?.isTerminalPaymentEntityStatus(): Boolean {
  return when (this) {
    PaymentRequestStatus.SUCCEEDED,
    PaymentRequestStatus.FAILED,
    PaymentRequestStatus.CANCELED,
    PaymentRequestStatus.EXPIRED -> true

    PaymentRequestStatus.ACCEPTING_PAYMENTS,
    PaymentRequestStatus.REQUIRES_ACTION,
    PaymentRequestStatus.PENDING,
    PaymentRequestStatus.AUTHORIZED,
    null -> false
  }
}
