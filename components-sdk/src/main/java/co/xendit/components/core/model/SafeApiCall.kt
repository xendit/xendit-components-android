package co.xendit.components.core.model

import co.xendit.components.R
import co.xendit.components.util.XLogger
import co.xendit.components.ui.components.molecule.UiText
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private const val TAG = "SafeApiCall"
internal class SafeApiCall(
  private val loadingHandler: GlobalLoadingHandler,
  private val globalErrorHandler: GlobalErrorHandler
) {
  suspend fun <T> call(apiCall: suspend () -> Response<T>): Response<T> {
    loadingHandler.setLoading()
    try {
      // We only expect a successful response in the try block now.
      // If the response is not 2xx, Retrofit will throw an HttpException.
      return apiCall()
    } catch (e: HttpException) {
      // This is the most important block. It catches API errors (4xx, 5xx).
      // The HttpException contains the original response with the error body.
      XLogger.e("safeApiCall caught HttpException for code: ${e.code()}")
      val errorBody = e.response()?.errorBody()?.string() ?: "{}"
      return Response.error(e.code(), errorBody.toResponseBody(null))
    } catch (e: IOException) {
      // Handles network failures (timeouts, no internet)
      XLogger.e("safeApiCall caught IOException: ${e.message}")
      globalErrorHandler.postError(
        errorCode = "NETWORK_ERROR",
        errorMessage = UiText.StringResource(R.string.sessionnetwork_error_subtext)
      )
      return Response.error(
        503,
        "{\"error_code\":\"NETWORK_ERROR\",\"message\":\"Network error. Please check your connection.\"}"
          .toResponseBody(null)
      )
    } catch (e: JsonSyntaxException) {
      // This is a data mismatch error, not a server error.
      XLogger.e("safeApiCall caught JsonSyntaxException: ${e.message}")
      globalErrorHandler.postError(
        errorCode = "PARSE_ERROR",
        errorMessage = UiText.DynamicString("Failed to parse server response.")
      )
      return Response.error(
        // Use a custom error code or 500, but log it differently.
        500,
        "{\"error_code\":\"PARSE_ERROR\",\"message\":\"Failed to parse server response.\"}"
          .toResponseBody(null)
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // Handles any other unexpected crashes (like JSON parsing on a 200 response)
      XLogger.e("safeApiCall caught unexpected Exception", e)
      globalErrorHandler.postError(
        errorCode = "UNEXPECTED_ERROR",
        errorMessage = UiText.DynamicString("An unexpected error occurred: ${e.message}")
      )
      return Response.error(
        500,
        "{\"error_code\":\"UNEXPECTED_ERROR\",\"message\":\"An unexpected error occurred: ${e.message}\"}"
          .toResponseBody(null)
      )
    } finally {
      loadingHandler.stopLoading()
    }
  }
}
