package co.xendit.components.ui.helper

import android.content.Context
import co.xendit.components.R
import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.ui.components.molecule.UiText
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallmentLabelTest {

  @Test
  fun `toLabelDisplay returns Pay in Full for terms 0`() {
    val context = mockk<Context>()
    every { context.getString(R.string.sessioninstallment_plan_pay_in_full) } returns "Pay in full — {{amount}}"

    val plan = InstallmentPlan(
      interval = null,
      intervalCount = null,
      terms = 0,
      installmentAmount = 100000L,
      totalAmount = null,
      description = null,
      interestRate = null
    )
    val result = plan.toLabelDisplay(context = context, currency = "IDR")

    assertTrue(result is UiText.DynamicString)
    assertEquals("Pay in full — Rp100.000", (result as UiText.DynamicString).value)
  }

  @Test
  fun `toLabelDisplay returns installments for terms greater than 0`() {
    val context = mockk<Context>()
    every { context.getString(R.string.sessioninstallment_plan_pay_in_installments) } returns "{{installments}}x Installments — {{amount}}"

    val plan = InstallmentPlan(
      interval = null,
      intervalCount = null,
      terms = 3,
      installmentAmount = 33333L,
      totalAmount = null,
      description = null,
      interestRate = null
    )
    val result = plan.toLabelDisplay(context = context, currency = "IDR")

    assertTrue(result is UiText.DynamicString)
    assertEquals("3x Installments — Rp33.333", (result as UiText.DynamicString).value)
  }

  private fun assertTrue(condition: Boolean) {
    org.junit.Assert.assertTrue(condition)
  }
}
