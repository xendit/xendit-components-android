package co.xendit.paymentsdk.core.model

import android.content.Context
import co.xendit.paymentsdk.ui.components.molecule.UiText
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class GlobalErrorHandler(
  val context: Context,
) {
  private val _apiErrorFlow = MutableSharedFlow<Pair<String?, UiText?>>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val apiErrorFlow = _apiErrorFlow.asSharedFlow()

  fun postError(errorCode: String? = null, errorMessage: UiText) {
    _apiErrorFlow.tryEmit(Pair(errorCode, errorMessage))
  }

  fun getErrorMessageFromApiError(apiError: String): UiText {
    return when (apiError) {
      else -> UiText.DynamicString(apiError)
    }
  }
}
