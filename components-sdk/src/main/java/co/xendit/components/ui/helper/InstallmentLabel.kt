package co.xendit.components.ui.helper

import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.ui.components.molecule.UiText

internal fun InstallmentPlan.toLabelDisplay(): UiText {
  val formattedAmount = CurrencyUtil.formatAmount(this.installmentAmount, "IDR")
  return when (this.terms) {
    0 -> UiText.DynamicString("Pay in Full - $formattedAmount")
    else -> UiText.DynamicString("${this.terms}x Installments - $formattedAmount")
  }
}
