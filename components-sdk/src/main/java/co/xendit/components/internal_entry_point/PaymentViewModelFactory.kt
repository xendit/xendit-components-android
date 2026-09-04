package co.xendit.components.internal_entry_point

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import co.xendit.components.core.CoreSdkComponent.globalErrorHandler
import co.xendit.components.core.TelemetrySdkComponent.sessionTelemetry
import co.xendit.components.data.DataSdkComponent.xenditRepository
import co.xendit.components.ui.PaymentViewModel

internal class PaymentViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    co.xendit.components.core.CoreSdkComponent.init(context)
    val repository = xenditRepository
    val errorHandler = globalErrorHandler
    val telemetry = sessionTelemetry
    return PaymentViewModel(
      xenditRepository = repository,
      globalErrorHandler = errorHandler,
      telemetry = telemetry,
    ) as T

  }
}
