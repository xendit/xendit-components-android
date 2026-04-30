package co.xendit.components.ui.helper

import java.text.NumberFormat
import java.util.Locale

internal object CurrencyUtil {
  fun formatAmount(amount: Long?, currency: String?): String {
    if (amount == null || currency.isNullOrBlank()) return ""
    val symbol =
      when (currency) {
        "IDR" -> "Rp"
        "USD" -> "$"
        "PHP" -> "₱"
        else -> currency
      }
    val number = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("id").setRegion("ID").build())
    return "$symbol${number.format(amount)}"
  }
}

