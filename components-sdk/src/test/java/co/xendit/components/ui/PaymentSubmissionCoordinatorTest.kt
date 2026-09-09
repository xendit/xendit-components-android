package co.xendit.components.ui

import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.FieldType
import co.xendit.components.data.model.PaymentAction
import co.xendit.components.data.model.PaymentActionDescriptor
import co.xendit.components.data.model.PaymentRequest
import co.xendit.components.data.model.PaymentRequestStatus
import co.xendit.components.data.model.PaymentResponse
import co.xendit.components.data.network.repo.session.XenditRepository
import co.xendit.components.data.network.repo.session.XenditRepositoryResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentSubmissionCoordinatorTest {

  private val repository = mockk<XenditRepository>(relaxed = true)
  private val coordinator = PaymentSubmissionCoordinator(repository)
  private val executionContext = PaymentExecutionContext(
    sessionAuthKey = "auth-key",
    publicKey = "pk-key",
    paymentSessionId = "payment-session-id"
  )

  @Test
  fun `submit returns validation failure before repository call`() = runTest {
    val result = coordinator.submit(
      sessionType = null,
      context = executionContext,
      formValues = emptyMap(),
      fields = listOf(
        ChannelFormField(
          label = "Card Number",
          type = FieldType.Text(),
          channelProperty = "card_number",
          required = true,
          span = 2
        )
      )
    ) {
      PaymentRequest(
        sessionId = it.sessionAuthKey,
        channelCode = "CARDS",
        channelProperties = emptyMap()
      )
    }

    assertTrue(result is PaymentSubmissionResult.ValidationFailure)
    result as PaymentSubmissionResult.ValidationFailure
    assertEquals("CARD_NUMBER_REQUIRED", result.validationError)
    coVerify(exactly = 0) { repository.createPaymentRequest(any(), any()) }
    coVerify(exactly = 0) { repository.createPaymentToken(any(), any()) }
  }

  @Test
  fun `submit classifies redirect action`() = runTest {
    coEvery { repository.createPaymentRequest(any(), any()) } returns XenditRepositoryResult.Success(
      PaymentResponse(
        id = "pr-123",
        status = PaymentRequestStatus.REQUIRES_ACTION,
        paymentActions = listOf(
          PaymentAction(
            type = "REDIRECT_CUSTOMER",
            descriptor = PaymentActionDescriptor.WEB_URL,
            value = "https://example.com"
          )
        )
      )
    )

    val result = coordinator.submit(
      sessionType = null,
      context = executionContext,
      formValues = mapOf("card_number" to "4111111111111111"),
      fields = emptyList()
    ) {
      PaymentRequest(
        sessionId = it.sessionAuthKey,
        channelCode = "CARDS",
        channelProperties = emptyMap()
      )
    }

    assertTrue(result is PaymentSubmissionResult.Success)
    result as PaymentSubmissionResult.Success
    assertTrue(result.data.presentationTarget is PaymentPresentationTarget.Redirect)
    assertEquals("pr-123", result.data.response.id)
  }
}
