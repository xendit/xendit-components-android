package co.xendit.components.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

internal object AmountFormat {
  private val currencyNumberFormatLocale: Map<String, String> = mapOf(
    "IDR" to "id",
    "VND" to "vi",
    "BRL" to "pt-BR",
    "RUB" to "ru",
    "CZK" to "cs",
    "RON" to "ro",
    "UAH" to "uk",
    "CLP" to "es-CL",
    "COP" to "es-CO",
    "UYU" to "es-UY",
    "ARS" to "es-AR",
    "INR" to "hi-IN",
    "NPR" to "hi-IN",
    "LKR" to "hi-IN",
    "BDT" to "hi-IN"
  )

  private val currencySymbols: Map<String, String> = mapOf(
    "USD" to "US$",
    "CAD" to "CA$",
    "EUR" to "€",
    "AFN" to "؋",
    "ALL" to "Lek",
    "AMD" to "֏",
    "ARS" to "AR$",
    "AUD" to "AU$",
    "AZN" to "₼",
    "BAM" to "KM",
    "BDT" to "৳",
    "BIF" to "FBu",
    "BND" to "BN$",
    "BOB" to "Bs",
    "BRL" to "R$",
    "BWP" to "P",
    "BYN" to "Br",
    "BZD" to "BZ$",
    "CDF" to "FrCD",
    "CHF" to "CHF",
    "CLP" to "CL$",
    "CNY" to "CN¥",
    "COP" to "CO$",
    "CRC" to "₡",
    "CVE" to "CV$",
    "CZK" to "Kč",
    "DJF" to "Fdj",
    "DKK" to "kr",
    "DOP" to "RD$",
    "ERN" to "Nfk",
    "ETB" to "Br",
    "GBP" to "£",
    "GEL" to "₾",
    "GHS" to "GH₵",
    "GNF" to "FG",
    "GTQ" to "Q",
    "HKD" to "HK$",
    "HNL" to "L",
    "HUF" to "Ft",
    "IDR" to "Rp",
    "ILS" to "₪",
    "INR" to "₹",
    "IRR" to "IRR",
    "ISK" to "kr",
    "JMD" to "J$",
    "JPY" to "￥",
    "KES" to "Ksh",
    "KHR" to "៛",
    "KMF" to "FC",
    "KRW" to "₩",
    "KZT" to "₸",
    "LKR" to "SL Re",
    "MDL" to "lei",
    "MGA" to "MGA",
    "MKD" to "MKD",
    "MMK" to "K",
    "MOP" to "MOP$",
    "MUR" to "₨",
    "MXN" to "MXN$",
    "MYR" to "RM",
    "MZN" to "MTn",
    "NAD" to "N$",
    "NGN" to "₦",
    "NIO" to "C$",
    "NOK" to "kr",
    "NPR" to "रु",
    "NZD" to "NZ$",
    "PAB" to "B/.",
    "PEN" to "S/.",
    "PHP" to "₱",
    "PKR" to "₨",
    "PLN" to "zł",
    "PYG" to "₲",
    "RON" to "RON",
    "RSD" to "RSD",
    "RUB" to "₽",
    "RWF" to "FR",
    "SDG" to "SDG",
    "SEK" to "kr",
    "SGD" to "S$",
    "SOS" to "Ssh",
    "THB" to "฿",
    "TOP" to "T$",
    "TRY" to "TL",
    "TTD" to "TT$",
    "TWD" to "NT$",
    "TZS" to "TSh",
    "UAH" to "₴",
    "UGX" to "USh",
    "UYU" to "$"+"U",
    "UZS" to "сум",
    "VND" to "₫",
    "XAF" to "FCFA",
    "XOF" to "CFA",
    "ZAR" to "R",
    "ZMW" to "K",
    "ZWL" to "ZWL$"
  )

  private val currencySymbolPosition: Map<String, String> = mapOf(
    "ALL" to "1 \$",
    "BAM" to "1 \$",
    "BYN" to "1 \$",
    "CZK" to "1 \$",
    "DKK" to "1 \$",
    "GEL" to "1 \$",
    "HUF" to "1 \$",
    "ISK" to "1 \$",
    "IRR" to "1 \$",
    "KHR" to "1\$",
    "MDL" to "1 \$",
    "MKD" to "1 \$",
    "NOK" to "1 \$",
    "PLN" to "1 \$",
    "RON" to "1 \$",
    "RSD" to "1 \$",
    "RUB" to "1\$",
    "SEK" to "1 \$",
    "UZS" to "1 \$",
    "VND" to "1\$"
  )

  private val currencySymbolDecimals: Map<String, Int> = mapOf(
    "BHD" to 3,
    "JOD" to 3,
    "KWD" to 3,
    "LYD" to 3,
    "OMR" to 3,
    "TND" to 3
  )

  fun symbol(currency: String?): String {
    val code = currency?.trim()?.uppercase(Locale.ROOT).orEmpty()
    if (code.isBlank()) return ""
    return currencySymbols[code] ?: code
  }

  fun format(amount: Long?, currency: String?): String {
    val code = currency?.trim()?.uppercase(Locale.ROOT).orEmpty()
    if (amount == null || code.isBlank()) return ""
    return formatInternal(BigDecimal.valueOf(amount), code)
  }

  fun format(amount: Double?, currency: String?): String {
    val code = currency?.trim()?.uppercase(Locale.ROOT).orEmpty()
    if (amount == null || code.isBlank()) return ""
    return formatInternal(BigDecimal.valueOf(amount), code)
  }

  private fun formatInternal(amount: BigDecimal, currencyCode: String): String {
    val isNegative = amount < BigDecimal.ZERO
    val absAmount = amount.abs()

    val localeTag = currencyNumberFormatLocale[currencyCode] ?: "en"
    val locale = Locale.forLanguageTag(localeTag)
    val decimals = currencySymbolDecimals[currencyCode] ?: 2

    val number =
      NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = 20
      }

    var str = number.format(absAmount)
    str = str.replace(Regex("(\\.|,)000?$"), "")

    val formatted =
      if (currencySymbols.containsKey(currencyCode)) {
        val symbol = currencySymbols[currencyCode] ?: currencyCode
        val positioning = currencySymbolPosition[currencyCode] ?: "\$1"
        positioning.replace("\$", symbol).replace("1", str)
      } else {
        "$currencyCode $str"
      }

    return if (isNegative) "-$formatted" else formatted
  }
}
