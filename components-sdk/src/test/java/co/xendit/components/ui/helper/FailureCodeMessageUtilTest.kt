package co.xendit.components.ui.helper

import android.content.Context
import co.xendit.components.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class FailureCodeMessageUtilTest {

  @Test
  fun resolveFailureMessage_knownCode_returnsStringFromCorrectResId() {
    val context = mockk<Context>()
    every { context.getString(R.string.sessionfailure_code_declined_by_issuer) } returns "Declined by issuer"

    val result = FailureCodeMessageUtil.resolveFailureMessage(context, "DECLINED_BY_ISSUER")

    assertEquals("Declined by issuer", result)
    verify(exactly = 1) { context.getString(R.string.sessionfailure_code_declined_by_issuer) }
  }

  @Test
  fun resolveFailureMessage_unknownCode_returnsDefaultFailedSubtext() {
    val context = mockk<Context>()
    every { context.getString(R.string.sessionpayment_token_status_failed_subtext) } returns "Payment failed"

    val result = FailureCodeMessageUtil.resolveFailureMessage(context, "SOME_NEW_CODE")

    assertEquals("Payment failed", result)
    verify(exactly = 1) { context.getString(R.string.sessionpayment_token_status_failed_subtext) }
  }

  @Test
  fun resolveFailureMessage_blankCode_returnsDefaultFailedSubtext() {
    val context = mockk<Context>()
    every { context.getString(R.string.sessionpayment_token_status_failed_subtext) } returns "Payment failed"

    val result = FailureCodeMessageUtil.resolveFailureMessage(context, "   ")

    assertEquals("Payment failed", result)
    verify(exactly = 1) { context.getString(R.string.sessionpayment_token_status_failed_subtext) }
  }

  @Test
  fun resolveFailureMessage_nullCode_returnsDefaultFailedSubtext() {
    val context = mockk<Context>()
    every { context.getString(R.string.sessionpayment_token_status_failed_subtext) } returns "Payment failed"

    val result = FailureCodeMessageUtil.resolveFailureMessage(context, null)

    assertEquals("Payment failed", result)
    verify(exactly = 1) { context.getString(R.string.sessionpayment_token_status_failed_subtext) }
  }
}
