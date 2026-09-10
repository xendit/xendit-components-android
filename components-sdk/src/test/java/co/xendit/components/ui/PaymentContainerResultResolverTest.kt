package co.xendit.components.ui

import co.xendit.components.data.model.BffSessionAllowSavePaymentMethod
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.BffSession
import co.xendit.components.data.model.PaymentRequestStatus
import co.xendit.components.data.model.PaymentResponse
import co.xendit.components.data.model.PaymentSessionStatus
import co.xendit.components.data.model.PollResponse
import co.xendit.components.data.model.SessionResponse
import co.xendit.components.data.model.SucceededChannel
import co.xendit.components.data.model.XenditPaymentResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PaymentContainerResultResolverTest {

  private val resolver = PaymentContainerResultResolver { code -> "resolved:$code" }

  @Test
  fun `resolve session completed to success result`() {
    val effect = resolver.resolveSession(
      SessionResponse(
        session = BffSession(
          id = "session-1",
          paymentSessionId = "payment-session-1",
          status = PaymentSessionStatus.COMPLETED,
          sessionType = BffSessionType.PAY,
          allowSavePaymentMethod = BffSessionAllowSavePaymentMethod.OPTIONAL,
          referenceId = "ref-1",
          currency = "IDR",
          country = "ID",
          amount = BigDecimal.TEN,
          items = emptyList()
        ),
        paymentChannels = emptyList(),
        succeededChannel = SucceededChannel(channelCode = "BCA_VA", logoUrl = null)
      )
    )

    assertTrue(effect is PaymentContainerEffect.Finish)
    val result = (effect as PaymentContainerEffect.Finish).result
    assertTrue(result is XenditPaymentResult.Success)
    result as XenditPaymentResult.Success
    assertEquals("payment-session-1", result.paymentRequestId)
    assertEquals("BCA_VA", result.channelCode)
  }

  @Test
  fun `resolve poll failed to notify failure with snackbar and close action`() {
    val effect = resolver.resolvePoll(
      PollResponse(
        session = BffSession(
          id = "session-1",
          paymentSessionId = "payment-session-1",
          status = PaymentSessionStatus.ACTIVE,
          sessionType = BffSessionType.PAY,
          allowSavePaymentMethod = BffSessionAllowSavePaymentMethod.OPTIONAL,
          referenceId = "ref-1",
          currency = "IDR",
          country = "ID",
          amount = BigDecimal.TEN,
          items = emptyList()
        ),
        paymentRequest = PaymentResponse(
          id = "pr-1",
          status = PaymentRequestStatus.FAILED,
          failure_code = "CARD_DECLINED",
          channelCode = "CARDS"
        ),
        paymentToken = null,
        succeededChannel = null
      )
    )

    assertTrue(effect is PaymentContainerEffect.NotifyFailure)
    effect as PaymentContainerEffect.NotifyFailure
    assertTrue(effect.closeWebPayment)
    assertEquals("resolved:CARD_DECLINED", effect.snackbarMessage)
    assertEquals("CARD_DECLINED", effect.result.error.code)
  }

  @Test
  fun `resolve network api error to notify failure`() {
    val effect = resolver.resolveApiError(
      errorCode = "NETWORK_ERROR",
      message = "No internet"
    )

    assertTrue(effect is PaymentContainerEffect.NotifyFailure)
    effect as PaymentContainerEffect.NotifyFailure
    assertEquals("No internet", effect.result.error.message)
    assertEquals("NETWORK_ERROR", effect.result.error.code)
    assertEquals("No internet", effect.snackbarMessage)
  }
}
