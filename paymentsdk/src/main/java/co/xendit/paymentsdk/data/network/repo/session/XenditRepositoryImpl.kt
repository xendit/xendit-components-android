package co.xendit.paymentsdk.data.network.repo.session

import co.xendit.paymentsdk.core.model.SafeApiCall
import co.xendit.paymentsdk.data.model.CardDetails
import co.xendit.paymentsdk.data.model.CardInfoRequest
import co.xendit.paymentsdk.data.model.PaymentOptionsRequest
import co.xendit.paymentsdk.data.model.PaymentRequest
import co.xendit.paymentsdk.data.model.PaymentResponse
import co.xendit.paymentsdk.data.model.SessionResponse
import co.xendit.paymentsdk.data.model.PollResponse
import co.xendit.paymentsdk.data.model.PaymentOptionsResponse
import co.xendit.paymentsdk.data.network.remote.session.XenditApi
import retrofit2.Response

class XenditRepositoryImpl(
  private val safeApiCall: SafeApiCall,
  private val api: XenditApi,
) : XenditRepository {
  override suspend fun getSession(
    sessionId: String,
    componentsVersion: String
  ): Response<SessionResponse> {
    return safeApiCall.call { api.getSession(sessionId, componentsVersion) }
  }

  override suspend fun createPaymentRequest(
    request: PaymentRequest,
    componentsVersion: String
  ): Response<PaymentResponse> {
    return safeApiCall.call { api.createPaymentRequest(request, componentsVersion) }
  }

  override suspend fun createPaymentToken(
    request: PaymentRequest,
    componentsVersion: String
  ): Response<PaymentResponse> {
    return safeApiCall.call { api.createPaymentToken(request, componentsVersion) }
  }

  override suspend fun getPaymentRequest(
    paymentRequestId: String,
    componentsVersion: String
  ): Response<PaymentResponse> {
    return safeApiCall.call { api.getPaymentRequest(paymentRequestId, componentsVersion) }
  }

  override suspend fun getCardInfo(
    sessionId: String,
    componentsVersion: String,
    encryptedCardNumber: String
  ): Response<CardDetails> {
    return safeApiCall.call {
      api.getCardInfo(
        sessionId = sessionId,
        componentsVersion = componentsVersion,
        request = CardInfoRequest(cardNumber = encryptedCardNumber)
      )
    }
  }

  override suspend fun pollSession(
    sessionId: String,
    tokenRequestId: String?,
    componentsVersion: String
  ): Response<PollResponse> {
    return safeApiCall.call { api.pollSession(sessionId, tokenRequestId, componentsVersion) }
  }

  override suspend fun getPaymentOptions(
    sessionId: String,
    componentsVersion: String,
    request: PaymentOptionsRequest
  ): Response<PaymentOptionsResponse> {
    return safeApiCall.call { api.getPaymentOptions(sessionId, componentsVersion, request) }
  }
}
