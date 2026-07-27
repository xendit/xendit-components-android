package co.xendit.components.data.model

import co.xendit.components.core.CoreSdkComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionResponseTest {

  @Test
  fun usesPaymentTokenSubmission_returnsTrueForSaveAndSubscription() {
    assertTrue(BffSessionType.SAVE.usesPaymentTokenSubmission())
    assertTrue(BffSessionType.SUBSCRIPTION.usesPaymentTokenSubmission())
    assertFalse(BffSessionType.PAY.usesPaymentTokenSubmission())
    assertFalse(BffSessionType.AUTHORIZATION.usesPaymentTokenSubmission())
    assertFalse((null as BffSessionType?).usesPaymentTokenSubmission())
  }

  @Test
  fun isPaySession_returnsTrueOnlyForPay() {
    assertTrue(BffSessionType.PAY.isPaySession())
    assertFalse(BffSessionType.SAVE.isPaySession())
    assertFalse(BffSessionType.SUBSCRIPTION.isPaySession())
    assertFalse(BffSessionType.AUTHORIZATION.isPaySession())
    assertFalse((null as BffSessionType?).isPaySession())
  }

  @Test
  fun deserialize_whenSubscriptionSessionPresent_mapsSubscriptionDetails() {
    val json =
      """
      {
        "session": {
          "payment_session_id": "ps-123",
          "status": "ACTIVE",
          "session_type": "SUBSCRIPTION",
          "reference_id": "ref-123",
          "currency": "IDR",
          "country": "ID",
          "amount": 0,
          "subscription": {
            "immediate_payment": true,
            "schedule": {
              "anchor_date": "2026-08-01T00:00:00Z",
              "interval": "MONTH",
              "interval_count": 1,
              "retry_interval": "DAY",
              "retry_interval_count": 2,
              "total_recurrence": 12,
              "total_retry": 3
            }
          }
        },
        "channels": []
      }
      """.trimIndent()

    val response = CoreSdkComponent.gson.fromJson(json, SessionResponse::class.java)

    assertNotNull(response)
    assertEquals(BffSessionType.SUBSCRIPTION, response.session?.sessionType)
    assertEquals(true, response.session?.subscription?.immediatePayment)
    assertEquals("2026-08-01T00:00:00Z", response.session?.subscription?.schedule?.anchorDate)
    assertEquals("MONTH", response.session?.subscription?.schedule?.interval)
    assertEquals(1, response.session?.subscription?.schedule?.intervalCount)
    assertEquals("DAY", response.session?.subscription?.schedule?.retryInterval)
    assertEquals(2, response.session?.subscription?.schedule?.retryIntervalCount)
    assertEquals(12, response.session?.subscription?.schedule?.totalRecurrence)
    assertEquals(3, response.session?.subscription?.schedule?.totalRetry)
  }
}
