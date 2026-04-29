package co.xendit.components.ui.helper

import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.FieldType
import co.xendit.components.data.model.RegexValidator
import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormCheckerTest {

  @Test
  fun `validateField returns error when required field is empty`() {
    val field = ChannelFormField(
      label = "Test Label",
      placeholder = "",
      type = FieldType.Text(),
      channelProperty = "test_prop",
      required = true,
      span = 1
    )

    val result = FormChecker.validateField(field, "")

    assertEquals("Test Label is required", result)
  }

  @Test
  fun `validateField returns null when required field is not empty`() {
    val field = ChannelFormField(
      label = "Test Label",
      placeholder = "",
      type = FieldType.Text(),
      channelProperty = "test_prop",
      required = true,
      span = 1
    )

    val result = FormChecker.validateField(field, "value")

    assertNull(result)
  }

  @Test
  fun `validateField returns error when regex validation fails`() {
    val field = ChannelFormField(
      label = "Email",
      placeholder = "",
      type = FieldType.Text(
        regexValidators = listOf(
          RegexValidator("^[A-Za-z0-9+_.-]+@(.+)$", "Invalid email")
        )
      ),
      channelProperty = "email",
      required = true,
      span = 1
    )

    val result = FormChecker.validateField(field, "invalid-email")

    assertEquals("Invalid email", result)
  }

  @Test
  fun `validateAllField returns true when all fields are valid`() {
    val fields = listOf(
      ChannelFormField(
        label = "Field 1",
        placeholder = "",
        type = FieldType.Text(),
        channelProperty = "field1",
        required = true,
        span = 1
      )
    )
    val values = mapOf("field1" to "value1")

    val result = FormChecker.validateAllField(fields, values)

    assertTrue(result)
  }

  @Test
  fun `validateAllField returns false when at least one field is invalid`() {
    val fields = listOf(
      ChannelFormField(
        label = "Field 1",
        placeholder = "",
        type = FieldType.Text(),
        channelProperty = "field1",
        required = true,
        span = 1
      )
    )
    val values = mapOf("field1" to "")

    val result = FormChecker.validateAllField(fields, values)

    assertFalse(result)
  }

  @Test
  fun `validateField returns null when optional email is blank`() {
    val field = ChannelFormField(
      label = "Email",
      placeholder = "",
      type = FieldType.Email(),
      channelProperty = "email",
      required = false,
      span = 1
    )

    val result = FormChecker.validateField(field, "")

    assertNull(result)
  }

  @Test
  fun `validateField returns error when email format is invalid`() {
    val field = ChannelFormField(
      label = "Email",
      placeholder = "",
      type = FieldType.Email(),
      channelProperty = "email",
      required = false,
      span = 1
    )

    val result = FormChecker.validateField(field, "invalid-email")

    assertEquals("Email is not valid", result)
  }

  @Test
  fun `validateField uses country code lookup for phone number validation`() {
    val phoneUtil = PhoneNumberUtil.getInstance()
    val usExample = phoneUtil.getExampleNumber("US")
    val usNationalNumber = usExample.nationalNumber.toString()

    val field = ChannelFormField(
      label = "Phone",
      placeholder = "",
      type = FieldType.PhoneNumber(),
      channelProperty = "phone",
      required = false,
      span = 1
    )

    val validValues =
      mapOf(
        "phone" to usNationalNumber,
        "phone_country_code" to "US"
      )
    assertNull(FormChecker.validateField(field, usNationalNumber, validValues))

    val invalidValues =
      mapOf(
        "phone" to usNationalNumber,
        "phone_country_code" to "ID"
      )
    assertEquals(
      "Phone is not valid",
      FormChecker.validateField(field, usNationalNumber, invalidValues)
    )
  }
}
