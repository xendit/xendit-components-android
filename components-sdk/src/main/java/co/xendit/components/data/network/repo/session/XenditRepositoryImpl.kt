package co.xendit.components.data.network.repo.session

import co.xendit.components.core.model.SafeApiCall
import co.xendit.components.data.model.CardDetails
import co.xendit.components.data.model.CardInfoRequest
import co.xendit.components.data.model.PaymentOptionsRequest
import co.xendit.components.data.model.PaymentRequest
import co.xendit.components.data.model.PaymentResponse
import co.xendit.components.data.model.SessionResponse
import co.xendit.components.data.model.PollResponse
import co.xendit.components.data.model.PaymentOptionsResponse
import co.xendit.components.data.model.SimulatePaymentRequest
import co.xendit.components.data.network.remote.session.XenditApi

internal class XenditRepositoryImpl(
  private val safeApiCall: SafeApiCall,
  private val api: XenditApi,
) : XenditRepository {
  override suspend fun getSession(
    sessionId: String,
    componentsVersion: String
  ): XenditRepositoryResult<SessionResponse> {
    return safeApiCall.call { api.getSession(sessionId, componentsVersion) }
      .toXenditRepositoryResult()
  }

  override suspend fun createPaymentRequest(
    request: PaymentRequest,
    componentsVersion: String
  ): XenditRepositoryResult<PaymentResponse> {
    return safeApiCall.call { api.createPaymentRequest(request, componentsVersion) }
      .toXenditRepositoryResult()
  }

  override suspend fun createPaymentToken(
    request: PaymentRequest,
    componentsVersion: String
  ): XenditRepositoryResult<PaymentResponse> {
    return safeApiCall.call { api.createPaymentToken(request, componentsVersion) }
      .toXenditRepositoryResult()
  }

  override suspend fun getPaymentRequest(
    paymentRequestId: String,
    componentsVersion: String
  ): XenditRepositoryResult<PaymentResponse> {
    return safeApiCall.call { api.getPaymentRequest(paymentRequestId, componentsVersion) }
      .toXenditRepositoryResult()
  }

  override suspend fun getCardInfo(
    sessionId: String,
    componentsVersion: String,
    encryptedCardNumber: String
  ): XenditRepositoryResult<CardDetails> {
    return safeApiCall.call {
      api.getCardInfo(
        sessionId = sessionId,
        componentsVersion = componentsVersion,
        request = CardInfoRequest(cardNumber = encryptedCardNumber)
      )
    }.toXenditRepositoryResult()
  }

  override suspend fun pollSession(
    sessionId: String,
    tokenRequestId: String?,
    componentsVersion: String
  ): XenditRepositoryResult<PollResponse> {
    return safeApiCall.call { api.pollSession(sessionId, tokenRequestId, componentsVersion) }
      .toXenditRepositoryResult()
  }

  override suspend fun simulatePaymentRequest(
    sessionId: String,
    paymentRequestId: String,
    request: SimulatePaymentRequest,
    componentsVersion: String
  ): XenditRepositoryResult<PaymentResponse> {
    return safeApiCall.call {
      api.simulatePaymentRequest(
        sessionId = sessionId,
        paymentRequestId = paymentRequestId,
        componentsVersion = componentsVersion,
        request = request
      )
    }.toXenditRepositoryResult()
  }

  override suspend fun getPaymentOptions(
    sessionId: String,
    componentsVersion: String,
    request: PaymentOptionsRequest
  ): XenditRepositoryResult<PaymentOptionsResponse> {
    return safeApiCall.call { api.getPaymentOptions(sessionId, componentsVersion, request) }
      .toXenditRepositoryResult()
  }
}
