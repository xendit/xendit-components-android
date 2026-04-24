package co.xendit.components.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** Visual transformation for card number (XXXX XXXX XXXX XXXX) */
internal class GroupedDigitsTransformation(
  private val groupSize: Int = 4,
  private val separator: Char = ' ',
  private val maxDigits: Int? = null
) : VisualTransformation {

  override fun filter(text: AnnotatedString): TransformedText {
    val raw = text.text.filter { it.isDigit() }
    val digits = if (maxDigits != null) raw.take(maxDigits) else raw

    val originalToTransformed = IntArray(digits.length + 1)
    val out = buildString {
      originalToTransformed[0] = 0
      digits.forEachIndexed { index, ch ->
        append(ch)
        var transformedIndex = length
        val originalOffset = index + 1

        if (originalOffset % groupSize == 0) {
          val shouldAppendSeparator =
            if (index != digits.lastIndex) {
              true
            } else {
              maxDigits != null && digits.length < maxDigits
            }
          if (shouldAppendSeparator) {
            append(separator)
            transformedIndex = length
          }
        }

        originalToTransformed[originalOffset] = transformedIndex
      }
    }

    val transformedToOriginal = IntArray(out.length + 1)
    run {
      var originalCount = 0
      transformedToOriginal[0] = 0
      out.forEachIndexed { index, ch ->
        if (ch.isDigit()) originalCount++
        transformedToOriginal[index + 1] = originalCount
      }
    }

    val offsetMapping = object : OffsetMapping {
      override fun originalToTransformed(offset: Int): Int {
        val digitCount = digits.length
        val clamped = offset.coerceIn(0, digitCount)
        return originalToTransformed[clamped].coerceIn(0, out.length)
      }

      override fun transformedToOriginal(offset: Int): Int {
        val clamped = offset.coerceIn(0, out.length)
        return transformedToOriginal[clamped].coerceIn(0, digits.length)
      }
    }

    return TransformedText(AnnotatedString(out), offsetMapping)
  }
}

/** Visual transformation for expiry date (MM/YY) */
internal class ExpiryDateTransformation : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val trimmed = if (text.text.length >= 4) text.text.substring(0..3) else text.text
    var out = ""
    for (i in trimmed.indices) {
      out += trimmed[i]
      if (i == 1) out += "/"
    }

    val offsetMapping =
      object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
          if (offset <= 1) return offset
          if (offset <= 4) return offset + 1
          return 5
        }

        override fun transformedToOriginal(offset: Int): Int {
          if (offset <= 2) return offset
          if (offset <= 5) return offset - 1
          return 4
        }
      }

    return TransformedText(AnnotatedString(out), offsetMapping)
  }
}
