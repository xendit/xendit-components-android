package co.xendit.components.ui.helper

import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.FieldType
import co.xendit.components.data.model.RegexValidator
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
}
