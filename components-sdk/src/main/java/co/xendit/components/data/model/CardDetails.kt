package co.xendit.components.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
internal data class CardDetails(
  @SerializedName("require_billing_information") val requireBillingInformation: Boolean,
  @SerializedName("country_codes") val countryCodes: List<String>,
  @SerializedName("schemes") val schemes: List<String>? = null
)
