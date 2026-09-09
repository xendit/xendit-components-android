package co.xendit.components.ui

import co.xendit.components.data.mock.SAMPLE_CARD_CHANNEL
import co.xendit.components.data.model.PaymentAction
import co.xendit.components.data.model.PaymentActionDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentStateFlowStateTest {

  @Test
  fun `flowState defaults to loading session`() {
    val state = PaymentState()

    assertEquals(PaymentFlowState.LoadingSession, state.flowState)
    assertNull(state.overlayState)
  }

  @Test
  fun `flowState becomes startup error before session is loaded`() {
    val state = PaymentState(errorMessage = "boom")

    assertEquals(PaymentFlowState.StartupError("boom"), state.flowState)
  }

  @Test
  fun `flowState becomes selecting payment method when channels are available`() {
    val state = PaymentState(channels = listOf(SAMPLE_CARD_CHANNEL))

    assertEquals(PaymentFlowState.SelectingPaymentMethod, state.flowState)
  }

  @Test
  fun `flowState prioritizes redirect actions`() {
    val redirectAction = PaymentAction(
      type = "REDIRECT_CUSTOMER",
      descriptor = PaymentActionDescriptor.WEB_URL,
      value = "https://example.com"
    )
    val state = PaymentState(
      channels = listOf(SAMPLE_CARD_CHANNEL),
      paymentActionRedirect = redirectAction
    )

    assertEquals(PaymentFlowState.Redirecting(redirectAction), state.flowState)
  }

  @Test
  fun `overlayState prioritizes awaiting action over loading`() {
    val state = PaymentState(
      isLoading = true,
      awaitingPaymentAction = AwaitingPaymentAction.GooglePayProcessing
    )

    assertEquals(
      PaymentOverlayState.AwaitingAction(AwaitingPaymentAction.GooglePayProcessing),
      state.overlayState
    )
  }

  @Test
  fun `overlayState falls back to loading when no awaiting action exists`() {
    val state = PaymentState(isLoading = true)

    assertEquals(PaymentOverlayState.Loading, state.overlayState)
  }
}
