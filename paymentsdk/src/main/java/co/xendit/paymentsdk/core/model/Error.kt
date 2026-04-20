package co.xendit.paymentsdk.core.model

import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody

internal fun ResponseBody.asApiError(gson: Gson = Gson()): APIError {
  return try {
    gson.fromJson(string(), APIError::class.java)
  } catch (e: Exception) {
    return APIError("0", "")
  }
}

@Keep
internal data class APIError(
  @SerializedName("error_code")
  val errorCode: String,
  @SerializedName("message")
  var message: String = "",
  @SerializedName("error_content")
  val errorContent: ErrorContent? = null
)

@Keep
internal data class ErrorContent(
  @SerializedName("title")
  val title: String,
  @SerializedName("message_1")
  val message1: String,
  @SerializedName("message_2")
  val message2: String
)