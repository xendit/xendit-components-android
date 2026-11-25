package co.xendit.components.core.network.cookie

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttp
import okhttp3.internal.publicsuffix.PublicSuffixDatabase
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.buffer
import okio.source

internal class InMemoryCookieJar(
  private val clock: () -> Long = System::currentTimeMillis,
) : CookieJar {

  init {
    PublicSuffixDatabaseCompat.ensureInitialized()
  }

  private data class CookieKey(
    val name: String,
    val domain: String,
    val path: String,
  )

  private val cookieStore = LinkedHashMap<CookieKey, Cookie>()

  @Synchronized
  override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
    val now = clock()
    for (cookie in cookies) {
      if (cookie.name.isEmpty()) continue
      if (!isValidCookiePrefix(cookie, url)) continue

      val domainAndSchemeCookie =
        if (cookie.path == "/") cookie else cookie.newBuilder().path("/").build()
      if (!domainAndSchemeCookie.matches(url)) continue

      val key = CookieKey(
        name = cookie.name,
        domain = cookie.domain,
        path = cookie.path,
      )
      val existing = cookieStore[key]
      if (existing != null && existing.expiresAt <= now) {
        cookieStore.remove(key)
      }

      if (!url.isHttps && hasActiveSecureCookieForHost(cookie.name, url, now)) {
        continue
      }

      if (cookie.expiresAt <= now) {
        cookieStore.remove(key)
      } else {
        cookieStore[key] = cookie
      }
    }
  }

  @Synchronized
  override fun loadForRequest(url: HttpUrl): List<Cookie> {
    val now = clock()
    val validCookies = mutableListOf<Cookie>()
    val iterator = cookieStore.entries.iterator()
    while (iterator.hasNext()) {
      val cookie = iterator.next().value
      if (cookie.expiresAt <= now) {
        iterator.remove()
      } else if (cookie.matches(url)) {
        validCookies.add(cookie)
      }
    }
    return validCookies.sortedByDescending { it.path.length }
  }

  @Synchronized
  fun clear() {
    cookieStore.clear()
  }

  private fun isValidCookiePrefix(cookie: Cookie, url: HttpUrl): Boolean {
    if (cookie.name.startsWith("__Secure-")) {
      return cookie.secure && url.isHttps
    }
    if (cookie.name.startsWith("__Host-")) {
      return cookie.secure && url.isHttps && cookie.hostOnly && cookie.path == "/"
    }
    return true
  }

  private fun hasActiveSecureCookieForHost(name: String, url: HttpUrl, now: Long): Boolean {
    val httpsCheckUrl = url.newBuilder().scheme("https").encodedPath("/").build()
    for (existing in cookieStore.values) {
      if (existing.name != name || !existing.secure || existing.expiresAt <= now) continue
      val existingDomainCheck =
        if (existing.path == "/") existing else existing.newBuilder().path("/").build()
      if (existingDomainCheck.matches(httpsCheckUrl)) {
        return true
      }
    }
    return false
  }
}

internal object PublicSuffixDatabaseCompat {

  @Volatile private var hasLoadedFromAssets = false

  private val FALLBACK_PUBLIC_SUFFIX_BYTES: ByteString by lazy {
    listOf(
      "ac.id",
      "biz.id",
      "co",
      "co.id",
      "co.jp",
      "co.th",
      "co.uk",
      "com",
      "com.au",
      "com.my",
      "com.ph",
      "com.sg",
      "com.vn",
      "dev",
      "edu.my",
      "edu.ph",
      "edu.sg",
      "edu.vn",
      "go.id",
      "go.th",
      "gov.my",
      "gov.ph",
      "gov.sg",
      "gov.vn",
      "id",
      "in.th",
      "io",
      "my",
      "my.id",
      "net",
      "net.id",
      "net.my",
      "net.ph",
      "net.sg",
      "net.th",
      "net.vn",
      "or.id",
      "or.th",
      "org",
      "org.my",
      "org.ph",
      "org.sg",
      "org.vn",
      "ph",
      "sch.id",
      "sg",
      "th",
      "vn",
      "web.id",
    )
      .sorted()
      .joinToString(separator = "\n", postfix = "\n")
      .encodeUtf8()
  }

  @Synchronized
  fun ensureInitialized(context: Context? = null) {
    if (context != null && !hasLoadedFromAssets) {
      val assetBytes = readPublicSuffixAssetOrNull(context)
      if (assetBytes != null) {
        runCatching { OkHttp.initialize(context) }
        isDatabaseReady()
        injectPublicSuffixBytes(assetBytes.first, assetBytes.second)
        hasLoadedFromAssets = true
        return
      }
    }

    if (!isDatabaseReady()) {
      injectPublicSuffixBytes(FALLBACK_PUBLIC_SUFFIX_BYTES, ByteString.EMPTY)
    }
  }

  private fun isDatabaseReady(): Boolean {
    return runCatching {
      PublicSuffixDatabase.get().getEffectiveTldPlusOne("xendit.co") == "xendit.co"
    }.getOrDefault(false)
  }

  private fun readPublicSuffixAssetOrNull(context: Context): Pair<ByteString, ByteString>? {
    return runCatching {
      val assets = context.assets ?: return null
      assets.open("PublicSuffixDatabase.list").source().buffer().use { bufferedSource ->
        val totalBytes = bufferedSource.readInt()
        if (totalBytes <= 0) return null
        val bytes = bufferedSource.readByteString(totalBytes.toLong())
        val totalExceptionBytes = bufferedSource.readInt()
        if (totalExceptionBytes < 0) return null
        val exceptionBytes = bufferedSource.readByteString(totalExceptionBytes.toLong())
        bytes to exceptionBytes
      }
    }.getOrNull()
  }

  private fun injectPublicSuffixBytes(bytes: ByteString, exceptionBytes: ByteString) {
    runCatching {
      val database = PublicSuffixDatabase.get()
      val listField = PublicSuffixDatabase::class.java.getDeclaredField("publicSuffixList")
      listField.isAccessible = true
      val list = listField.get(database) ?: return
      synchronized(list) {
        list.javaClass.getMethod("setBytes", ByteString::class.java).invoke(list, bytes)
        list.javaClass
          .getMethod("setExceptionBytes", ByteString::class.java)
          .invoke(list, exceptionBytes)
      }
    }
  }
}

