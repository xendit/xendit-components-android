package co.xendit.paymentsdk.ui.helper

import java.time.YearMonth

internal object FormCheckerUtil {
  fun isValidCreditCard(cardNumber: String): Boolean {
    // Remove any spaces or dashes the user might have entered
    val digits = cardNumber.replace(Regex("\\D"), "")

    // A valid card must usually be between 13 and 19 digits
    if (digits.length < 13 || digits.length > 19) return false

    var sum = 0
    var isSecond = false

    // Iterate from right to left
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
    // 1. Ensure exactly 4 digits (MMYY)
    val digits = expiryData.replace(Regex("\\D"), "")
    if (digits.length != 4) return false

    // 2. Extract Month and Year
    val month = digits.substring(0, 2).toIntOrNull() ?: return false
    val yearShort = digits.substring(2, 4).toIntOrNull() ?: return false

    // 3. Validate month range
    if (month !in 1..12) return false

    return try {
      // 4. Convert to YearMonth (uu handles the 2-digit year)
      // We format it back to "MM/uu" string just to use the parser safely,
      // or create YearMonth directly
      val currentMonth = YearMonth.now()
      val expiry = YearMonth.of(2000 + yearShort, month)

      // 5. Check if expiry is today or in the future
      !expiry.isBefore(currentMonth)
    } catch (e: Exception) {
      false
    }
  }
}