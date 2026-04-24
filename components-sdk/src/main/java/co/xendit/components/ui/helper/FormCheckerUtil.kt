package co.xendit.components.ui.helper

import java.time.YearMonth

internal object FormCheckerUtil {
  fun isValidCreditCard(cardNumber: String): Boolean {
    val digits = cardNumber.replace(Regex("\\D"), "")

    if (digits.length < 13 || digits.length > 19) return false

    var sum = 0
    var isSecond = false

    for (i in digits.length - 1 downTo 0) {
      var d = digits[i] - '0' // Convert Char to Int

      if (isSecond) {
        d *= 2
        if (d > 9) d -= 9
      }

      sum += d
      isSecond = !isSecond
    }

    return sum % 10 == 0
  }

  fun isValidCardExpiry(expiryData: String): Boolean {
    val digits = expiryData.replace(Regex("\\D"), "")
    if (digits.length != 4) return false

    val month = digits.substring(0, 2).toIntOrNull() ?: return false
    val yearShort = digits.substring(2, 4).toIntOrNull() ?: return false

    if (month !in 1..12) return false

    return try {
      val currentMonth = YearMonth.now()
      val expiry = YearMonth.of(2000 + yearShort, month)

      !expiry.isBefore(currentMonth)
    } catch (e: Exception) {
      false
    }
  }
}