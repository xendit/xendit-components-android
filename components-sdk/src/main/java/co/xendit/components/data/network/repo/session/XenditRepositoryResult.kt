package co.xendit.components.data.network.repo.session

import co.xendit.components.core.model.APIError
import co.xendit.components.core.model.asApiError
import retrofit2.Response

internal sealed interface XenditRepositoryResult<out T> {
  data class Success<T>(val data: T) : XenditRepositoryResult<T>

  data class Failure(
    val statusCode: Int,
    val apiError: APIError? = null
  ) : XenditRepositoryResult<Nothing> {
    val errorCode: String?
      get() = apiError?.errorCode

    val message: String?
      get() = apiError?.message
  }

  data class EmptyBody(val statusCode: Int) : XenditRepositoryResult<Nothing>
}

internal fun <T> Response<T>.toXenditRepositoryResult(): XenditRepositoryResult<T> {
  return if (isSuccessful) {
    val body = body()
    if (body != null) {
      XenditRepositoryResult.Success(body)
    } else {
      XenditRepositoryResult.EmptyBody(code())
    }
  } else {
    XenditRepositoryResult.Failure(
      statusCode = code(),
      apiError = errorBody()?.asApiError()
    )
  }
}
