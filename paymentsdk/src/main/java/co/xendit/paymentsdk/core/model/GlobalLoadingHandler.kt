package co.xendit.paymentsdk.core.model

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class GlobalLoadingHandler(
  val context: Context,
) {

  private val _apiLoadingFlow = MutableSharedFlow<Boolean>()
  val apiLoadingFlow = _apiLoadingFlow.asSharedFlow()

  suspend fun setLoading() {
    _apiLoadingFlow.emit(true)
  }

  suspend fun stopLoading() {
    _apiLoadingFlow.emit(false)
  }
}
