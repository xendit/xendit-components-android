package co.xendit.components.ui.helper

internal object QrNmidSearcherUtil {

  fun getNationalMerchantID(qrisData: String): String? {
    val cleanData = qrisData.replace("\\s".toRegex(), "") // Hapus spasi/newline

    for (tagNumber in 26..51) {
      val merchantAccountInfo = getValueByTag(cleanData, tagNumber.toString())
      if (merchantAccountInfo != null) {
        val reverseDomain = getValueByTag(merchantAccountInfo, "00")
        if (reverseDomain == "ID.CO.QRIS.WWW") {
          val nmid = getValueByTag(merchantAccountInfo, "02")
          if (nmid != null) return nmid
        }
      }
    }
    return null
  }

  fun getValueByTag(data: String?, targetTag: String): String? {
    if (data == null || data.length < 4) return null
    val formattedTarget = targetTag.padStart(2, '0')
    var index = 0
    while (index <= data.length - 4) {
      val tag = data.substring(index, index + 2)
      val length = data.substring(index + 2, index + 4).toIntOrNull() ?: return null

      val valueStart = index + 4
      val valueEnd = valueStart + length

      if (valueEnd > data.length) break

      if (tag == formattedTarget) {
        return data.substring(valueStart, valueEnd)
      }
      index = valueEnd
    }
    return null
  }
}