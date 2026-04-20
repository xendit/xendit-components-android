package co.xendit.paymentsdk.core.network.interceptor

import co.xendit.paymentsdk.core.model.APIError
import co.xendit.paymentsdk.core.model.GlobalErrorHandler
import co.xendit.paymentsdk.core.model.asApiError
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

// Note: This is no longer a simple class, it needs its dependency.
internal class ErrorInterceptor(private val globalErrorHandler: GlobalErrorHandler) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    try {
      val request = chain.request()
      val response = chain.proceed(chain.request())
      val handleManually = request.header("HANDLE_ERROR_MANUALLY_HEADER") == "true"
      if (!response.isSuccessful && !handleManually) {
        val errorBodyString = response.peekBody(Long.MAX_VALUE).string()

        // Now parse the safe string copy of the body.
        val apiError = errorBodyString.asApiError()
        val errorMessage = globalErrorHandler.getErrorMessageFromApiError(apiError.message)

        // Post the error message to the UI.
        runBlocking {
          globalErrorHandler.postError(
            errorCode = apiError.message,
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
      // Return a default error if parsing the string fails
      APIError("0", "Failed to parse error message")
    }
  }
}
