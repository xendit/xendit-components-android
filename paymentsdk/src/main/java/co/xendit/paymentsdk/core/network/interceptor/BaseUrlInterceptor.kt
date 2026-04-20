package co.xendit.paymentsdk.core.network.interceptor

import okhttp3.HttpUrl
import okhttp3.Interceptor

internal class BaseUrlInterceptor(
  private val baseUrlProvider: () -> HttpUrl
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
    val request = chain.request()
    val originalUrl = request.url
    val baseUrl = baseUrlProvider()

    val rewrittenUrl =
      originalUrl.newBuilder()
        .scheme(baseUrl.scheme)
        .host(baseUrl.host)
        .port(baseUrl.port)
        .build()

    val rewrittenRequest = request.newBuilder().url(rewrittenUrl).build()
    return chain.proceed(rewrittenRequest)
  }
}