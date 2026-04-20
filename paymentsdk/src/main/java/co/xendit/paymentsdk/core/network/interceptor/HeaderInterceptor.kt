package co.xendit.paymentsdk.core.network.interceptor

import co.xendit.paymentsdk.core.network.provider.HeaderProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.nio.charset.StandardCharsets

internal class HeaderInterceptor(
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
}
