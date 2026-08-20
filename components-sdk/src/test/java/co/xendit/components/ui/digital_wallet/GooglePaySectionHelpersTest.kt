package co.xendit.components.ui.digital_wallet

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GooglePaySectionHelpersTest {

  // ── mapGooglePayStatusToError ─────────────────────────────────────────────────────

  @Test fun `mapGooglePayStatusToError with CANCELED returns null (explicitly ignore user dismiss)`() {
    assertNull(mapGooglePayStatusToError(CommonStatusCodes.CANCELED))
  }

  @Test fun `mapGooglePayStatusToError with DEVELOPER_ERROR emits code + exact web-aligned title and message`() {
    val err = mapGooglePayStatusToError(CommonStatusCodes.DEVELOPER_ERROR)!!
    assertEquals("GOOGLE_PAY_DEVELOPER_ERROR", err.code)
    assertEquals("Google Pay Error", err.title)
    assertEquals(
      "Something went wrong with Google Pay. Please try again or use a different payment method.",
      err.message
    )
  }

  @Test fun `mapGooglePayStatusToError with INTERNAL_ERROR emits correct code and same generic message as dev error (web parity)`() {
    val err = mapGooglePayStatusToError(CommonStatusCodes.INTERNAL_ERROR)!!
    assertEquals("GOOGLE_PAY_INTERNAL_ERROR", err.code)
    assertEquals("Google Pay Error", err.title)
    assertEquals(
      "Something went wrong with Google Pay. Please try again or use a different payment method.",
      err.message
    )
  }

  @Test fun `mapGooglePayStatusToError with unknown non-documented code falls back to UNKNOWN with printed statusCode`() {
    val err = mapGooglePayStatusToError(statusCodeRaw = 42)!!
    assertEquals("GOOGLE_PAY_UNKNOWN_ERROR", err.code)
    assertEquals("Google Pay Error", err.title)
    assertTrue(err.message.contains("42"))
    assertTrue(err.message.startsWith("An unknown error occurred with Google Pay."))
  }

  @Test fun `mapGooglePayStatusToError with raw null input falls back to UNKNOWN with value -1 (placeholder)`() {
    val err = mapGooglePayStatusToError(null)!!
    assertEquals("GOOGLE_PAY_UNKNOWN_ERROR", err.code)
    assertTrue(err.message.contains("-1"))
  }

  // ── extractPaymentMethodType ──────────────────────────────────────────────────────

  @Test fun `extractPaymentMethodType returns nested paymentMethodData type field`() {
    val json = """
      {
        "apiVersionMinor":0,
        "paymentMethodData":{
          "type":"CARD",
          "tokenizationData":{"type":"PAYMENT_GATEWAY","token":"tok_1"}
        }
      }
    """.trimIndent()
    assertEquals("CARD", extractPaymentMethodType(json))
  }

  @Test fun `extractPaymentMethodType returns PayPal type when chosen`() {
    val json = """
      {"paymentMethodData":{"type":"PAYPAL","description":"paypal@x.com"}}
    """.trimIndent()
    assertEquals("PAYPAL", extractPaymentMethodType(json))
  }

  @Test fun `extractPaymentMethodType returns null when field missing`() {
    assertNull(extractPaymentMethodType("""{"apiVersion":2}"""))
  }

  @Test fun `extractPaymentMethodType returns null for malformed JSON`() {
    assertNull(extractPaymentMethodType("this is not json"))
  }

  @Test fun `extractPaymentMethodType returns null for empty or blank strings`() {
    assertNull(extractPaymentMethodType(""))
    assertNull(extractPaymentMethodType("   "))
  }

  @Test fun `extractPaymentMethodType handles paymentMethodData present but missing type`() {
    assertNull(extractPaymentMethodType("""{"paymentMethodData":{"description":"x"}}"""))
  }

  // ── extractStatusCode ─────────────────────────────────────────────────────────────

  @Test fun `extractStatusCode reads ApiException statusCode`() {
    val exc = ApiException(Status(CommonStatusCodes.DEVELOPER_ERROR))
    assertEquals(CommonStatusCodes.DEVELOPER_ERROR, extractStatusCode(exc))
  }

  @Test fun `extractStatusCode reads ResolvableApiException statusCode`() {
    val exc = ResolvableApiException(Status(CommonStatusCodes.INTERNAL_ERROR))
    assertEquals(CommonStatusCodes.INTERNAL_ERROR, extractStatusCode(exc))
  }

  @Test fun `extractStatusCode returns null for plain Exception without statusCode`() {
    assertNull(extractStatusCode(IllegalStateException("boom")))
    assertNull(extractStatusCode(null))
    assertNull(extractStatusCode(RuntimeException("msg")))
  }
}
