package co.xendit.paymentsdk.data.mock

import co.xendit.paymentsdk.data.model.BffCardBrand
import co.xendit.paymentsdk.data.model.BffCardInfo
import co.xendit.paymentsdk.data.model.BffChannel
import co.xendit.paymentsdk.data.model.ChannelFormField
import co.xendit.paymentsdk.data.model.FieldType
import co.xendit.paymentsdk.data.model.RegexValidator

/**
 * Sample BffChannel object for "Cards" based on the API response. This can be used for testing or
 * as a mock.
 */
val SAMPLE_CARD_CHANNEL =
  BffChannel(
    brandName = "Cards",
    channelCode = "CARDS",
    pmType = "CARDS",
    brandLogoUrl = "https://assets.xendit.co/payment-session/logos/CARDS.svg",
    uiGroup = "cards",
    allowPayWithoutSave = true,
    allowSave = true,
    brandColor = "#000000",
    minAmount = 5000,
    maxAmount = 200000000,
    requiresCustomerDetails = false,
    card =
      BffCardInfo(
        brands =
          listOf(
            BffCardBrand(
              "VISA",
              "https://assets.xendit.co/payment-session/logos/VISA.svg"
            ),
            BffCardBrand(
              "MASTERCARD",
              "https://assets.xendit.co/payment-session/logos/MASTERCARD.svg"
            ),
            BffCardBrand(
              "AMEX",
              "https://assets.xendit.co/payment-session/logos/AMEX.svg"
            ),
            BffCardBrand(
              "JCB",
              "https://assets.xendit.co/payment-session/logos/JCB.svg"
            )
          )
      ),
    form =
      listOf(
        ChannelFormField(
          groupLabel = "Card Details",
          label = "Card Number",
          placeholder = "1234 1234 1234 1234",
          type =
            FieldType(
              name = "credit_card_number",
              minLength = null,
              maxLength = null,
              numeric = null,
              autocomplete = null,
              regexValidators = null
            ),
          channelProperty = "card_details.card_number",
          required = true,
          span = 2,
          initialValue = null,
          disabled = null,
          join = null,
          flags = null
        ),
        ChannelFormField(
          groupLabel = null,
          label = "Card Expiry Date",
          placeholder = "MM/YY",
          type =
            FieldType(
              name = "credit_card_expiry",
              minLength = null,
              maxLength = null,
              numeric = null,
              autocomplete = null,
              regexValidators = null
            ),
          channelProperty =
            listOf(
              "card_details.expiry_month",
              "card_details.expiry_year"
            ),
          required = true,
          span = 1,
          initialValue = null,
          disabled = null,
          join = true,
          flags = null
        ),
        ChannelFormField(
          groupLabel = null,
          label = "CVN",
          placeholder = "CVN",
          type =
            FieldType(
              name = "credit_card_cvn",
              minLength = null,
              maxLength = null,
              numeric = null,
              autocomplete = null,
              regexValidators = null
            ),
          channelProperty = "card_details.cvn",
          required = true,
          span = 1,
          initialValue = null,
          disabled = null,
          join = true,
          flags = null
        ),
        ChannelFormField(
          groupLabel = "Cardholder Name",
          label = "First Name",
          placeholder = "First Name",
          type =
            FieldType(
              name = "text",
              minLength = 1,
              maxLength = 50,
              numeric = null,
              autocomplete = null,
              regexValidators =
                listOf(
                  RegexValidator(
                    "/^[A-Za-z][ -~À-ÖØ-öø-ÿ]*$/",
                    "Invalid input. Please use alphabetic characters only"
                  )
                )
            ),
          channelProperty = "card_details.cardholder_first_name",
          required = true,
          span = 1,
          initialValue = null,
          disabled = null,
          join = null,
          flags = null
        ),
        ChannelFormField(
          groupLabel = null,
          label = "Last Name",
          placeholder = "Last Name",
          type =
            FieldType(
              name = "text",
              minLength = 1,
              maxLength = 50,
              numeric = null,
              autocomplete = null,
              regexValidators =
                listOf(
                  RegexValidator(
                    "/^[A-Za-z][ -~À-ÖØ-öø-ÿ]*$/",
                    "Invalid input. Please use alphabetic characters only"
                  )
                )
            ),
          channelProperty = "card_details.cardholder_last_name",
          required = true,
          span = 1,
          initialValue = null,
          disabled = null,
          join = null,
          flags = null
        ),
        ChannelFormField(
          groupLabel = null,
          label = "Email",
          placeholder = "john.doe@example.com",
          type =
            FieldType(
              name = "email",
              minLength = null,
              maxLength = null,
              numeric = null,
              autocomplete = null,
              regexValidators = null
            ),
          channelProperty = "card_details.cardholder_email",
          required = true,
          span = 2,
          initialValue = null,
          disabled = null,
          join = null,
          flags = null
        ),
        ChannelFormField(
          groupLabel = null,
          label = "Mobile Number",
          placeholder = "8000032341",
          type =
            FieldType(
              name = "phone_number",
              minLength = null,
              maxLength = null,
              numeric = null,
              autocomplete = null,
              regexValidators = null
            ),
          channelProperty = "card_details.cardholder_phone_number",
          required = true,
          span = 2,
          initialValue = null,
          disabled = null,
          join = null,
          flags = null
        ),
        // ... billing information fields ...
        ChannelFormField(
          groupLabel = "Billing Address",
          label = "First Name",
          placeholder = "First Name",
          type =
            FieldType(
              name = "text",
              minLength = null,
              maxLength = 255,
              numeric = null,
              autocomplete = "given-name",
              regexValidators = null
            ),
          channelProperty = "billing_information.first_name",
          required = true,
          span = 1,
          initialValue = null,
          disabled = null,
          join = null,
          flags = mapOf("require_billing_information" to true)
        ),
        ChannelFormField(
          groupLabel = null,
          label = "Last Name",
          placeholder = "Last Name",
          type =
            FieldType(
              name = "text",
              minLength = null,
              maxLength = 255,
              numeric = null,
              autocomplete = "family-name",
              regexValidators = null
            ),
          channelProperty = "billing_information.last_name",
          required = true,
          span = 1,
          initialValue = null,
          disabled = null,
          join = true,
          flags = mapOf("require_billing_information" to true)
        ),
        ChannelFormField(
          groupLabel = null,
          label = "Email",
          placeholder = "Email",
          type =
            FieldType(
              name = "text",
              minLength = null,
              maxLength = 255,
              numeric = null,
              autocomplete = "email",
              regexValidators = null
            ),
          channelProperty = "billing_information.email",
          required = true,
          span = 2,
          initialValue = null,
          disabled = null,
          join = true,
          flags = mapOf("require_billing_information" to true)
        ),
        ChannelFormField(
          groupLabel = null,
          label = "Country",
          placeholder = "Country",
          type =
            FieldType(
              name = "country",
              minLength = null,
              maxLength = null,
              numeric = null,
              autocomplete = null,
              regexValidators = null
            ),
          channelProperty = "billing_information.country",
          required = true,
          span = 2,
          initialValue = null,
          disabled = null,
          join = true,
          flags = mapOf("require_billing_information" to true)
        ),
        ChannelFormField(
          groupLabel = null,
          label = "Province",
          placeholder = "Province",
          type =
            FieldType(
              name = "province",
              minLength = null,
              maxLength = null,
              numeric = null,
              autocomplete = null,
              regexValidators = null
            ),
          channelProperty = "billing_information.province_state",
          required = true,
          span = 2,
          initialValue = null,
          disabled = null,
          join = true,
          flags = mapOf("require_billing_information" to true)
        ),
        ChannelFormField(
          groupLabel = null,
          label = "City",
          placeholder = "City",
          type =
            FieldType(
              name = "text",
              minLength = null,
              maxLength = 255,
              numeric = null,
              autocomplete = "address-level2",
              regexValidators = null
            ),
          channelProperty = "billing_information.city",
          required = true,
          span = 2,
          initialValue = null,
          disabled = null,
          join = true,
          flags = mapOf("require_billing_information" to true)
        ),
        ChannelFormField(
          groupLabel = null,
          label = "Address Line 1",
          placeholder = "Address Line 1",
          type =
            FieldType(
              name = "text",
              minLength = null,
              maxLength = 255,
              numeric = null,
              autocomplete = "address-line1",
              regexValidators = null
            ),
          channelProperty = "billing_information.street_line1",
          required = true,
          span = 2,
          initialValue = null,
          disabled = null,
          join = true,
          flags = mapOf("require_billing_information" to true)
        ),
        ChannelFormField(
          groupLabel = null,
          label = "Address Line 2",
          placeholder = "Address Line 2",
          type =
            FieldType(
              name = "text",
              minLength = null,
              maxLength = 255,
              numeric = null,
              autocomplete = "address-line2",
              regexValidators = null
            ),
          channelProperty = "billing_information.street_line2",
          required = true,
          span = 2,
          initialValue = null,
          disabled = null,
          join = true,
          flags = mapOf("require_billing_information" to true)
        ),
        ChannelFormField(
          groupLabel = null,
          label = "Postal Code",
          placeholder = "Postal Code",
          type =
            FieldType(
              name = "postal_code",
              minLength = null,
              maxLength = null,
              numeric = null,
              autocomplete = null,
              regexValidators = null
            ),
          channelProperty = "billing_information.postal_code",
          required = true,
          span = 2,
          initialValue = null,
          disabled = null,
          join = true,
          flags = mapOf("require_billing_information" to true)
        )
      ),
    instructions = emptyList()
  )
