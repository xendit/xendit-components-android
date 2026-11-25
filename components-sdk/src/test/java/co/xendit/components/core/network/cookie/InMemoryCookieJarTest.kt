package co.xendit.components.core.network.cookie

import android.content.Context
import android.content.res.AssetManager
import co.xendit.components.core.CoreSdkComponent
import io.mockk.every
import io.mockk.mockk
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.internal.http.BridgeInterceptor
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryCookieJarTest {

  private var currentTimeMillis = 1_700_000_000_000L
  private lateinit var cookieJar: InMemoryCookieJar

  @Before
  fun setUp() {
    currentTimeMillis = 1_700_000_000_000L
    cookieJar = InMemoryCookieJar(clock = { currentTimeMillis })
  }

  @After
  fun tearDown() {
    CoreSdkComponent.cookieJar.clear()
    CoreSdkComponent.setBaseUrl("https://checkout-ui-gateway.xendit.co")
  }

  @Test
  fun `saveFromResponse and loadForRequest persists cookies for checkout-ui-gateway`() {
    val sessionUrl = "https://checkout-ui-gateway.xendit.co/api/sessions/sess-123".toHttpUrl()
    val paymentRequestUrl =
      "https://checkout-ui-gateway.xendit.co/api/sessions/payment_requests".toHttpUrl()

    val cookie =
      checkNotNull(
        Cookie.parse(
          sessionUrl,
          "cug_session=abc123; Path=/; Secure; HttpOnly"
        )
      )

    cookieJar.saveFromResponse(sessionUrl, listOf(cookie))

    val loaded = cookieJar.loadForRequest(paymentRequestUrl)
    assertEquals(1, loaded.size)
    assertEquals("cug_session", loaded[0].name)
    assertEquals("abc123", loaded[0].value)
  }

  @Test
  fun `saveFromResponse replaces existing cookie with same name domain and path`() {
    val url = "https://checkout-ui-gateway.xendit.co/api/sessions/sess-123".toHttpUrl()
    val first = checkNotNull(Cookie.parse(url, "cug_session=v1; Path=/; Secure"))
    val second = checkNotNull(Cookie.parse(url, "cug_session=v2; Path=/; Secure"))

    cookieJar.saveFromResponse(url, listOf(first))
    cookieJar.saveFromResponse(url, listOf(second))

    val loaded = cookieJar.loadForRequest(url)
    assertEquals(1, loaded.size)
    assertEquals("v2", loaded[0].value)
  }

  @Test
  fun `saveFromResponse with expired or Max-Age=0 cookie deletes existing cookie`() {
    val url = "https://checkout-ui-gateway.xendit.co/api/sessions/sess-123".toHttpUrl()
    val activeCookie =
      Cookie.Builder()
        .name("cug_session")
        .value("active")
        .hostOnlyDomain("checkout-ui-gateway.xendit.co")
        .path("/")
        .expiresAt(currentTimeMillis + 60_000L)
        .secure()
        .build()

    val deletionCookie =
      Cookie.Builder()
        .name("cug_session")
        .value("")
        .hostOnlyDomain("checkout-ui-gateway.xendit.co")
        .path("/")
        .expiresAt(currentTimeMillis)
        .secure()
        .build()

    cookieJar.saveFromResponse(url, listOf(activeCookie))
    assertEquals(1, cookieJar.loadForRequest(url).size)

    cookieJar.saveFromResponse(url, listOf(deletionCookie))
    assertTrue(cookieJar.loadForRequest(url).isEmpty())
  }

  @Test
  fun `loadForRequest evicts cookies that have expired`() {
    val url = "https://checkout-ui-gateway.xendit.co/api/sessions/sess-123".toHttpUrl()
    val shortLivedCookie =
      Cookie.Builder()
        .name("short_lived")
        .value("1")
        .hostOnlyDomain("checkout-ui-gateway.xendit.co")
        .path("/")
        .expiresAt(currentTimeMillis + 5_000L)
        .build()
    val longLivedCookie =
      Cookie.Builder()
        .name("long_lived")
        .value("2")
        .hostOnlyDomain("checkout-ui-gateway.xendit.co")
        .path("/")
        .expiresAt(currentTimeMillis + 60_000L)
        .build()

    cookieJar.saveFromResponse(url, listOf(shortLivedCookie, longLivedCookie))
    assertEquals(2, cookieJar.loadForRequest(url).size)

    currentTimeMillis += 5_000L
    val remaining = cookieJar.loadForRequest(url)
    assertEquals(1, remaining.size)
    assertEquals("long_lived", remaining[0].name)
  }

  @Test
  fun `isolates host-only cookies across different checkout-ui-gateway environments`() {
    val prodLiveUrl = "https://checkout-ui-gateway.xendit.co/api/sessions/sess-1".toHttpUrl()
    val prodDevUrl = "https://checkout-ui-gateway-prod-dev.xendit.co/api/sessions/sess-1".toHttpUrl()
    val stgLiveUrl = "https://checkout-ui-gateway-live.stg.tidnex.dev/api/sessions/sess-1".toHttpUrl()

    val prodLiveCookie = checkNotNull(Cookie.parse(prodLiveUrl, "env=pl; Path=/; Secure"))
    val stgLiveCookie = checkNotNull(Cookie.parse(stgLiveUrl, "env=sl; Path=/; Secure"))

    cookieJar.saveFromResponse(prodLiveUrl, listOf(prodLiveCookie))
    cookieJar.saveFromResponse(stgLiveUrl, listOf(stgLiveCookie))

    assertEquals(listOf("pl"), cookieJar.loadForRequest(prodLiveUrl).map { it.value })
    assertTrue(cookieJar.loadForRequest(prodDevUrl).isEmpty())
    assertEquals(listOf("sl"), cookieJar.loadForRequest(stgLiveUrl).map { it.value })
  }

  @Test
  fun `rejects cookies for mismatched domains and insecure origins setting Secure cookies`() {
    val gatewayUrl = "https://checkout-ui-gateway.xendit.co/api/sessions/sess-1".toHttpUrl()
    val mismatchedDomainCookie =
      Cookie.Builder()
        .name("foreign")
        .value("evil")
        .domain("attacker.example.com")
        .path("/")
        .build()

    cookieJar.saveFromResponse(gatewayUrl, listOf(mismatchedDomainCookie))
    assertTrue(cookieJar.loadForRequest(gatewayUrl).isEmpty())
    assertTrue(
      cookieJar.loadForRequest("https://attacker.example.com/".toHttpUrl()).isEmpty()
    )

    val httpGatewayUrl = "http://checkout-ui-gateway.xendit.co/api/sessions/sess-1".toHttpUrl()
    val secureCookieOverHttp =
      Cookie.Builder()
        .name("sec")
        .value("val")
        .hostOnlyDomain("checkout-ui-gateway.xendit.co")
        .path("/")
        .secure()
        .build()

    cookieJar.saveFromResponse(httpGatewayUrl, listOf(secureCookieOverHttp))
    assertTrue(cookieJar.loadForRequest(gatewayUrl).isEmpty())

    // Existing Secure cookie must not be overwritten or deleted by an insecure http:// response
    val httpsSecureCookie =
      Cookie.Builder()
        .name("cug_sec")
        .value("safe")
        .hostOnlyDomain("checkout-ui-gateway.xendit.co")
        .path("/")
        .secure()
        .build()
    cookieJar.saveFromResponse(gatewayUrl, listOf(httpsSecureCookie))

    val httpOverwriteAttempt =
      Cookie.Builder()
        .name("cug_sec")
        .value("tampered")
        .hostOnlyDomain("checkout-ui-gateway.xendit.co")
        .path("/")
        .build()
    val httpDeleteAttempt =
      Cookie.Builder()
        .name("cug_sec")
        .value("")
        .hostOnlyDomain("checkout-ui-gateway.xendit.co")
        .path("/")
        .expiresAt(currentTimeMillis - 1)
        .build()

    cookieJar.saveFromResponse(httpGatewayUrl, listOf(httpOverwriteAttempt))
    cookieJar.saveFromResponse(httpGatewayUrl, listOf(httpDeleteAttempt))

    val loaded = cookieJar.loadForRequest(gatewayUrl)
    assertEquals(1, loaded.size)
    assertEquals("safe", loaded[0].value)
  }

  @Test
  fun `domain cookie on parent domain parses via Cookie parse and matches gateway subdomains on same parent domain only`() {
    val prodLiveUrl = "https://checkout-ui-gateway.xendit.co/api/sessions/sess-1".toHttpUrl()
    val prodDevUrl = "https://checkout-ui-gateway-prod-dev.xendit.co/api/sessions/sess-1".toHttpUrl()
    val stgLiveUrl = "https://checkout-ui-gateway-live.stg.tidnex.dev/api/sessions/sess-1".toHttpUrl()
    val stgDevUrl = "https://checkout-ui-gateway-dev.stg.tidnex.dev/api/sessions/sess-1".toHttpUrl()

    val parentDomainCookie =
      checkNotNull(
        Cookie.parse(
          prodLiveUrl,
          "xendit_shared=shared123; Domain=.xendit.co; Path=/; Secure; HttpOnly"
        )
      )
    val stgDomainCookie =
      checkNotNull(
        Cookie.parse(
          stgLiveUrl,
          "stg_shared=stg456; Domain=.stg.tidnex.dev; Path=/; Secure"
        )
      )
    cookieJar.saveFromResponse(prodLiveUrl, listOf(parentDomainCookie))
    cookieJar.saveFromResponse(stgLiveUrl, listOf(stgDomainCookie))

    assertEquals(listOf("shared123"), cookieJar.loadForRequest(prodLiveUrl).map { it.value })
    assertEquals(listOf("shared123"), cookieJar.loadForRequest(prodDevUrl).map { it.value })
    assertEquals(listOf("stg456"), cookieJar.loadForRequest(stgLiveUrl).map { it.value })
    assertEquals(listOf("stg456"), cookieJar.loadForRequest(stgDevUrl).map { it.value })

    // Public suffix Domain attributes must be rejected by Cookie.parse
    assertNull(Cookie.parse(prodLiveUrl, "evil=1; Domain=.co; Path=/; Secure"))
    assertNull(Cookie.parse(stgLiveUrl, "evil=1; Domain=.dev; Path=/; Secure"))
    assertNull(
      Cookie.parse(
        "https://checkout-ui-gateway.xendit.co.id/api/sessions/sess-1".toHttpUrl(),
        "evil=1; Domain=.co.id; Path=/; Secure"
      )
    )
  }

  @Test
  fun `saveFromResponse accepts cookies for different path on same host and prevents cross-domain or sub-path Secure shadowing`() {
    val bootstrapUrl = "https://checkout-ui-gateway.xendit.co/v1/bootstrap".toHttpUrl()
    val httpsUrl = "https://checkout-ui-gateway.xendit.co/api/sessions/sess-1".toHttpUrl()
    val httpUrl = "http://checkout-ui-gateway.xendit.co/api/sessions/sess-1".toHttpUrl()

    // Cookie with Path=/api/sessions set from /v1/bootstrap (valid per RFC 6265 Section 5.3)
    val crossPathCookie =
      checkNotNull(
        Cookie.parse(bootstrapUrl, "cross_path=ok; Path=/api/sessions; Secure")
      )
    cookieJar.saveFromResponse(bootstrapUrl, listOf(crossPathCookie))
    assertEquals(
      listOf("ok"),
      cookieJar.loadForRequest(
        "https://checkout-ui-gateway.xendit.co/api/sessions/poll".toHttpUrl()
      ).map { it.value }
    )

    // Active Secure cookie on checkout-ui-gateway.xendit.co cannot be shadowed by http:// using Domain=.xendit.co or sub-path
    val activeSecureCookie =
      Cookie.Builder()
        .name("cug_sec")
        .value("safe")
        .hostOnlyDomain("checkout-ui-gateway.xendit.co")
        .path("/")
        .expiresAt(currentTimeMillis + 10_000L)
        .secure()
        .build()
    cookieJar.saveFromResponse(httpsUrl, listOf(activeSecureCookie))

    val httpParentDomainShadow =
      Cookie.Builder()
        .name("cug_sec")
        .value("shadow_domain")
        .domain("xendit.co")
        .path("/")
        .build()
    val httpSubPathShadow =
      Cookie.Builder()
        .name("cug_sec")
        .value("shadow_path")
        .hostOnlyDomain("checkout-ui-gateway.xendit.co")
        .path("/api/sessions")
        .build()
    cookieJar.saveFromResponse(httpUrl, listOf(httpParentDomainShadow, httpSubPathShadow))
    assertEquals(
      listOf("safe"),
      cookieJar.loadForRequest(httpsUrl).filter { it.name == "cug_sec" }.map { it.value }
    )

    // Once the Secure cookie expires, it no longer blocks new cookies
    currentTimeMillis += 15_000L
    val postExpiryCookie =
      Cookie.Builder()
        .name("cug_sec")
        .value("after_expiry")
        .hostOnlyDomain("checkout-ui-gateway.xendit.co")
        .path("/")
        .expiresAt(currentTimeMillis + 10_000L)
        .build()
    cookieJar.saveFromResponse(httpUrl, listOf(postExpiryCookie))
    assertEquals(
      listOf("after_expiry"),
      cookieJar.loadForRequest(httpsUrl).filter { it.name == "cug_sec" }.map { it.value }
    )
  }

  @Test
  fun `enforces __Secure- and __Host- cookie prefix rules`() {
    val httpsUrl = "https://checkout-ui-gateway.xendit.co/api/sessions/sess-1".toHttpUrl()

    val validHostPrefix =
      checkNotNull(Cookie.parse(httpsUrl, "__Host-sess=ok; Path=/; Secure; HttpOnly"))
    val invalidHostWithDomain =
      checkNotNull(
        Cookie.parse(httpsUrl, "__Host-bad1=no; Domain=.xendit.co; Path=/; Secure")
      )
    val invalidHostWithSubPath =
      checkNotNull(Cookie.parse(httpsUrl, "__Host-bad2=no; Path=/api; Secure"))
    val invalidHostWithoutSecure =
      checkNotNull(Cookie.parse(httpsUrl, "__Host-bad3=no; Path=/"))
    val validSecurePrefix =
      checkNotNull(
        Cookie.parse(httpsUrl, "__Secure-token=ok; Domain=.xendit.co; Path=/api; Secure")
      )
    val invalidSecureWithoutFlag =
      checkNotNull(Cookie.parse(httpsUrl, "__Secure-bad=no; Path=/"))

    cookieJar.saveFromResponse(
      httpsUrl,
      listOf(
        validHostPrefix,
        invalidHostWithDomain,
        invalidHostWithSubPath,
        invalidHostWithoutSecure,
        validSecurePrefix,
        invalidSecureWithoutFlag,
      )
    )

    val loaded = cookieJar.loadForRequest(httpsUrl)
    assertEquals(
      setOf("__Host-sess", "__Secure-token"),
      loaded.map { it.name }.toSet()
    )
  }

  @Test
  fun `CoreSdkComponent init handles null applicationContext and loads PublicSuffixDatabase from assets when available`() {
    val nullAppCtx = mockk<Context>()
    every { nullAppCtx.applicationContext } returns null
    every { nullAppCtx.assets } returns null
    CoreSdkComponent.init(nullAppCtx)

    val rulesBytes = "co\nco.id\ncom\ndev\n".toByteArray(Charsets.UTF_8)
    val excBytes = "www.ck\n".toByteArray(Charsets.UTF_8)
    val assetBuffer =
      Buffer()
        .writeInt(rulesBytes.size)
        .write(rulesBytes)
        .writeInt(excBytes.size)
        .write(excBytes)

    val assetManager = mockk<AssetManager>()
    every { assetManager.open("PublicSuffixDatabase.list") } answers { assetBuffer.clone().inputStream() }
    val ctxWithAssets = mockk<Context>()
    every { ctxWithAssets.applicationContext } returns ctxWithAssets
    every { ctxWithAssets.assets } returns assetManager

    CoreSdkComponent.init(ctxWithAssets)

    val parsed =
      Cookie.parse(
        "https://checkout-ui-gateway.xendit.co/api/sessions/sess-1".toHttpUrl(),
        "__cf_bm=abc; Domain=.xendit.co; Path=/; Secure; HttpOnly"
      )
    assertNotNull(parsed)
    assertEquals("xendit.co", parsed?.domain)
  }

  @Test
  fun `path matching enforces path boundaries`() {
    val setUrl = "https://checkout-ui-gateway.xendit.co/api/sessions/sess-1".toHttpUrl()
    val scopedCookie =
      checkNotNull(Cookie.parse(setUrl, "scoped=1; Path=/api/sessions; Secure"))
    cookieJar.saveFromResponse(setUrl, listOf(scopedCookie))

    assertEquals(
      1,
      cookieJar.loadForRequest(
        "https://checkout-ui-gateway.xendit.co/api/sessions".toHttpUrl()
      ).size
    )
    assertEquals(
      1,
      cookieJar.loadForRequest(
        "https://checkout-ui-gateway.xendit.co/api/sessions/payment_requests".toHttpUrl()
      ).size
    )
    assertTrue(
      cookieJar.loadForRequest(
        "https://checkout-ui-gateway.xendit.co/api/sessions_other".toHttpUrl()
      ).isEmpty()
    )
    assertTrue(
      cookieJar.loadForRequest(
        "https://checkout-ui-gateway.xendit.co/v1/sessions/performance".toHttpUrl()
      ).isEmpty()
    )
  }

  @Test
  fun `orders cookies with longer paths before shorter paths`() {
    val url = "https://checkout-ui-gateway.xendit.co/api/sessions/sess-1/poll".toHttpUrl()
    val rootCookie = checkNotNull(Cookie.parse(url, "token=root; Path=/"))
    val apiSessionsCookie = checkNotNull(Cookie.parse(url, "token=sessions; Path=/api/sessions"))
    val apiCookie = checkNotNull(Cookie.parse(url, "token=api; Path=/api"))

    cookieJar.saveFromResponse(url, listOf(rootCookie, apiSessionsCookie, apiCookie))

    val loaded = cookieJar.loadForRequest(url)
    assertEquals(listOf("sessions", "api", "root"), loaded.map { it.value })
  }

  @Test
  fun `clear removes all cookies`() {
    val url = "https://checkout-ui-gateway.xendit.co/api/sessions/sess-1".toHttpUrl()
    val cookie = checkNotNull(Cookie.parse(url, "cug_session=abc; Path=/"))
    cookieJar.saveFromResponse(url, listOf(cookie))
    assertEquals(1, cookieJar.loadForRequest(url).size)

    cookieJar.clear()
    assertTrue(cookieJar.loadForRequest(url).isEmpty())
  }

  @Test
  fun `CoreSdkComponent okHttpClient has InMemoryCookieJar enabled and sends cookies across requests`() {
    val context = mockk<Context>(relaxed = true)
    every { context.applicationContext } returns context
    CoreSdkComponent.init(context)
    CoreSdkComponent.cookieJar.clear()
    CoreSdkComponent.setBaseUrl("https://checkout-ui-gateway-live.stg.tidnex.dev")

    val client = CoreSdkComponent.okHttpClient
    assertNotEquals(CookieJar.NO_COOKIES, client.cookieJar)
    assertSame(CoreSdkComponent.cookieJar, client.cookieJar)

    val capturedRequestCookies = mutableListOf<String?>()
    val capturedHosts = mutableListOf<String>()

    val testClient =
      client.newBuilder()
        .addInterceptor(BridgeInterceptor(client.cookieJar))
        .addInterceptor(
          Interceptor { chain ->
            val req = chain.request()
            capturedHosts.add(req.url.host)
            capturedRequestCookies.add(req.header("Cookie"))

            val responseBuilder =
              Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))

            if (req.url.encodedPath.endsWith("/api/sessions/sess-123")) {
              responseBuilder.addHeader(
                "Set-Cookie",
                "__cf_bm=cf_token_123; Path=/; Domain=.tidnex.dev; Secure; HttpOnly"
              )
              responseBuilder.addHeader(
                "Set-Cookie",
                "cug_affinity=node_42; Path=/api/sessions; Secure"
              )
            }
            responseBuilder.build()
          }
        )
        .build()

    // 1st request: getSession (rewritten by BaseUrlInterceptor to stg host)
    val getSessionReq =
      Request.Builder()
        .url("https://checkout-ui-gateway.xendit.co/api/sessions/sess-123")
        .get()
        .build()
    testClient.newCall(getSessionReq).execute().close()

    // 2nd request: createPaymentRequest to same checkout-ui-gateway host
    val createPaymentReq =
      Request.Builder()
        .url("https://checkout-ui-gateway.xendit.co/api/sessions/payment_requests")
        .post("{}".toRequestBody("application/json".toMediaType()))
        .build()
    testClient.newCall(createPaymentReq).execute().close()

    assertEquals(
      listOf(
        "checkout-ui-gateway-live.stg.tidnex.dev",
        "checkout-ui-gateway-live.stg.tidnex.dev"
      ),
      capturedHosts
    )
    assertEquals(null, capturedRequestCookies[0])
    assertEquals("cug_affinity=node_42; __cf_bm=cf_token_123", capturedRequestCookies[1])
  }
}
