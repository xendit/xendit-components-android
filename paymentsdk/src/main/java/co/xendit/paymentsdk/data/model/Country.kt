package co.xendit.paymentsdk.data.model

import androidx.annotation.Keep
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

@Keep
data class Country(val name: String, val code: String, val dialCode: String) {
  val flagUrl: String
    get() = "https://assets.xendit.co/payment-session/flags/circle/${code.lowercase()}.svg"

  companion object {
    private val phoneUtil = PhoneNumberUtil.getInstance()

    private val _countriesFlow = MutableStateFlow<List<Country>>(emptyList())
    val countriesFlow: StateFlow<List<Country>> = _countriesFlow.asStateFlow()

    val countries: List<Country> by lazy {
      // Priority countries to show immediately

      val allRegions = phoneUtil.supportedRegions
      val list = allRegions
        .map { regionCode ->
          val dialCode = phoneUtil.getCountryCodeForRegion(regionCode)
          val name = Locale("", regionCode).displayCountry
          Country(name = name, code = regionCode, dialCode = dialCode.toString())
        }
        .filter { it.name.isNotEmpty() }
        .sortedWith(compareBy<Country> { it.name })

      _countriesFlow.value = list
      list
    }

    fun warmUp() {
      // Accessing countries property triggers lazy initialization
      countries
    }

    fun fromCode(code: String): Country? {
      // Access property safely, might block if not ready
      return countries.find { it.code.equals(code, ignoreCase = true) }
    }

    fun fromDialCode(dialCode: String): Country? {
      // Access property safely, might block if not ready
      return countries.find { it.dialCode == dialCode }
    }
  }
}

fun List<Country>.findCountryByCode(code: String): Country? {
  return find { it.code.equals(code, ignoreCase = true) }
}

fun List<Country>.findCountryByDialCode(dialCode: String): Country? {
  return find { it.dialCode == dialCode }
}
