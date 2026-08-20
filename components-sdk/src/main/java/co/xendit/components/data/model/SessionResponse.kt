package co.xendit.components.data.model

import androidx.annotation.Keep
import co.xendit.components.XenditComponentsPaymentType
import co.xendit.components.core.model.FallbackValue
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

@Keep
internal data class SessionResponse(
  @SerializedName("session") val session: BffSession?,
  @SerializedName("business") val business: BffBusiness? = null,
  @SerializedName("channels") val paymentChannels: List<BffChannel>?,
  @SerializedName("channel_ui_groups") val channelUiGroups: List<BffChannelUiGroup>? = null,
  @SerializedName("succeeded_channel") val succeededChannel: SucceededChannel?,
  @SerializedName("digital_wallets") val digitalWallets: BffDigitalWallets? = null
)

@Keep
internal data class BffSession(
  @SerializedName("id") val id: String?,
  @SerializedName("payment_session_id") val paymentSessionId: String?,
  @SerializedName("status") val status: PaymentSessionStatus?,
  @SerializedName("session_type") val sessionType: BffSessionType?,
  @SerializedName("allow_save_payment_method") val allowSavePaymentMethod: BffSessionAllowSavePaymentMethod?,
  @SerializedName("reference_id") val referenceId: String?,
  @SerializedName("currency") val currency: String?,
  @SerializedName("country") val country: String?,
  @SerializedName("amount") val amount: BigDecimal?,
  @SerializedName("subscription") val subscription: BffSubscription? = null,
  @SerializedName("items") val items: List<BffItem>?
)

@Keep
internal data class BffBusiness(
  @SerializedName("name") val name: String? = null,
  @SerializedName("country_of_operation") val countryOfOperation: String? = null,
  @SerializedName("merchant_profile_picture_url") val merchantProfilePictureUrl: String? = null
)

@Keep
internal data class BffChannelUiGroup(
  @SerializedName("id") val id: String,
  @SerializedName("label") val label: String,
  @SerializedName("icon_url") val iconUrl: String? = null
)

@Keep
internal data class BffDigitalWallets(
  @SerializedName("google_pay") val googlePay: BffGooglePay? = null,
  @SerializedName("apple_pay") val applePay: BffApplePay? = null
)

@Keep
internal data class BffGooglePay(
  @SerializedName("merchant_id") val merchantId: String,
  @SerializedName("allowed_payment_methods") val allowedPaymentMethods: List<BffGooglePayAllowedMethod>
)

@Keep
internal data class BffGooglePayAllowedMethod(
  @SerializedName("channel_code") val channelCode: String,
  @SerializedName("payment_request_id") val paymentRequestId: String? = null,
  @SerializedName("payment_method_specification") val paymentMethodSpecification: JsonObject? = null
)

@Keep
internal data class BffApplePay(
  @SerializedName("merchant_id") val merchantId: String? = null,
  @SerializedName("apple_pay_payment_request") val applePayPaymentRequest: JsonObject? = null
)


@Keep
internal enum class PaymentSessionStatus {
  @SerializedName("ACTIVE")
  ACTIVE,
  @SerializedName("PENDING")
  PENDING,
  @SerializedName("COMPLETED")
  COMPLETED,
  @SerializedName("EXPIRED")
  EXPIRED,
  @SerializedName("CANCELED")
  CANCELED
}

@Keep
internal enum class BffSessionType {
  @SerializedName("SAVE")
  SAVE,
  @SerializedName("PAY")
  PAY,
  @SerializedName("AUTHORIZATION")
  AUTHORIZATION,
  @SerializedName("SUBSCRIPTION")
  SUBSCRIPTION
}

internal fun BffSessionType?.usesPaymentTokenSubmission(): Boolean {
  return this == BffSessionType.SAVE || this == BffSessionType.SUBSCRIPTION
}

internal fun BffSessionType?.isPaySession(): Boolean {
  return this == BffSessionType.PAY
}

@Keep
internal enum class BffSessionAllowSavePaymentMethod {
  @SerializedName("DISABLED")
  DISABLED,
  @SerializedName("FORCED")
  FORCED,
  @SerializedName("OPTIONAL")
  OPTIONAL
}

@Keep
internal data class BffSubscription(
  @SerializedName("immediate_payment") val immediatePayment: Boolean? = null,
  @SerializedName("schedule") val schedule: BffSubscriptionSchedule
)

@Keep
internal enum class BffSubscriptionInterval {
  @SerializedName("DAY")
  DAY,
  @SerializedName("WEEK")
  WEEK,
  @SerializedName("MONTH")
  MONTH,
  @FallbackValue
  @SerializedName("UNKNOWN")
  UNKNOWN
}

@Keep
internal data class BffSubscriptionSchedule(
  @SerializedName("anchor_date") val anchorDate: String,
  @SerializedName("interval") val interval: BffSubscriptionInterval,
  @SerializedName("interval_count") val intervalCount: Int,
  @SerializedName("retry_interval") val retryInterval: BffSubscriptionInterval? = null,
  @SerializedName("retry_interval_count") val retryIntervalCount: Int? = null,
  @SerializedName("total_recurrence") val totalRecurrence: Int? = null,
  @SerializedName("total_retry") val totalRetry: Int? = null
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
  @SerializedName("brand_logo_url") val brandLogoUrl: String?,
  @SerializedName("brand_color") val brandColor: String,
  @SerializedName("pm_type") val pmType: XenditComponentsPaymentType?,
  @SerializedName("ui_group") val uiGroup: String,
  @SerializedName("channel_code") val channelCode: String,
  @SerializedName("allow_pay_without_save") val allowPayWithoutSave: Boolean,
  @SerializedName("allow_save") val allowSave: Boolean,
  @SerializedName("min_amount") val minAmount: BigDecimal?,
  @SerializedName("max_amount") val maxAmount: BigDecimal?,
  @SerializedName("requires_customer_details") val requiresCustomerDetails: Boolean?,
  @SerializedName("card") val card: BffCardInfo?,
  @SerializedName("form") val form: List<ChannelFormField>?,
  @SerializedName("instructions") val instructions: List<String>?,
  @SerializedName("banner") val banner: BffChannelBanner? = null
)

@Keep
internal data class BffChannelBanner(
  @SerializedName("image_url") val imageUrl: String?,
  @SerializedName("alt_text") val altText: String? = null,
  @SerializedName("aspect_ratio") val aspectRatio: Float? = null
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
  @SerializedName("placeholder") val placeholder: String? = null,
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

  data class CreditCardNumber(@SerializedName("name") override val name: String = "credit_card_number") :
    FieldType()

  data class CreditCardExpiry(@SerializedName("name") override val name: String = "credit_card_expiry") :
    FieldType()

  data class CreditCardCvn(@SerializedName("name") override val name: String = "credit_card_cvn") :
    FieldType()

  data class PhoneNumber(@SerializedName("name") override val name: String = "phone_number") :
    FieldType()

  data class Email(@SerializedName("name") override val name: String = "email") : FieldType()
  data class PostalCode(@SerializedName("name") override val name: String = "postal_code") :
    FieldType()

  data class Country(@SerializedName("name") override val name: String = "country") : FieldType()
  data class Province(@SerializedName("name") override val name: String = "province") : FieldType()
  data class InstallmentPlan(@SerializedName("name") override val name: String = "installment_plan") :
    FieldType()

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
