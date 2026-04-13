package co.xendit.paymentsdk.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class SessionResponse(
  @SerializedName("session") val session: BffSession?,
  @SerializedName("channels") val paymentChannels: List<BffChannel>?
)

@Keep
data class BffSession(
  @SerializedName("id") val id: String?,
  @SerializedName("payment_session_id") val paymentSessionId: String?,
  @SerializedName("status") val status: String?,
  @SerializedName("session_type") val sessionType: String?,
  @SerializedName("allow_save_payment_method") val allowSavePaymentMethod: String?,
  @SerializedName("reference_id") val referenceId: String?,
  @SerializedName("currency") val currency: String?,
  @SerializedName("amount") val amount: Long?,
  @SerializedName("items") val items: List<BffItem>?
)

@Keep
data class BffItem(
  @SerializedName("reference_id") val referenceId: String?,
  @SerializedName("type") val type: String?,
  @SerializedName("name") val name: String?,
  @SerializedName("net_unit_amount") val netUnitAmount: Long?,
  @SerializedName("quantity") val quantity: Int?,
  @SerializedName("category") val category: String?
)

@Keep
data class BffChannel(
  @SerializedName("brand_name") val brandName: String,
  @SerializedName("brand_logo_url") val brandLogoUrl: String,
  @SerializedName("brand_color") val brandColor: String,
  @SerializedName("pm_type") val pmType: String?,
  @SerializedName("ui_group") val uiGroup: String,
  @SerializedName("channel_code") val channelCode: String,
  @SerializedName("allow_pay_without_save") val allowPayWithoutSave: Boolean,
  @SerializedName("allow_save") val allowSave: Boolean,
  @SerializedName("min_amount") val minAmount: Long?,
  @SerializedName("max_amount") val maxAmount: Long?,
  @SerializedName("requires_customer_details") val requiresCustomerDetails: Boolean?,
  @SerializedName("card") val card: BffCardInfo?,
  @SerializedName("form") val form: List<ChannelFormField>?,
  @SerializedName("instructions") val instructions: List<String>?
)

@Keep
data class BffCardInfo(@SerializedName("brands") val brands: List<BffCardBrand>)

@Keep
data class BffCardBrand(
  @SerializedName("name") val name: String,
  @SerializedName("logo_url") val logoUrl: String
)

@Keep
data class ChannelFormField(
  @SerializedName("label") val label: String,
  @SerializedName("group_label") val groupLabel: String? = null,
  @SerializedName("placeholder") val placeholder: String,
  @SerializedName("type") val type: FieldType,
  @SerializedName("channel_property")
  val channelProperty: Any, // Can be String or List<String>
  @SerializedName("required") val required: Boolean,
  @SerializedName("span") val span: Int, // 1 or 2
  @SerializedName("initial_value") val initialValue: String? = null,
  @SerializedName("disabled") val disabled: Boolean? = null,
  @SerializedName("join") val join: Boolean? = null,
  @SerializedName("display_if") val displayIf: List<List<String>>? = null,
  @SerializedName("flags") val flags: Map<String, Any>? = null
)

@Keep
data class FieldType(
  @SerializedName("name") val name: String,
  @SerializedName("min_length") val minLength: Int?,
  @SerializedName("max_length") val maxLength: Int?,
  @SerializedName("numeric") val numeric: Boolean?,
  @SerializedName("autocomplete") val autocomplete: String?,
  @SerializedName("regex_validators") val regexValidators: List<RegexValidator>?
)

data class RegexValidator(
  @SerializedName("regex") val regex: String,
  @SerializedName("message") val message: String
)

fun ChannelFormField.primaryChannelPropertyKey(): String {
  return when (val prop = channelProperty) {
    is String -> prop
    is List<*> -> prop.firstOrNull()?.toString().orEmpty()
    else -> ""
  }
}
