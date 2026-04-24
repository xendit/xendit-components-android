package co.xendit.components.data.network.repo.session

import co.xendit.components.data.model.CardDetails
import co.xendit.components.data.model.PaymentOptionsRequest
import co.xendit.components.data.model.PaymentRequest
import co.xendit.components.data.model.PaymentResponse
import co.xendit.components.data.model.SessionResponse
import co.xendit.components.data.model.PollResponse
import co.xendit.components.data.model.PaymentOptionsResponse
import retrofit2.Response

internal interface XenditRepository {
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

const val COMPONENT_VERSION = "v0.0.16"
