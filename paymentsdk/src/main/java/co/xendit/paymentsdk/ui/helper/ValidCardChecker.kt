package co.xendit.paymentsdk.ui.helper

object ValidCardChecker {
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
}