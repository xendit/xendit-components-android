package co.xendit.paymentsdk.ui.helper

import co.xendit.paymentsdk.data.model.InstallmentPlan
import co.xendit.paymentsdk.ui.components.molecule.UiText
import java.text.NumberFormat
import java.util.Locale

internal fun InstallmentPlan.toLabelDisplay(): UiText {
  val formattedAmount = NumberFormat.getNumberInstance(Locale("id", "ID")).format(this.installmentAmount)
  return when (this.terms) {
    0 -> UiText.DynamicString("Pay in Full - Rp${formattedAmount}")
    else -> UiText.DynamicString("${this.terms}x Installments - Rp${formattedAmount}")
  }
}