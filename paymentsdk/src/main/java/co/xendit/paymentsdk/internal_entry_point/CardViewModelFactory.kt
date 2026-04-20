package co.xendit.paymentsdk.internal_entry_point

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import co.xendit.paymentsdk.data.DataSdkComponent.xenditRepository
import co.xendit.paymentsdk.ui.card.CardViewModel

internal class CardViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    co.xendit.paymentsdk.core.CoreSdkComponent.init(context)
    val repository = xenditRepository
    return CardViewModel(repository) as T
  }
}

