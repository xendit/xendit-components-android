package co.xendit.paymentsdk

import org.junit.Assert.assertEquals
import org.junit.Test

class XenditComponentsTest {

  @Test
  fun parseSdkKey_validKey_parsesCorrectly() {
    val sdkKey = "session-123-prod-PK123-SIG123"
    val keys = XenditComponents.parseSdkKey(sdkKey)

    assertEquals("session-123", keys.sessionAuthKey)
    assertEquals("PK123", keys.publicKey)
    assertEquals("SIG123", keys.signature)
  }

  @Test(expected = IllegalArgumentException::class)
  fun parseSdkKey_invalidKey_throwsException() {
    val sdkKey = "invalid-key"
    XenditComponents.parseSdkKey(sdkKey)
  }
}
