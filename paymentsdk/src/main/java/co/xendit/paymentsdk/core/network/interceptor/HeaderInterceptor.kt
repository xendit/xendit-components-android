package co.xendit.paymentsdk.core.network.interceptor

import co.xendit.paymentsdk.core.network.provider.HeaderProvider
import co.xendit.paymentsdk.core.network.provider.SecretProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.nio.charset.StandardCharsets

class HeaderInterceptor(
  private val secretProvider: SecretProvider,
  private val provider: HeaderProvider,
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val requestBuilder = chain.request().newBuilder()

    requestBuilder.header("Content-Type", "application/json")
    requestBuilder.header("Origin", provider.getOrigin())

    try {
      return chain.proceed(requestBuilder.build())
    } catch (e: Exception) {
      throw e
    }
  }

  /** Safely reads the request body into a string without consuming it. */
  private fun bodyToString(request: Request): String {
    return try {
      val buffer = Buffer()
      request.body?.writeTo(buffer)
      buffer.readString(StandardCharsets.UTF_8)
    } catch (e: Exception) {
      "" // Return empty string if body is absent or can't be read
    }
  }
}
