package co.xendit.components.ui

import co.xendit.components.XenditComponentsPaymentType
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.BffDigitalWallets
import co.xendit.components.data.model.BffGooglePay
import co.xendit.components.data.model.BffGooglePayAllowedMethod
import co.xendit.components.data.model.BffSession
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.SessionResponse
import com.google.gson.JsonObject
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GooglePayChannelLogicTest {

  private fun sessionResponseWithGooglePay(googlePay: BffGooglePay?): SessionResponse {
    return SessionResponse(
      session = null,
      paymentChannels = emptyList(),
      digitalWallets = BffDigitalWallets(googlePay = googlePay, applePay = null),
      succeededChannel = null
    )
  }

  private fun allowedMethod(
    channelCode: String,
    type: String
  ): BffGooglePayAllowedMethod {
    val spec = JsonObject().apply { addProperty("type", type) }
    return BffGooglePayAllowedMethod(channelCode = channelCode, paymentMethodSpecification = spec)
  }

  private fun googlePayWith(vararg methods: BffGooglePayAllowedMethod): BffGooglePay {
    return BffGooglePay(merchantId = "merch-TEST", allowedPaymentMethods = methods.toList())
  }

  private fun channel(
    channelCode: String,
    minAmount: BigDecimal? = null,
    maxAmount: BigDecimal? = null,
    pmType: XenditComponentsPaymentType? = null,
  ): BffChannel {
    return BffChannel(
      brandName = channelCode,
      brandLogoUrl = null,
      brandColor = "#000000",
      pmType = pmType,
      uiGroup = channelCode,
      channelCode = channelCode,
      allowPayWithoutSave = true,
      allowSave = false,
      minAmount = minAmount,
      maxAmount = maxAmount,
      requiresCustomerDetails = null,
      card = null,
      form = null,
      instructions = null,
    )
  }

  private fun sessionResponseForGooglePay(
    googlePay: BffGooglePay?,
    session: BffSession?,
    channels: List<BffChannel>,
  ): SessionResponse {
    return SessionResponse(
      session = session,
      paymentChannels = channels,
      digitalWallets = BffDigitalWallets(googlePay = googlePay, applePay = null),
      succeededChannel = null,
    )
  }

  private fun paySession(amount: BigDecimal): BffSession {
    return BffSession(
      id = "session-id",
      paymentSessionId = "pay-session-id",
      status = null,
      sessionType = BffSessionType.PAY,
      allowSavePaymentMethod = null,
      referenceId = null,
      currency = "IDR",
      country = "ID",
      amount = amount,
      items = emptyList(),
    )
  }

  // ── buildGooglePayChannelProperties ────────────────────────────────────────────────

  @Test fun `buildGooglePayChannelProperties with CARDS channel emits google_pay key with full JSON`() {
    val signed = """{"signature":"xyz","paymentMethodData":{"type":"CARD"}}"""
    val result = buildGooglePayChannelProperties(paymentDataJson = signed, channelCode = "CARDS")
    assertEquals(signed, result["google_pay"])
    assertEquals(1, result.size)
  }

  @Test fun `buildGooglePayChannelProperties non-CARDS wallet returns empty map (PayPal flow)`() {
    val signed = """{"paymentMethodData":{"type":"PAYPAL"}}"""
    val result = buildGooglePayChannelProperties(paymentDataJson = signed, channelCode = "PAYPAL")
    assertTrue(result.isEmpty())
  }

  @Test fun `buildGooglePayChannelProperties blank unknown code falls back to empty map`() {
    assertTrue(buildGooglePayChannelProperties("{\"a\":1}", channelCode = "").isEmpty())
    assertTrue(buildGooglePayChannelProperties("{\"a\":1}", channelCode = "SHOPEEPAY").isEmpty())
  }

  // ── resolveGooglePayChannelCodeOrError: success cases (type always present) ────────

  @Test fun `resolve matches single CARD allowed method to CARDS`() {
    val cfg = googlePayWith(allowedMethod("CARDS", type = "CARD"))
    val result = resolveGooglePayChannelCodeOrError(cfg, paymentMethodType = "CARD")
    assertTrue(result is ResolvedGooglePayChannel.Ok)
    assertEquals("CARDS", (result as ResolvedGooglePayChannel.Ok).code)
  }

  @Test fun `resolve picks the matching allowed method by type (PAYPAL) ignoring first-match`() {
    val cfg = googlePayWith(
      allowedMethod("CARDS", type = "CARD"),
      allowedMethod("PAYPAL", type = "PAYPAL")
    )
    assertEquals(
      "PAYPAL",
      (resolveGooglePayChannelCodeOrError(cfg, "PAYPAL") as ResolvedGooglePayChannel.Ok).code
    )
    assertEquals(
      "CARDS",
      (resolveGooglePayChannelCodeOrError(cfg, "CARD") as ResolvedGooglePayChannel.Ok).code
    )
  }

  @Test fun `resolve matches case-insensitive (lowercase card, mixed-case PayPal)`() {
    val cfg = googlePayWith(
      allowedMethod("CARDS", type = "CARD"),
      allowedMethod("PAYPAL", type = "PAYPAL")
    )
    assertEquals(
      "CARDS",
      (resolveGooglePayChannelCodeOrError(cfg, "card") as ResolvedGooglePayChannel.Ok).code
    )
    assertEquals(
      "PAYPAL",
      (resolveGooglePayChannelCodeOrError(cfg, "PaYpAl") as ResolvedGooglePayChannel.Ok).code
    )
  }

  // ── resolveGooglePayChannelCodeOrError: null/blank paymentMethodType ALWAYS errors (Web SDK parity) ──

  @Test fun `resolve when paymentMethodType is null AND 1 allowed method → ERR (Web SDK parity, no fallback even for size 1)`() {
    val cfgCards = googlePayWith(allowedMethod("CARDS", type = "CARD"))
    val err1 = resolveGooglePayChannelCodeOrError(cfgCards, paymentMethodType = null)
    assertTrue(err1 is ResolvedGooglePayChannel.Err)
    val t1 = (err1 as ResolvedGooglePayChannel.Err).userMessage
    assertTrue("Expected 'missing' keyword, got: $t1", t1.contains("missing"))
    assertTrue("Expected count 1 in message, got: $t1", t1.contains("1"))
    assertTrue("Expected method list, got: $t1", t1.contains("CARD"))

    val cfgPaypal = googlePayWith(allowedMethod("PAYPAL", type = "PAYPAL"))
    val err2 = resolveGooglePayChannelCodeOrError(cfgPaypal, paymentMethodType = null)
    assertTrue(err2 is ResolvedGooglePayChannel.Err)
    val t2 = (err2 as ResolvedGooglePayChannel.Err).userMessage
    assertTrue("Expected PAYPAL mentioned in configured methods, got: $t2", t2.contains("PAYPAL"))
  }

  @Test fun `resolve when paymentMethodType is blank or whitespace AND 1 allowed → ERR (same as null, Web SDK parity)`() {
    val cfg = googlePayWith(allowedMethod("CARDS", type = "CARD"))
    listOf("", "   ", "\t\n").forEach { blank ->
      val err = resolveGooglePayChannelCodeOrError(cfg, paymentMethodType = blank)
      assertTrue("Expected Err for blank input '$blank'", err is ResolvedGooglePayChannel.Err)
      val text = (err as ResolvedGooglePayChannel.Err).userMessage
      assertTrue("blank='$blank' should say 'missing', got: $text", text.contains("missing"))
      assertTrue("blank='$blank' should reference count 1, got: $text", text.contains("1"))
      assertTrue("blank='$blank' should list configured methods CARD, got: $text", text.contains("CARD"))
    }
  }

  @Test fun `resolve when paymentMethodType is null AND 2 allowed methods → ERR (matches Web SDK strict-match no-fallback behavior)`() {
    val cfg = googlePayWith(
      allowedMethod("CARDS", type = "CARD"),
      allowedMethod("PAYPAL", type = "PAYPAL")
    )
    val err = resolveGooglePayChannelCodeOrError(cfg, paymentMethodType = null)
    assertTrue(err is ResolvedGooglePayChannel.Err)
    val text = (err as ResolvedGooglePayChannel.Err).userMessage
    assertTrue("Expected 'missing' keyword, got: $text", text.contains("missing"))
    assertTrue("Expected count 2 in message, got: $text", text.contains("2"))
    assertTrue("Expected configured methods CARD PAYPAL in message, got: $text",
      text.contains("CARD") && text.contains("PAYPAL"))
  }

  @Test fun `resolve when paymentMethodType is blank or whitespace AND 3 allowed → ERR (Web SDK parity)`() {
    val cfg = googlePayWith(
      allowedMethod("SHOPEEPAY", type = "SHOPEEPAY"),
      allowedMethod("GOPAY", type = "GOPAY"),
      allowedMethod("CARDS", type = "CARD")
    )
    listOf("", "   ", "\t\n").forEach { blank ->
      val err = resolveGooglePayChannelCodeOrError(cfg, paymentMethodType = blank)
      assertTrue("Expected Err for blank input '$blank'", err is ResolvedGooglePayChannel.Err)
      val text = (err as ResolvedGooglePayChannel.Err).userMessage
      assertTrue("blank='$blank' should mention 'missing' type, got: $text", text.contains("missing"))
      assertTrue("blank='$blank' should reference count 3, got: $text", text.contains("3"))
      assertTrue("blank='$blank' should list SHOPEEPAY GOPAY CARD, got: $text",
        text.contains("SHOPEEPAY") && text.contains("GOPAY") && text.contains("CARD"))
    }
  }

  @Test fun `resolve when paymentMethodType is null AND multiple allowed AND first has null spec → still ERR`() {
    val cfg = BffGooglePay(
      merchantId = "m",
      allowedPaymentMethods = listOf(
        BffGooglePayAllowedMethod(channelCode = "CARDS", paymentMethodSpecification = null),
        allowedMethod("PAYPAL", type = "PAYPAL")
      )
    )
    val err = resolveGooglePayChannelCodeOrError(cfg, paymentMethodType = null)
    assertTrue(err is ResolvedGooglePayChannel.Err)
    val text = (err as ResolvedGooglePayChannel.Err).userMessage
    assertTrue("Expected 'missing' type keyword, got: $text", text.contains("missing"))
    assertTrue("Expected count 2 in message, got: $text", text.contains("2"))
    assertTrue("Expected PAYPAL in configured methods (null specs filtered out), got: $text",
      text.contains("PAYPAL"))
  }

  // ── resolveGooglePayChannelCodeOrError: other error cases ──────────────────────────

  @Test fun `resolve returns Err when googlePay config is missing (session did not return digital wallets)`() {
    val result = resolveGooglePayChannelCodeOrError(null, "CARD")
    assertTrue(result is ResolvedGooglePayChannel.Err)
    assertTrue((result as ResolvedGooglePayChannel.Err).userMessage.contains("missing from the session response"))
  }

  @Test fun `resolve returns Err when allowedPaymentMethods list is empty`() {
    val cfg = googlePayWith()
    val result = resolveGooglePayChannelCodeOrError(cfg, "CARD")
    assertTrue(result is ResolvedGooglePayChannel.Err)
    assertTrue((result as ResolvedGooglePayChannel.Err).userMessage.contains("empty"))
  }

  @Test fun `resolve returns Err when paymentMethodType does not match any configured type`() {
    val cfg = googlePayWith(
      allowedMethod("CARDS", type = "CARD"),
      allowedMethod("PAYPAL", type = "PAYPAL")
    )
    val err = resolveGooglePayChannelCodeOrError(cfg, paymentMethodType = "SHOPEEPAY")
    assertTrue(err is ResolvedGooglePayChannel.Err)
    val text = (err as ResolvedGooglePayChannel.Err).userMessage
    assertTrue(text.contains("SHOPEEPAY"))
    assertTrue(text.contains("CARD"))
    assertTrue(text.contains("PAYPAL"))
  }

  @Test fun `resolve returns Err when allowedPaymentMethods have null specs (unparseable) and type is provided`() {
    val spec = null
    val cfg = BffGooglePay(
      merchantId = "m",
      allowedPaymentMethods = listOf(
        BffGooglePayAllowedMethod(channelCode = "CARDS", paymentMethodSpecification = spec)
      )
    )
    val err = resolveGooglePayChannelCodeOrError(cfg, "CARD")
    assertTrue(err is ResolvedGooglePayChannel.Err)
    assertTrue((err as ResolvedGooglePayChannel.Err).userMessage.contains("CARD"))
    assertTrue(err.userMessage.contains("(none)"))
  }

  @Test fun `resolve returns Err when single allowed method has null spec AND paymentMethodType is blank`() {
    val cfg = BffGooglePay(
      merchantId = "m",
      allowedPaymentMethods = listOf(
        BffGooglePayAllowedMethod(channelCode = "CARDS", paymentMethodSpecification = null)
      )
    )
    val err = resolveGooglePayChannelCodeOrError(cfg, "")
    assertTrue(err is ResolvedGooglePayChannel.Err)
    val text = (err as ResolvedGooglePayChannel.Err).userMessage
    assertTrue("Expected 'missing' type in message (blank → missing type path), got: $text",
      text.contains("missing"))
    assertTrue("Expected count 1 in message, got: $text", text.contains("1"))
    assertTrue("Expected methods listed as (none) because specs are null, got: $text",
      text.contains("(none)"))
    assertNull(null)
    assertFalse(false)
  }

  @Test fun `shouldRenderGooglePaySection returns false when session has no google pay config`() {
    assertFalse(
      shouldRenderGooglePaySection(
        sessionResponse = sessionResponseWithGooglePay(googlePay = null),
        merchantPreferredPaymentMethod = listOf(XenditComponentsPaymentType.GOOGLE_PAY)
      )
    )
  }

  @Test fun `shouldRenderGooglePaySection returns true when config exists and google pay is allowed`() {
    assertTrue(
      shouldRenderGooglePaySection(
        sessionResponse = sessionResponseWithGooglePay(
          googlePay = googlePayWith(allowedMethod("CARDS", "CARD")),
        ),
        merchantPreferredPaymentMethod = emptyList(),
      )
    )
  }

  @Test fun `shouldRenderGooglePaySection returns false when preferred methods exclude google pay`() {
    assertFalse(
      shouldRenderGooglePaySection(
        sessionResponse = sessionResponseWithGooglePay(
          googlePay = googlePayWith(allowedMethod("CARDS", "CARD")),
        ),
        merchantPreferredPaymentMethod = listOf(XenditComponentsPaymentType.CARDS),
      )
    )
  }

  // ── filterGooglePayAllowedMethodsByAmount / min-max gating ────────────────────────

  @Test fun `filterGooglePayAllowedMethodsByAmount keeps allowed methods whose channel satisfies amount range`() {
    val googlePay = googlePayWith(
      allowedMethod("CARDS", "CARD"),
      allowedMethod("PAYPAL", "PAYPAL"),
    )
    val channels = listOf(
      channel("CARDS", minAmount = BigDecimal(5000), maxAmount = BigDecimal(50000)),
      channel("PAYPAL", minAmount = BigDecimal(100000), maxAmount = BigDecimal(1_000_000)),
    )

    val result = filterGooglePayAllowedMethodsByAmount(
      googlePay = googlePay,
      channels = channels,
      amount = BigDecimal(20000),
      sessionType = BffSessionType.PAY,
    )

    assertEquals(1, result.size)
    assertEquals("CARDS", result.single().channelCode)
  }

  @Test fun `filterGooglePayAllowedMethodsByAmount returns empty when amount below min for all allowed methods`() {
    val googlePay = googlePayWith(
      allowedMethod("CARDS", "CARD"),
      allowedMethod("PAYPAL", "PAYPAL"),
    )
    val channels = listOf(
      channel("CARDS", minAmount = BigDecimal(50000)),
      channel("PAYPAL", minAmount = BigDecimal(100000)),
    )

    val result = filterGooglePayAllowedMethodsByAmount(
      googlePay = googlePay,
      channels = channels,
      amount = BigDecimal(1000),
      sessionType = BffSessionType.PAY,
    )

    assertTrue(result.isEmpty())
  }

  @Test fun `filterGooglePayAllowedMethodsByAmount returns empty when amount above max for all allowed methods`() {
    val googlePay = googlePayWith(allowedMethod("CARDS", "CARD"))
    val channels = listOf(channel("CARDS", maxAmount = BigDecimal(50000)))

    val result = filterGooglePayAllowedMethodsByAmount(
      googlePay = googlePay,
      channels = channels,
      amount = BigDecimal(60000),
      sessionType = BffSessionType.PAY,
    )

    assertTrue(result.isEmpty())
  }

  @Test fun `filterGooglePayAllowedMethodsByAmount ignores amount when sessionType is not PAY`() {
    val googlePay = googlePayWith(allowedMethod("CARDS", "CARD"))
    val channels = listOf(channel("CARDS", minAmount = BigDecimal(50000), maxAmount = BigDecimal(100000)))

    val result = filterGooglePayAllowedMethodsByAmount(
      googlePay = googlePay,
      channels = channels,
      amount = BigDecimal(1000),
      sessionType = BffSessionType.SAVE,
    )

    assertEquals(1, result.size)
    assertEquals("CARDS", result.single().channelCode)
  }

  @Test fun `host show condition returns false when all allowed methods are filtered out by min and max amount`() {
    val googlePay = googlePayWith(allowedMethod("CARDS", "CARD"))
    val channels = listOf(
      channel("CARDS", minAmount = BigDecimal(100000), maxAmount = BigDecimal(500000))
    )
    val response = sessionResponseForGooglePay(
      googlePay = googlePay,
      session = paySession(amount = BigDecimal(5000)),
      channels = channels,
    )
    val filtered = filterGooglePayAllowedMethodsByAmount(
      googlePay = googlePay,
      channels = channels,
      amount = response.session?.amount,
      sessionType = response.session?.sessionType,
    )
    val shouldShow = shouldRenderGooglePaySection(
      sessionResponse = response,
      merchantPreferredPaymentMethod = listOf(XenditComponentsPaymentType.GOOGLE_PAY),
    ) && filtered.isNotEmpty()

    assertFalse(shouldShow)
  }

  @Test fun `host show condition returns true when at least one allowed method passes min and max amount filter`() {
    val googlePay = googlePayWith(
      allowedMethod("CARDS", "CARD"),
      allowedMethod("PAYPAL", "PAYPAL"),
    )
    val channels = listOf(
      channel("CARDS", minAmount = BigDecimal(100000)),
      channel("PAYPAL", minAmount = BigDecimal(10000), maxAmount = BigDecimal(500000)),
    )
    val response = sessionResponseForGooglePay(
      googlePay = googlePay,
      session = paySession(amount = BigDecimal(20000)),
      channels = channels,
    )
    val filtered = filterGooglePayAllowedMethodsByAmount(
      googlePay = googlePay,
      channels = channels,
      amount = response.session?.amount,
      sessionType = response.session?.sessionType,
    )
    val shouldShow = shouldRenderGooglePaySection(
      sessionResponse = response,
      merchantPreferredPaymentMethod = emptyList(),
    ) && filtered.isNotEmpty()

    assertTrue(shouldShow)
  }

  @Test fun `filterGooglePayAllowedMethodsByAmount ignores allowed methods whose channelCode is absent from channels list`() {
    val googlePay = googlePayWith(allowedMethod("CARDS", "CARD"))
    val channels = emptyList<BffChannel>()

    val result = filterGooglePayAllowedMethodsByAmount(
      googlePay = googlePay,
      channels = channels,
      amount = BigDecimal(20000),
      sessionType = BffSessionType.PAY,
    )

    assertTrue(result.isEmpty())
  }
}
