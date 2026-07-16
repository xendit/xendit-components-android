package co.xendit.components.data.model

import co.xendit.components.XenditComponentsPaymentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class BffChannelAvailabilityTest {

  @Test
  fun amountAvailabilityStatus_whenAmountIsNull_returnsAvailable() {
    val channel = createChannel(minAmount = "10.00", maxAmount = "100.00")

    assertEquals(AmountAvailabilityStatus.AVAILABLE, channel.amountAvailabilityStatus(amount = null))
    assertTrue(channel.isAvailableForAmount(amount = null))
  }

  @Test
  fun amountAvailabilityStatus_whenAmountIsBelowMin_returnsBelowMinAndUnavailable() {
    val channel = createChannel(minAmount = "10.00", maxAmount = "100.00")

    assertEquals(
      AmountAvailabilityStatus.BELOW_MIN,
      channel.amountAvailabilityStatus(amount = BigDecimal("9.99"))
    )
    assertFalse(channel.isAvailableForAmount(amount = BigDecimal("9.99")))
  }

  @Test
  fun amountAvailabilityStatus_whenAmountIsAboveMax_returnsAboveMaxAndUnavailable() {
    val channel = createChannel(minAmount = "10.00", maxAmount = "100.00")

    assertEquals(
      AmountAvailabilityStatus.ABOVE_MAX,
      channel.amountAvailabilityStatus(amount = BigDecimal("100.01"))
    )
    assertFalse(channel.isAvailableForAmount(amount = BigDecimal("100.01")))
  }

  @Test
  fun amountAvailabilityStatus_whenAmountMatchesMinOrMax_returnsAvailable() {
    val channel = createChannel(minAmount = "10.00", maxAmount = "100.00")

    assertEquals(
      AmountAvailabilityStatus.AVAILABLE,
      channel.amountAvailabilityStatus(amount = BigDecimal("10.00"))
    )
    assertEquals(
      AmountAvailabilityStatus.AVAILABLE,
      channel.amountAvailabilityStatus(amount = BigDecimal("100.00"))
    )
    assertTrue(channel.isAvailableForAmount(amount = BigDecimal("10.00")))
    assertTrue(channel.isAvailableForAmount(amount = BigDecimal("100.00")))
  }

  @Test
  fun amountAvailabilityStatus_whenOnlyMinExists_allowsAmountsAtOrAboveMin() {
    val channel = createChannel(minAmount = "10.00", maxAmount = null)

    assertEquals(
      AmountAvailabilityStatus.BELOW_MIN,
      channel.amountAvailabilityStatus(amount = BigDecimal("9.99"))
    )
    assertEquals(
      AmountAvailabilityStatus.AVAILABLE,
      channel.amountAvailabilityStatus(amount = BigDecimal("10.00"))
    )
    assertEquals(
      AmountAvailabilityStatus.AVAILABLE,
      channel.amountAvailabilityStatus(amount = BigDecimal("999.99"))
    )
  }

  @Test
  fun amountAvailabilityStatus_whenOnlyMaxExists_allowsAmountsAtOrBelowMax() {
    val channel = createChannel(minAmount = null, maxAmount = "100.00")

    assertEquals(
      AmountAvailabilityStatus.AVAILABLE,
      channel.amountAvailabilityStatus(amount = BigDecimal("0.01"))
    )
    assertEquals(
      AmountAvailabilityStatus.AVAILABLE,
      channel.amountAvailabilityStatus(amount = BigDecimal("100.00"))
    )
    assertEquals(
      AmountAvailabilityStatus.ABOVE_MAX,
      channel.amountAvailabilityStatus(amount = BigDecimal("100.01"))
    )
  }

  @Test
  fun amountAvailabilityStatus_whenSessionTypeIsSave_ignoresMinMaxLimits() {
    val channel = createChannel(minAmount = "10.00", maxAmount = "100.00")

    assertEquals(
      AmountAvailabilityStatus.AVAILABLE,
      channel.amountAvailabilityStatus(
        amount = BigDecimal("9.99"),
        sessionType = BffSessionType.SAVE
      )
    )
    assertEquals(
      AmountAvailabilityStatus.AVAILABLE,
      channel.amountAvailabilityStatus(
        amount = BigDecimal("100.01"),
        sessionType = BffSessionType.SAVE
      )
    )
    assertTrue(
      channel.isAvailableForAmount(
        amount = BigDecimal("9.99"),
        sessionType = BffSessionType.SAVE
      )
    )
    assertTrue(
      channel.isAvailableForAmount(
        amount = BigDecimal("100.01"),
        sessionType = BffSessionType.SAVE
      )
    )
  }

  @Test
  fun groupAmountAvailabilityStatus_whenAllChannelsAvailable_returnsAvailable() {
    val channels = listOf(
      createChannel(minAmount = "10.00", maxAmount = "100.00"),
      createChannel(minAmount = "1.00", maxAmount = "200.00")
    )

    assertEquals(
      AmountAvailabilityStatus.AVAILABLE,
      channels.groupAmountAvailabilityStatus(amount = BigDecimal("50.00"))
    )
  }

  @Test
  fun groupAmountAvailabilityStatus_whenAllChannelsAboveMax_returnsAboveMax() {
    val channels = listOf(
      createChannel(maxAmount = "40.00"),
      createChannel(maxAmount = "45.00")
    )

    assertEquals(
      AmountAvailabilityStatus.ABOVE_MAX,
      channels.groupAmountAvailabilityStatus(amount = BigDecimal("50.00"))
    )
  }

  @Test
  fun groupAmountAvailabilityStatus_whenAllChannelsBelowMin_returnsBelowMin() {
    val channels = listOf(
      createChannel(minAmount = "60.00"),
      createChannel(minAmount = "75.00")
    )

    assertEquals(
      AmountAvailabilityStatus.BELOW_MIN,
      channels.groupAmountAvailabilityStatus(amount = BigDecimal("50.00"))
    )
  }

  @Test
  fun groupAmountAvailabilityStatus_whenGroupHasMixedStatuses_returnsNull() {
    val channels = listOf(
      createChannel(minAmount = "60.00"),
      createChannel(maxAmount = "40.00")
    )

    assertNull(channels.groupAmountAvailabilityStatus(amount = BigDecimal("50.00")))
  }

  @Test
  fun groupAmountAvailabilityStatus_whenListIsEmptyOrAmountIsNull_returnsNull() {
    assertNull(emptyList<BffChannel>().groupAmountAvailabilityStatus(amount = BigDecimal("50.00")))
    assertNull(listOf(createChannel()).groupAmountAvailabilityStatus(amount = null))
  }

  @Test
  fun groupAmountAvailabilityStatus_whenSessionTypeIsSave_returnsAvailable() {
    val channels = listOf(
      createChannel(minAmount = "60.00"),
      createChannel(maxAmount = "40.00")
    )

    assertEquals(
      AmountAvailabilityStatus.AVAILABLE,
      channels.groupAmountAvailabilityStatus(
        amount = BigDecimal("50.00"),
        sessionType = BffSessionType.SAVE
      )
    )
  }

  private fun createChannel(
    minAmount: String? = null,
    maxAmount: String? = null
  ): BffChannel {
    return BffChannel(
      brandName = "Test Channel",
      brandLogoUrl = null,
      brandColor = "#000000",
      pmType = XenditComponentsPaymentType.EWALLET,
      uiGroup = "ewallet",
      channelCode = "TEST_CHANNEL",
      allowPayWithoutSave = true,
      allowSave = false,
      minAmount = minAmount?.let(::BigDecimal),
      maxAmount = maxAmount?.let(::BigDecimal),
      requiresCustomerDetails = false,
      card = null,
      form = null,
      instructions = null
    )
  }
}
