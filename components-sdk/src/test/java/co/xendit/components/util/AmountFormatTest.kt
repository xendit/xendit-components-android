package co.xendit.components.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountFormatTest {

  @Test
  fun symbol_returnsMappedSymbol() {
    assertEquals("Rp", AmountFormat.symbol("IDR"))
    assertEquals("US$", AmountFormat.symbol("usd"))
    assertEquals("₱", AmountFormat.symbol("PHP"))
  }

  @Test
  fun symbol_returnsEmptyForNullOrBlank() {
    assertEquals("", AmountFormat.symbol(null))
    assertEquals("", AmountFormat.symbol("  "))
  }

  @Test
  fun format_returnsEmptyForNullAmountOrBlankCurrency() {
    assertEquals("", AmountFormat.format(null as Long?, "IDR"))
    assertEquals("", AmountFormat.format(1000L, null))
    assertEquals("", AmountFormat.format(1000L, " "))
  }

  @Test
  fun format_formatsWithLocaleAndSymbol() {
    assertEquals("Rp150.000", AmountFormat.format(150_000L, "IDR"))
    assertEquals("US$150,000", AmountFormat.format(150_000L, "USD"))
  }

  @Test
  fun format_appliesSymbolPositioningWhenConfigured() {
    assertEquals("1,000៛", AmountFormat.format(1_000L, "KHR"))
  }

  @Test
  fun format_removesTrailingZeroDecimals() {
    assertEquals("US$12", AmountFormat.format(12.0, "USD"))
    assertEquals("US$12.30", AmountFormat.format(12.3, "USD"))
  }

  @Test
  fun format_supportsCurrenciesWithThreeDecimals() {
    assertEquals("BHD 12.300", AmountFormat.format(12.3, "BHD"))
  }

  @Test
  fun format_formatsNegativeAmount() {
    assertEquals("-US$1,200", AmountFormat.format(-1_200L, "USD"))
  }

  @Test
  fun format_fallsBackToCurrencyCodeWhenSymbolUnknown() {
    assertEquals("XYZ 10", AmountFormat.format(10L, "XYZ"))
  }
}
