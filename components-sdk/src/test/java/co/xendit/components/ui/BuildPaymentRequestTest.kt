package co.xendit.components.ui

import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.FieldType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildPaymentRequestTest {

  @Test
  fun buildPaymentRequest_whenChannelFormNull_doesNotFilterFormValues() {
    val phoneField =
      ChannelFormField(
        label = "Phone",
        type = FieldType.PhoneNumber(),
        channelProperty = "customer.mobile_number",
        required = true,
        span = 2
      )
    val firstNameField =
      ChannelFormField(
        label = "First Name",
        type = FieldType.Text(),
        channelProperty = "billing_information.first_name",
        required = true,
        span = 1
      )

    val formValues =
      mapOf(
        "customer.mobile_number" to "8123456789",
        "customer.mobile_number_country_code" to "ID",
        "billing_information.first_name" to "firstName"
      )

    val request =
      buildPaymentRequest(
        sessionAuthKey = "session-123",
        publicKey = "pk_test",
        paymentSessionId = "paySid-123",
        effectiveChannelCode = "EWALLET_X",
        formValues = formValues,
        fields = listOf(phoneField, firstNameField),
        savePaymentMethod = false,
        installmentPlans = null,
        effectiveChannelForm = null
      )

    assertEquals("session-123", request.sessionId)
    assertEquals("EWALLET_X", request.channelCode)
    assertNull(request.savePaymentMethod)

    val customer = request.channelProperties["customer"] as Map<*, *>
    assertEquals("+628123456789", customer["mobile_number"])

    val billing = request.channelProperties["billing_information"] as Map<*, *>
    assertEquals("firstName", billing["first_name"])
  }

  @Test
  fun buildPaymentRequest_whenAllowlistProvided_filtersFields_butKeepsPhoneCountryCode() {
    val phoneField =
      ChannelFormField(
        label = "Phone",
        type = FieldType.PhoneNumber(),
        channelProperty = "customer.mobile_number",
        required = true,
        span = 2
      )
    val firstNameField =
      ChannelFormField(
        label = "First Name",
        type = FieldType.Text(),
        channelProperty = "billing_information.first_name",
        required = true,
        span = 1
      )

    val formValues =
      mapOf(
        "customer.mobile_number" to "8123456789",
        "customer.mobile_number_country_code" to "ID",
        "billing_information.first_name" to "Arga"
      )

    val request =
      buildPaymentRequest(
        sessionAuthKey = "session-123",
        publicKey = "pk_test",
        paymentSessionId = "paySid-123",
        effectiveChannelCode = "CARD_X",
        formValues = formValues,
        fields = listOf(phoneField, firstNameField),
        savePaymentMethod = true,
        installmentPlans = null,
        effectiveChannelForm = listOf(phoneField)
      )

    assertEquals(true, request.savePaymentMethod)
    assertTrue("billing_information" !in request.channelProperties)

    val customer = request.channelProperties["customer"] as Map<*, *>
    assertEquals("+628123456789", customer["mobile_number"])
  }

  @Test
  fun buildPaymentRequest_whenNonSaveChannelSelected_doesNotIncludeSaveOnlyValues() {
    val phoneField =
      ChannelFormField(
        label = "Phone",
        type = FieldType.PhoneNumber(),
        channelProperty = "customer.mobile_number",
        required = true,
        span = 2
      )
    val saveOnlyCustomerIdField =
      ChannelFormField(
        label = "Customer ID",
        type = FieldType.Text(),
        channelProperty = "customer.id",
        required = false,
        span = 2
      )

    val formValues =
      mapOf(
        "customer.mobile_number" to "8123456789",
        "customer.mobile_number_country_code" to "ID",
        "customer.id" to "cust_save_only"
      )

    val request =
      buildPaymentRequest(
        sessionAuthKey = "session-123",
        publicKey = "pk_test",
        paymentSessionId = "paySid-123",
        effectiveChannelCode = "EWALLET_OVO",
        formValues = formValues,
        fields = listOf(phoneField, saveOnlyCustomerIdField),
        savePaymentMethod = false,
        installmentPlans = null,
        effectiveChannelForm = listOf(phoneField)
      )

    assertNull(request.savePaymentMethod)

    val customer = request.channelProperties["customer"] as Map<*, *>
    assertEquals("+628123456789", customer["mobile_number"])
    assertTrue("id" !in customer.keys)
  }
}
