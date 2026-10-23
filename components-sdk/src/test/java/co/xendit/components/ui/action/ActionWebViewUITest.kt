package co.xendit.components.ui.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionWebViewUITest {

  @Test
  fun `generateBridgeToken produces 64-char hex token and is unique`() {
    val token1 = generateBridgeToken()
    val token2 = generateBridgeToken()

    assertEquals(64, token1.length)
    assertTrue(token1.matches(Regex("^[0-9a-f]{64}$")))
    assertNotEquals(token1, token2)
  }

  @Test
  fun `isTrustedMessageOrigin accepts xendit and tidnex https origins and initialOrigin`() {
    val initialOrigin = "https://acs.partnerbank.com"

    assertTrue(isTrustedMessageOrigin("https://redirect.xendit.co", initialOrigin))
    assertTrue(isTrustedMessageOrigin("https://xendit.co", initialOrigin))
    assertTrue(isTrustedMessageOrigin("https://checkout-ui-gateway.xendit.co", initialOrigin))
    assertTrue(isTrustedMessageOrigin("https://checkout-ui-gateway-live.stg.tidnex.dev", initialOrigin))
    assertTrue(isTrustedMessageOrigin("https://acs.partnerbank.com", initialOrigin))
  }

  @Test
  fun `isTrustedMessageOrigin rejects untrusted or non-https origins`() {
    val initialOrigin = "https://redirect.xendit.co"

    assertFalse(isTrustedMessageOrigin(null, initialOrigin))
    assertFalse(isTrustedMessageOrigin("", initialOrigin))
    assertFalse(isTrustedMessageOrigin("null", initialOrigin))
    assertFalse(isTrustedMessageOrigin("http://redirect.xendit.co", initialOrigin))
    assertFalse(isTrustedMessageOrigin("https://evilxendit.co", initialOrigin))
    assertFalse(isTrustedMessageOrigin("https://xendit.co.evil.com", initialOrigin))
    assertFalse(isTrustedMessageOrigin("https://attacker.example.com", initialOrigin))
  }
}
