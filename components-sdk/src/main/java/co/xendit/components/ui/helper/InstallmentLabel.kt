package co.xendit.components.ui.helper

import android.content.Context
import co.xendit.components.R
import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.ui.components.molecule.UiText
import java.text.NumberFormat
import java.util.Locale

internal fun InstallmentPlan.toLabelDisplay(context: Context, currency: String?): UiText {
  val symbol = when (currency) {
    "IDR" -> "Rp"
    "USD" -> "$"
    "PHP" -> "₱"
    else -> currency
  }
  val formattedAmount = symbol + NumberFormat.getNumberInstance(Locale("id", "ID"))
    .format(this.installmentAmount)
  return when (this.terms) {
    0 -> {
      val rawString = context.getString(R.string.sessioninstallment_plan_pay_in_full)
      val formattedString = rawString
        .replace("{{amount}}", formattedAmount)
      UiText.DynamicString(formattedString)
    }

    else -> {
      val rawString = context.getString(R.string.sessioninstallment_plan_pay_in_installments)
      val formattedString = rawString
        .replace("{{installments}}", this.terms.toString())
        .replace("{{amount}}", formattedAmount)
      UiText.DynamicString(formattedString)
    }
  }
}