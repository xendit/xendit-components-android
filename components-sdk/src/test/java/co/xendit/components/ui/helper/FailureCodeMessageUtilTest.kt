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
  fun resolveFailureMessage_unknownCode_usesUnknownTemplateAndReplacesFailureCode() {
    val context = mockk<Context>()
    every { context.getString(R.string.sessionfailure_code_unknown) } returns "Failure {{failureCode}}"

    val result = FailureCodeMessageUtil.resolveFailureMessage(context, "SOME_NEW_CODE")

    assertEquals("Failure SOME_NEW_CODE", result)
    verify(exactly = 1) { context.getString(R.string.sessionfailure_code_unknown) }
  }

  @Test
  fun resolveFailureMessage_blankCode_usesUNKNOWNReplacement() {
    val context = mockk<Context>()
    every { context.getString(R.string.sessionfailure_code_unknown) } returns "Failure {{failureCode}}"

    val result = FailureCodeMessageUtil.resolveFailureMessage(context, "   ")

    assertEquals("Failure UNKNOWN", result)
    verify(exactly = 1) { context.getString(R.string.sessionfailure_code_unknown) }
  }

  @Test
  fun resolveFailureMessage_nullCode_usesUNKNOWNReplacement() {
    val context = mockk<Context>()
    every { context.getString(R.string.sessionfailure_code_unknown) } returns "Failure {{failureCode}}"

    val result = FailureCodeMessageUtil.resolveFailureMessage(context, null)

    assertEquals("Failure UNKNOWN", result)
    verify(exactly = 1) { context.getString(R.string.sessionfailure_code_unknown) }
  }
}

