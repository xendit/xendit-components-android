package co.xendit.paymentsdk.data.network.remote.session

import co.xendit.paymentsdk.data.model.CardDetails
import co.xendit.paymentsdk.data.model.CardInfoRequest
import co.xendit.paymentsdk.data.model.PaymentOptionsRequest
import co.xendit.paymentsdk.data.model.PaymentRequest
import co.xendit.paymentsdk.data.model.PaymentResponse
import co.xendit.paymentsdk.data.model.SessionResponse
import co.xendit.paymentsdk.data.model.PollResponse
import co.xendit.paymentsdk.data.model.PaymentOptionsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface XenditApi {
  @GET("api/sessions/{session_id}")
  suspend fun getSession(
    @Path("session_id") sessionId: String,
    @Query("components_version") componentsVersion: String
  ): Response<SessionResponse>

  @POST("api/sessions/payment_requests")
  suspend fun createPaymentRequest(
    @Body request: PaymentRequest,
    @Query("components_version") componentsVersion: String
  ): Response<PaymentResponse>

  @POST("api/sessions/payment_tokens")
  suspend fun createPaymentToken(
    @Body request: PaymentRequest,
    @Query("components_version") componentsVersion: String
  ): Response<PaymentResponse>

  @GET("api/sessions/payment_requests/{payment_request_id}")
  suspend fun getPaymentRequest(
    @Path("payment_request_id") paymentRequestId: String,
    @Query("components_version") componentsVersion: String
  ): Response<PaymentResponse>

  @POST("api/sessions/{session_id}/card_info")
  suspend fun getCardInfo(
    @Path("session_id") sessionId: String,
    @Query("components_version") componentsVersion: String,
    @Body request: CardInfoRequest
  ): Response<CardDetails>

  @GET("api/sessions/{session_id}/poll")
  suspend fun pollSession(
    @Path("session_id") sessionId: String,
    @Query("token_request_id") tokenRequestId: String?,
    @Query("components_version") componentsVersion: String
  ): Response<PollResponse>

  @POST("api/sessions/{session_id}/payment_options")
  suspend fun getPaymentOptions(
    @Path("session_id") sessionId: String,
    @Query("components_version") componentsVersion: String,
    @Body request: PaymentOptionsRequest
  ): Response<PaymentOptionsResponse>
}
