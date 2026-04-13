package co.xendit.paymentsdk.util

import co.xendit.paymentsdk.data.encryption.XenditEncryption
import co.xendit.paymentsdk.data.model.ChannelFormField
import co.xendit.paymentsdk.data.model.Country
import co.xendit.paymentsdk.data.model.primaryChannelPropertyKey

import co.xendit.paymentsdk.data.model.InstallmentPlan

object PaymentRequestMapper {

  /**
   * Maps flat form values (dot-notated keys) to a nested map structure as required by the payment
   * API. Handles encryption for sensitive fields based on their type.
   */
  fun mapFormValuesToChannelProperties(
    formValues: Map<String, String>,
    fields: List<ChannelFormField>,
    publicKey: String,
    sessionId: String,
    installmentPlans: List<InstallmentPlan>? = null
  ): Map<String, Any> {
    val flatMap = mutableMapOf<String, Any>()

    fields.forEach { field ->
      val propertyKey = field.primaryChannelPropertyKey()
      val value = formValues[propertyKey] ?: return@forEach

      val isSensitive = isSensitiveField(field)

      when (val prop = field.channelProperty) {
        is String -> {
          var finalValue = if (isSensitive) encrypt(value, publicKey, sessionId) else value
          if (field.type.name == "phone_number") {
            val countryCodeKey = "${propertyKey}_country_code"
            val countryCode = formValues[countryCodeKey] ?: "ID"
            val dialCode = Country.fromCode(countryCode)?.dialCode ?: "62"
            finalValue = "+$dialCode$value"
          }
          flatMap[prop] = finalValue
        }

        is List<*> -> {
          // Handle list properties, specifically credit card expiry split
          if (field.type.name == "credit_card_expiry" && prop.size >= 2) {
            val monthKey = prop[0].toString()
            val yearKey = prop[1].toString()
            val (month, year) = splitExpiry(value)

            flatMap[monthKey] = if (isSensitive) encrypt(month, publicKey, sessionId) else month
            flatMap[yearKey] = if (isSensitive) encrypt(year, publicKey, sessionId) else year
          } else if (field.type.name == "installment_plan" && prop.size >= 2) {
            val termsKey = prop[0].toString()
            val intervalKey = prop[1].toString()
            val termsNum = value.toIntOrNull()
            
            if (termsNum != null && termsNum > 0) {
              val plan = installmentPlans?.find { it.terms == termsNum }
              if (plan != null) {
                flatMap[termsKey] = termsNum
                if (plan.interval != null) flatMap[intervalKey] = plan.interval
              }
            }
          } else {
            // Default to using the first key if not specially handled
            val key = prop.firstOrNull()?.toString() ?: ""
            if (key.isNotEmpty()) {
              flatMap[key] = if (isSensitive) encrypt(value, publicKey, sessionId) else value
            }
          }
        }
      }
    }

    return unflatten(flatMap)
  }

  private fun isSensitiveField(field: ChannelFormField): Boolean {
    // Currently designating card sensitive fields for encryption
    return field.type.name in listOf("credit_card_number", "credit_card_expiry", "credit_card_cvn")
  }

  private fun encrypt(value: String, publicKey: String, sessionId: String): String {
    //    val cleaned = value.replace("\\s".toRegex(), "")
    val cleaned = value
    return if (cleaned.isBlank()) "" else XenditEncryption.encrypt(cleaned, publicKey, sessionId)
  }

  private fun splitExpiry(expiry: String): Pair<String, String> {
    return if (expiry.length == 4) {
      expiry.take(2) to "20${expiry.takeLast(2)}"
    } else {
      "" to ""
    }
  }

  //  Converts a flat map with dot-notated keys into a nested map structure.
  //  Imagine you are processing card_details.card_number.
  //
  //  Loop 1 (Handling "card_details"):
  //  Target Key: card_details.
  //  current: Starts as the empty root {}.
  //  Action: getOrPut("card_details") doesn't find anything. It creates an empty map {} and puts it
  // under the key "card_details".
  //  Result: Your root map is now {"card_details": {}}.
  //  Variable next: Becomes that inner empty map. We move inside it for the next part of the loop.
  //
  //  Loop 2 (Handling "card_number"):
  //  Target Key: card_number.
  //  current: Is now the map we just found/created {"card_details": -> HERE }.
  //  Action: This is the last part of the dot-string, so the function skips the getOrPut loop and
  // simply sets the value: current["card_number"] = "1234...".
  //  RESULT
  // {
  //  "card_details": {
  //    "card_number": "1234..."
  //  }
  // }

  private fun unflatten(flatMap: Map<String, Any>): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    for ((key, value) in flatMap) {
      val parts = key.split(".")
      var current = result
      for (i in 0 until parts.size - 1) {
        val part = parts[i]

        @Suppress("UNCHECKED_CAST")
        //        NORMAL MODE
        //        var next = current[part]
        //        if (next == null) {
        //          next = mutableMapOf<String, Any>()
        //          current[part] = next
        //        }
        val next = current.getOrPut(part) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
        current = next
      }
      current[parts.last()] = value
    }
    return result
  }
}
