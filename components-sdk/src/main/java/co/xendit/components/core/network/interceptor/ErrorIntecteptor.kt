package co.xendit.components.core.network.interceptor

import co.xendit.components.core.model.APIError
import co.xendit.components.core.model.GlobalErrorHandler
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

internal class ErrorInterceptor(private val globalErrorHandler: GlobalErrorHandler) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    try {
      val request = chain.request()
      val response = chain.proceed(chain.request())
      val handleManually = request.header("HANDLE_ERROR_MANUALLY_HEADER") == "true"
      if (!response.isSuccessful && !handleManually) {
        val errorBodyString = response.peekBody(Long.MAX_VALUE).string()

        val apiError = errorBodyString.asApiError()
        val bestMessage = apiError.errorContent?.message1 ?: apiError.message
        val errorMessage = globalErrorHandler.getErrorMessageFromApiError(bestMessage)

        runBlocking {
          globalErrorHandler.postError(
            errorCode = apiError.errorCode,
            errorMessage = errorMessage
          )
        }
      }
      return response
    } catch (e: Exception) {
      throw e
    }
  }

  private fun String.asApiError(gson: Gson = Gson()): APIError {
    return try {
      gson.fromJson(this, APIError::class.java)
    } catch (e: Exception) {
      APIError("0", "Failed to parse error message")
    }
  }
}
