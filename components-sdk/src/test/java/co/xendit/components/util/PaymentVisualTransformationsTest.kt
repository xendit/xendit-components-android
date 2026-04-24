package co.xendit.components.util

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentVisualTransformationsTest {

  @Test
  fun cardNumberTransformation_formatsCorrectly() {
    val transformation = GroupedDigitsTransformation(groupSize = 4, maxDigits = 16)
    val input = AnnotatedString("1234567812345678")
    val result = transformation.filter(input)

    assertEquals("1234 5678 1234 5678", result.text.text)
  }

  @Test
  fun cardNumberTransformation_formatsPartialCorrectly() {
    val transformation = GroupedDigitsTransformation(groupSize = 3)
    val input = AnnotatedString("12345")
    val result = transformation.filter(input)

    assertEquals("123 45", result.text.text)
  }

  @Test
  fun expiryDateTransformation_formatsCorrectly() {
    val transformation = GroupedDigitsTransformation(groupSize = 2, separator = '/', maxDigits = 4)
    val input = AnnotatedString("1225")
    val result = transformation.filter(input)

    assertEquals("12/25", result.text.text)
  }

  @Test
  fun expiryDateTransformation_formatsPartialCorrectly() {
    val transformation = GroupedDigitsTransformation(groupSize = 2, separator = '/', maxDigits = 4)
    val input = AnnotatedString("1")
    val result = transformation.filter(input)

    assertEquals("1", result.text.text)
  }

  @Test
  fun expiryDateTransformation_formatsTwoDigitsCorrectly() {
    val transformation = GroupedDigitsTransformation(groupSize = 2, separator = '/', maxDigits = 4)
    val input = AnnotatedString("12")
    val result = transformation.filter(input)

    assertEquals("12/", result.text.text)
  }
}
