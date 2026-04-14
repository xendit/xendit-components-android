package co.xendit.paymentsdk.data.network.repo.session

import co.xendit.paymentsdk.data.model.CardDetails
import co.xendit.paymentsdk.data.model.PaymentOptionsRequest
import co.xendit.paymentsdk.data.model.PaymentRequest
import co.xendit.paymentsdk.data.model.PaymentResponse
import co.xendit.paymentsdk.data.model.SessionResponse
import co.xendit.paymentsdk.data.model.PollResponse
import co.xendit.paymentsdk.data.model.PaymentOptionsResponse
import retrofit2.Response

interface XenditRepository {
  suspend fun getSession(
    sessionId: String,
    componentsVersion: String = COMPONENT_VERSION
  ): Response<SessionResponse>

  suspend fun createPaymentRequest(
    request: PaymentRequest,
    componentsVersion: String = COMPONENT_VERSION
  ): Response<PaymentResponse>

  suspend fun createPaymentToken(
    request: PaymentRequest,
    componentsVersion: String = COMPONENT_VERSION
  ): Response<PaymentResponse>

  suspend fun getPaymentRequest(
    paymentRequestId: String,
    componentsVersion: String = COMPONENT_VERSION
  ): Response<PaymentResponse>

  suspend fun getCardInfo(
    sessionId: String,
    componentsVersion: String = COMPONENT_VERSION,
    encryptedCardNumber: String
  ): Response<CardDetails>

  suspend fun pollSession(
    sessionId: String,
    tokenRequestId: String?,
    componentsVersion: String = COMPONENT_VERSION
  ): Response<PollResponse>

  suspend fun getPaymentOptions(
    sessionId: String,
    componentsVersion: String = COMPONENT_VERSION,
    request: PaymentOptionsRequest
  ): Response<PaymentOptionsResponse>
}

const val COMPONENT_VERSION = "v0.0.18"
