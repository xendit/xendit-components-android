package co.xendit.components.core.model

import android.util.Log
import com.google.gson.JsonSyntaxException
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private const val TAG = "SafeApiCall"
internal class SafeApiCall(val loadingHandler: GlobalLoadingHandler) {
  suspend fun <T> call(apiCall: suspend () -> Response<T>): Response<T> {
    loadingHandler.setLoading()
    try {
      // We only expect a successful response in the try block now.
      // If the response is not 2xx, Retrofit will throw an HttpException.
      return apiCall()
    } catch (e: HttpException) {
      // This is the most important block. It catches API errors (4xx, 5xx).
      // The HttpException contains the original response with the error body.
      Log.d(TAG, "safeApiCall caught HttpException for code: ${e.code()}")
      val errorBody = e.response()?.errorBody()?.string() ?: "{}"
      return Response.error(e.code(), errorBody.toResponseBody(null))
    } catch (e: IOException) {
      // Handles network failures (timeouts, no internet)
      Log.d(TAG,"safeApiCall caught IOException: ${e.message}")
      return Response.error(
        503,
        "{\"responseMessage\":\"Network error. Please check your connection.\"}".toResponseBody(
          null
        )
      )
    } catch (e: JsonSyntaxException) {
      // This is a data mismatch error, not a server error.
      Log.d(TAG,"safeApiCall caught JsonSyntaxException: ${e.message}")
      return Response.error(
        // Use a custom error code or 500, but log it differently.
        500,
        "{\"responseMessage\":\"Failed to parse server response.\"}".toResponseBody(null)
      )
    } catch (e: Exception) {
      // Handles any other unexpected crashes (like JSON parsing on a 200 response)
      Log.e(TAG, "safeApiCall caught unexpected Exception", e)
      return Response.error(
        500,
        "{\"responseMessage\":\"An unexpected error occurred: ${e.message}\"}".toResponseBody(
          null
        )
      )
    } finally {
      loadingHandler.stopLoading()
    }
  }
}
