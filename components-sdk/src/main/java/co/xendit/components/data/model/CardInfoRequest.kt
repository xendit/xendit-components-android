package co.xendit.components.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
internal data class CardInfoRequest(
  @SerializedName("card_number") val cardNumber: String
)
