package co.xendit.paymentsdk.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
internal data class SessionResponse(
  @SerializedName("session") val session: BffSession?,
  @SerializedName("channels") val paymentChannels: List<BffChannel>?
)

@Keep
internal data class BffSession(
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
internal data class BffItem(
  @SerializedName("reference_id") val referenceId: String?,
  @SerializedName("type") val type: String?,
  @SerializedName("name") val name: String?,
  @SerializedName("net_unit_amount") val netUnitAmount: Long?,
  @SerializedName("quantity") val quantity: Int?,
  @SerializedName("category") val category: String?
)

@Keep
internal data class BffChannel(
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
internal data class BffCardInfo(@SerializedName("brands") val brands: List<BffCardBrand>)

@Keep
internal data class BffCardBrand(
  @SerializedName("name") val name: String,
  @SerializedName("logo_url") val logoUrl: String
)

@Keep
internal data class ChannelFormField(
  @SerializedName("group_label") val groupLabel: String? = null,
  @SerializedName("label") val label: String,
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
internal sealed class FieldType {
  @get:SerializedName("name")
  abstract val name: String

  companion object {
    operator fun invoke(
      name: String,
      minLength: Int? = null,
      maxLength: Int? = null,
      numeric: Boolean? = null,
      regexValidators: List<RegexValidator>? = null,
      autocomplete: String? = null,
      options: List<DropdownOption>? = null
    ): FieldType {
      return when (name) {
        "credit_card_number" -> CreditCardNumber()
        "credit_card_expiry" -> CreditCardExpiry()
        "credit_card_cvn" -> CreditCardCvn()
        "phone_number" -> PhoneNumber()
        "email" -> Email()
        "postal_code" -> PostalCode()
        "country" -> Country()
        "province" -> Province()
        "installment_plan" -> InstallmentPlan()
        "dropdown" -> Dropdown(options = options ?: emptyList())
        else ->
          Text(
            name = name,
            minLength = minLength,
            maxLength = maxLength,
            numeric = numeric,
            regexValidators = regexValidators,
            autocomplete = autocomplete
          )
      }
    }
  }

  data class CreditCardNumber(@SerializedName("name") override val name: String = "credit_card_number") : FieldType()
  data class CreditCardExpiry(@SerializedName("name") override val name: String = "credit_card_expiry") : FieldType()
  data class CreditCardCvn(@SerializedName("name") override val name: String = "credit_card_cvn") : FieldType()
  data class PhoneNumber(@SerializedName("name") override val name: String = "phone_number") : FieldType()
  data class Email(@SerializedName("name") override val name: String = "email") : FieldType()
  data class PostalCode(@SerializedName("name") override val name: String = "postal_code") : FieldType()
  data class Country(@SerializedName("name") override val name: String = "country") : FieldType()
  data class Province(@SerializedName("name") override val name: String = "province") : FieldType()
  data class InstallmentPlan(@SerializedName("name") override val name: String = "installment_plan") : FieldType()

  data class Text(
    @SerializedName("name") override val name: String = "text",
    @SerializedName("min_length") val minLength: Int? = null,
    @SerializedName("max_length") val maxLength: Int? = null,
    @SerializedName("numeric") val numeric: Boolean? = null,
    @SerializedName("regex_validators") val regexValidators: List<RegexValidator>? = null,
    @SerializedName("autocomplete") val autocomplete: String? = null
  ) : FieldType()

  data class Dropdown(
    @SerializedName("name") override val name: String = "dropdown",
    @SerializedName("options") val options: List<DropdownOption>
  ) : FieldType()
}

@Keep
internal data class DropdownOption(
  @SerializedName("label") val label: String,
  @SerializedName("subtitle") val subtitle: String? = null,
  @SerializedName("icon_url") val iconUrl: String? = null,
  @SerializedName("value") val value: String
)

internal data class RegexValidator(
  @SerializedName("regex") val regex: String,
  @SerializedName("message") val message: String
)

internal fun ChannelFormField.primaryChannelPropertyKey(): String {
  return when (val prop = channelProperty) {
    is String -> prop
    is List<*> -> prop.firstOrNull()?.toString().orEmpty()
    else -> ""
  }
}
