package co.xendit.paymentsdk.core.model

import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody

fun ResponseBody.asApiError(gson: Gson = Gson()): APIError {
  return try {
    gson.fromJson(string(), APIError::class.java)
  } catch (e: Exception) {
    return APIError("0", "")
  }
}

@Keep
data class APIError(
  @SerializedName("error_code")
  val errorCode: String,
  @SerializedName("message")
  var message: String = "",
  @SerializedName("error_content")
  val errorContent: ErrorContent? = null
) {
//  fun isHandledError(): Boolean {
//    return listOf("4000003", "4000004", "4000005").contains(errorCode)
//  }
}

@Keep
data class ErrorContent(
  @SerializedName("title")
  val title: String,
  @SerializedName("message_1")
  val message1: String,
  @SerializedName("message_2")
  val message2: String
)