package co.xendit.components.ui

import android.content.Context
import co.xendit.components.core.model.GlobalErrorHandler
import co.xendit.components.data.model.BffBusiness
import co.xendit.components.data.model.BffDigitalWallets
import co.xendit.components.data.model.BffGooglePay
import co.xendit.components.data.model.BffGooglePayAllowedMethod
import co.xendit.components.data.model.BffItem
import co.xendit.components.data.model.BffSession
import co.xendit.components.data.model.BffSessionAllowSavePaymentMethod
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.PaymentSessionStatus
import co.xendit.components.data.model.PollResponse
import co.xendit.components.data.model.SessionResponse
import co.xendit.components.data.network.repo.session.XenditRepository
import co.xendit.components.ui.components.molecule.UiText
import com.google.gson.JsonObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelGooglePayTest {

  private lateinit var repository: XenditRepository
  private lateinit var context: Context
  private lateinit var errorHandler: GlobalErrorHandler
  private lateinit var viewModel: PaymentViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk(relaxed = true)
    context = mockk(relaxed = true)
    errorHandler = spyk(GlobalErrorHandler(context = context))
    viewModel = PaymentViewModel(
      xenditRepository = repository,
      globalErrorHandler = errorHandler
    )
  }

  @After
  fun tearDown() {
    if (::viewModel.isInitialized) {
      runCatching { viewModel.wipeAllSensitiveData() }
    }
    Dispatchers.resetMain()
  }

  private fun allowMethod(channelCode: String, type: String): BffGooglePayAllowedMethod {
    val spec = JsonObject().apply { addProperty("type", type) }
    return BffGooglePayAllowedMethod(channelCode = channelCode, paymentMethodSpecification = spec)
  }

  private fun stubSessionResponseWithGooglePay(
    allowedMethods: List<BffGooglePayAllowedMethod>,
    sessionType: BffSessionType = BffSessionType.PAY,
    paymentSessionId: String = "pay-session-id",
    sessionAuthKey: String? = null,
    publicKey: String? = null,
    lastSessionTokenRequestId: String? = null
  ) {
    val googlePay = BffGooglePay(merchantId = "merch-123", allowedPaymentMethods = allowedMethods)
    val wallets = BffDigitalWallets(googlePay = googlePay, applePay = null)
    val bffSession = BffSession(
      id = paymentSessionId,
      paymentSessionId = paymentSessionId,
      status = PaymentSessionStatus.ACTIVE,
      sessionType = sessionType,
      allowSavePaymentMethod = BffSessionAllowSavePaymentMethod.DISABLED,
      referenceId = "ref-1",
      currency = "IDR",
      country = "ID",
      amount = BigDecimal.ONE,
      items = listOf(
        BffItem(
          referenceId = "item-1",
          type = "PRODUCT",
          name = "Product",
          netUnitAmount = 1000L,
          quantity = 1,
          category = "goods"
        )
      )
    )
    val resp = SessionResponse(
      session = bffSession,
      business = BffBusiness(
        name = "Business",
        countryOfOperation = "ID",
        merchantProfilePictureUrl = null
      ),
      paymentChannels = emptyList(),
      channelUiGroups = emptyList(),
      succeededChannel = null,
      digitalWallets = wallets
    )
    viewModel.injectSessionState(
      sessionResponse = resp,
      sessionType = sessionType,
      paymentSessionId = paymentSessionId,
      sessionAuthKey = sessionAuthKey,
      publicKey = publicKey,
      lastSessionTokenRequestId = lastSessionTokenRequestId
    )
  }

  // ── ActionIntent.GooglePayPaymentFailed ────────────────────────────────────────────

  @Test fun `GooglePayPaymentFailed sets concatenated title+message to state errorMessage and posts to handler`() = runTest {
    viewModel.dispatch(
      ActionIntent.GooglePayPaymentFailed(
        code = "GOOGLE_PAY_DEVELOPER_ERROR",
        title = "Google Pay Error",
        message = "Something went wrong with Google Pay. Please try again or use a different payment method."
      )
    )
    advanceUntilIdle()

    val err = viewModel.state.value.errorMessage
    assertNotNull(err)
    assertEquals(
      "Google Pay Error. Something went wrong with Google Pay. Please try again or use a different payment method.",
      err
    )
    assertFalse(viewModel.state.value.isLoading)
    verify(exactly = 1) {
      errorHandler.postError(errorMessage = any<UiText.DynamicString>())
    }
  }

  @Test fun `GooglePayPaymentFailed when title blank falls back to message only`() = runTest {
    viewModel.dispatch(
      ActionIntent.GooglePayPaymentFailed(
        code = "GOOGLE_PAY_UNKNOWN_ERROR",
        title = "",
        message = "Something went wrong"
      )
    )
    advanceUntilIdle()
    assertEquals("Something went wrong", viewModel.state.value.errorMessage)
  }

  @Test fun `GooglePayPaymentFailed when message blank falls back to title only`() = runTest {
    viewModel.dispatch(
      ActionIntent.GooglePayPaymentFailed(
        code = "GOOGLE_PAY_UNKNOWN_ERROR",
        title = "Google Pay Error",
        message = ""
      )
    )
    advanceUntilIdle()
    assertEquals("Google Pay Error", viewModel.state.value.errorMessage)
  }

  @Test fun `GooglePayPaymentFailed when all text blank falls back to code`() = runTest {
    viewModel.dispatch(
      ActionIntent.GooglePayPaymentFailed(
        code = "GOOGLE_PAY_UNKNOWN_ERROR",
        title = "",
        message = ""
      )
    )
    advanceUntilIdle()
    assertEquals("GOOGLE_PAY_UNKNOWN_ERROR", viewModel.state.value.errorMessage)
  }

  // ── ActionIntent.SubmitGooglePay: configuration errors (synchronous, before network) ──

  @Test fun `SubmitGooglePay with missing googlePay config posts error WITHOUT touching repository`() = runTest {
    val signed = """{"paymentMethodData":{"type":"CARD"}}"""
    viewModel.dispatch(
      ActionIntent.SubmitGooglePay(paymentDataJson = signed, paymentMethodType = "CARD")
    )
    advanceUntilIdle()

    val err = viewModel.state.value.errorMessage
    assertNotNull(err)
    assertTrue(err!!.contains("missing from the session response"))
    assertFalse(viewModel.state.value.isLoading)
    verify(exactly = 1) { errorHandler.postError(errorMessage = any<UiText>()) }
  }

  @Test fun `SubmitGooglePay with empty allowedPaymentMethods posts empty configuration error`() = runTest {
    stubSessionResponseWithGooglePay(allowedMethods = emptyList())

    viewModel.dispatch(
      ActionIntent.SubmitGooglePay(
        paymentDataJson = "{\"paymentMethodData\":{\"type\":\"CARD\"}}",
        paymentMethodType = "CARD"
      )
    )
    advanceUntilIdle()

    val msg = viewModel.state.value.errorMessage
    assertNotNull("Expected error when allowedPaymentMethods is empty", msg)
    assertTrue("Expected empty configuration message, got: $msg", msg!!.contains("empty"))
    assertFalse("Loading flag must be reset after error", viewModel.state.value.isLoading)
  }

  @Test fun `SubmitGooglePay when paymentMethodType not in allowed list posts unsupported-type error`() = runTest {
    stubSessionResponseWithGooglePay(
      allowedMethods = listOf(allowMethod("CARDS", "CARD"), allowMethod("PAYPAL", "PAYPAL"))
    )

    viewModel.dispatch(
      ActionIntent.SubmitGooglePay(
        paymentDataJson = "{\"paymentMethodData\":{\"type\":\"SHOPEEPAY\"}}",
        paymentMethodType = "SHOPEEPAY"
      )
    )
    advanceUntilIdle()

    val msg = viewModel.state.value.errorMessage
    assertNotNull(msg)
    assertTrue(
      "Expected SHOPEEPAY to appear in error message, got: $msg",
      msg!!.contains("SHOPEEPAY")
    )
    assertTrue(
      "Expected allowed methods list [CARD, PAYPAL] in error, got: $msg",
      msg.contains("CARD") && msg.contains("PAYPAL")
    )
    assertFalse(viewModel.state.value.isLoading)
    verify(exactly = 1) { errorHandler.postError(errorMessage = any<UiText>()) }
  }

  @Test fun `SubmitGooglePay with null paymentMethodType AND SINGLE allowed method → ERR (Web SDK parity, no size-1 fallback)`() = runTest {
    stubSessionResponseWithGooglePay(
      allowedMethods = listOf(allowMethod("CARDS", "CARD"))
    )
    viewModel.dispatch(
      ActionIntent.SubmitGooglePay(
        paymentDataJson = "{\"paymentMethodData\":{}}",
        paymentMethodType = null
      )
    )
    advanceUntilIdle()

    val err = viewModel.state.value.errorMessage
    assertNotNull("Expected missing-type resolution error, got null", err)
    assertTrue(
      "Expected 'missing' keyword in error message, got: $err",
      err!!.contains("missing")
    )
    assertTrue(
      "Expected count of configured methods (1) in error message, got: $err",
      err.contains("1")
    )
    assertTrue(
      "Expected configured methods listed (CARD) in error, got: $err",
      err.contains("CARD")
    )
    assertFalse(viewModel.state.value.isLoading)
    verify(exactly = 1) { errorHandler.postError(errorMessage = any<UiText>()) }
  }

  @Test fun `SubmitGooglePay with null paymentMethodType AND MULTIPLE allowed methods → ERR (Web SDK strict-match parity)`() = runTest {
    stubSessionResponseWithGooglePay(
      allowedMethods = listOf(allowMethod("CARDS", "CARD"), allowMethod("PAYPAL", "PAYPAL"))
    )
    viewModel.dispatch(
      ActionIntent.SubmitGooglePay(
        paymentDataJson = "{\"paymentMethodData\":{}}",
        paymentMethodType = null
      )
    )
    advanceUntilIdle()

    val err = viewModel.state.value.errorMessage
    assertNotNull("Expected missing-type resolution error, got null", err)
    assertTrue(
      "Expected message to mention 'missing' type, got: $err",
      err!!.contains("missing")
    )
    assertTrue(
      "Expected message to reference configured method count 2, got: $err",
      err.contains("2")
    )
    assertTrue(
      "Expected message to list configured methods CARD and PAYPAL, got: $err",
      err.contains("CARD") && err.contains("PAYPAL")
    )
    assertFalse(viewModel.state.value.isLoading)
    verify(exactly = 1) { errorHandler.postError(errorMessage = any<UiText>()) }
  }

  @Test fun `SubmitGooglePay with blank or whitespace paymentMethodType AND MULTIPLE allowed → ERR (same missing-type path as null, Web SDK parity)`() = runTest {
    stubSessionResponseWithGooglePay(
      allowedMethods = listOf(allowMethod("CARDS", "CARD"), allowMethod("PAYPAL", "PAYPAL"))
    )
    listOf("", "   ", "\t\n").forEach { blank ->
      viewModel.dispatch(
        ActionIntent.SubmitGooglePay(
          paymentDataJson = "{\"paymentMethodData\":{}}",
          paymentMethodType = blank
        )
      )
      advanceUntilIdle()

      val err = viewModel.state.value.errorMessage
      assertNotNull("Expected missing-type error for blank='$blank', got null", err)
      assertTrue(
        "blank='$blank' should contain 'missing' keyword, got: $err",
        err!!.contains("missing")
      )
      assertTrue(
        "blank='$blank' should reference method count 2, got: $err",
        err.contains("2")
      )
      assertFalse(viewModel.state.value.isLoading)
    }
  }

  // ── SubmitGooglePay: non-CARDS wallet (PAYPAL) enters polling WITHOUT submitPaymentInternal ──

  @Test fun `SubmitGooglePay with PAYPAL (non-CARDS) returns empty channelProperties → skips submitPaymentInternal, starts polling`() = runTest {
    coEvery {
      repository.pollSession(sessionId = "auth-key-123", tokenRequestId = any(), any())
    } returns Response.success(
      PollResponse(session = null, paymentRequest = null, paymentToken = null, succeededChannel = null)
    )

    stubSessionResponseWithGooglePay(
      allowedMethods = listOf(allowMethod("CARDS", "CARD"), allowMethod("PAYPAL", "PAYPAL")),
      sessionAuthKey = "auth-key-123",
      publicKey = "pk-123"
    )

    viewModel.dispatch(
      ActionIntent.SubmitGooglePay(
        paymentDataJson = """{"paymentMethodData":{"type":"PAYPAL"}}""",
        paymentMethodType = "PAYPAL"
      )
    )

    runCurrent()

    coVerify(exactly = 1) {
      repository.pollSession(sessionId = "auth-key-123", tokenRequestId = null, any())
    }
    coVerify(exactly = 0) { repository.createPaymentRequest(any(), any()) }
    coVerify(exactly = 0) { repository.createPaymentToken(any(), any()) }

    assertNull(viewModel.state.value.errorMessage)
    assertFalse(viewModel.state.value.isLoading)

    viewModel.wipeAllSensitiveData()
    runCurrent()
  }
}
