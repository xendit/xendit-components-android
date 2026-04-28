package co.xendit.components.ui.helper

import co.xendit.components.ui.helper.FormCheckerUtil.isValidEmail
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class FormCheckerUtilTest {

  @Test
  fun isValidCreditCard_validCards_returnsTrue() {
    // Test with some known valid card numbers (using Luhn algorithm)
    assertTrue(FormCheckerUtil.isValidCreditCard("378282246310005"))
    assertTrue(FormCheckerUtil.isValidCreditCard("3782 8224 6310 005"))
  }

  @Test
  fun isValidCreditCard_invalidCards_returnsFalse() {
    assertFalse(FormCheckerUtil.isValidCreditCard("49927398717")) // Invalid Luhn
    assertFalse(FormCheckerUtil.isValidCreditCard("123")) // Too short
    assertFalse(FormCheckerUtil.isValidCreditCard("12345678901234567890")) // Too long
    assertFalse(FormCheckerUtil.isValidCreditCard("")) // Empty
  }

  @Test
  fun isValidCardExpiry_validFutureDates_returnsTrue() {
    // Use "MMyy" to match your new 4-digit requirement
    val formatter = DateTimeFormatter.ofPattern("MMyy")

    val nextMonth = YearMonth.now().plusMonths(1)
    assertTrue(FormCheckerUtil.isValidCardExpiry(nextMonth.format(formatter)))

    val currentMonth = YearMonth.now()
    assertTrue(FormCheckerUtil.isValidCardExpiry(currentMonth.format(formatter)))
  }

  @Test
  fun isValidCardExpiry_pastDates_returnsFalse() {
    val formatter = DateTimeFormatter.ofPattern("MMyy")

    val lastMonth = YearMonth.now().minusMonths(1)
    assertFalse(FormCheckerUtil.isValidCardExpiry(lastMonth.format(formatter)))
  }

  @Test
  fun isValidCardExpiry_invalidFormats_returnsFalse() {
    // Invalid Month
    assertFalse(FormCheckerUtil.isValidCardExpiry("1325"))

    // Wrong digit counts
    assertFalse(FormCheckerUtil.isValidCardExpiry("125"))    // 3 digits
    assertFalse(FormCheckerUtil.isValidCardExpiry("012025")) // 6 digits

    // Non-numeric
    assertFalse(FormCheckerUtil.isValidCardExpiry("abcd"))
  }

  @Test
  fun isValidEmail_validFormats_returnsTrue() {
    assertTrue(isValidEmail("user@example.com"))
    assertTrue(isValidEmail("first.last@sub.domain.org"))
    assertTrue(isValidEmail("user123@gmail.co"))
  }

  @Test
  fun isValidEmail_invalidFormats_returnsFalse() {
    // Missing @
    assertFalse(isValidEmail("userexample.com"))

    // Missing domain
    assertFalse(isValidEmail("user@"))

    // Missing username
    assertFalse(isValidEmail("@example.com"))

    // Missing TLD (Top Level Domain)
    assertFalse(isValidEmail("user@example"))

    // Contains invalid characters/spaces
    assertFalse(isValidEmail("user name@example.com"))
  }
}
