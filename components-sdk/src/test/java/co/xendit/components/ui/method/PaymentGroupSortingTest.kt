package co.xendit.components.ui.method

import co.xendit.components.XenditComponentsPaymentType
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.BffChannelUiGroup
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PaymentGroupSortingTest {

  // Simple mock data classes for testing purposes
  private fun createMockChannel(
    id: String,
    uiGroup: String,
    pmType: XenditComponentsPaymentType?
  ): BffChannel {
    return BffChannel(
      brandName = "Brand $id",
      brandLogoUrl = null,
      brandColor = "#FFFFFF",
      pmType = pmType,
      uiGroup = uiGroup,
      channelCode = "MOCK_CODE_$id",
      allowPayWithoutSave = true,
      allowSave = false,
      minAmount = BigDecimal.ZERO,
      maxAmount = BigDecimal.TEN,
      requiresCustomerDetails = false,
      card = null,
      form = null,
      instructions = null
    )
  }
  @Test
  fun `processAndOrderUiGroups with 5 data elements and 6 merchant preferences`() {
    // Arrange: 5 Channels total (including one with a null pmType to test safety)
    val channels = listOf(
      createMockChannel(id = "1", uiGroup = "group_qr",       pmType = XenditComponentsPaymentType.QR_CODE),
      createMockChannel(id = "2", uiGroup = "group_cards",    pmType = XenditComponentsPaymentType.CARDS),
      createMockChannel(id = "3", uiGroup = "group_va",       pmType = XenditComponentsPaymentType.VIRTUAL_ACCOUNT),
      createMockChannel(id = "4", uiGroup = "group_wallet",   pmType = XenditComponentsPaymentType.EWALLET),
      createMockChannel(id = "5", uiGroup = "group_broken",   pmType = null) // ❌ Null pmType: Should be filtered out
    )

    // Merchant has 6 preferred methods in mind
    val merchantPreferences = listOf(
      XenditComponentsPaymentType.VIRTUAL_ACCOUNT, // 🎯 Becomes Index 0 after sanitizing
      XenditComponentsPaymentType.QR_CODE,         // 🎯 Becomes Index 1 after sanitizing
      XenditComponentsPaymentType.CARDS,           // 🎯 Becomes Index 2 after sanitizing
      XenditComponentsPaymentType.EWALLET          // 🎯 Becomes Index 3 after sanitizing
    )

    // Fallback Master Layout Order sequence
    val masterLayoutOrder = listOf(
      BffChannelUiGroup("group_wallet", label = "E-Wallet"),
      BffChannelUiGroup("group_cards", label = "Credit Card"),
      BffChannelUiGroup("group_qr", label = "QR Code"),
      BffChannelUiGroup("group_va", label = "Virtual Account")
    )

    // The SDK active supported payment list (Missing the pay later or unknown options)
    val supportedTypes = listOf(
      XenditComponentsPaymentType.VIRTUAL_ACCOUNT,
      XenditComponentsPaymentType.QR_CODE,
      XenditComponentsPaymentType.CARDS,
      XenditComponentsPaymentType.EWALLET
    )

    // Act
    val (groups, orderedKeys) = processAndOrderUiGroups(
      channels = channels,
      merchantPreferredPaymentMethod = merchantPreferences,
      channelUiGroups = masterLayoutOrder,
      supportedPaymentTypes = supportedTypes
    )

    // Assert:
    // Let's analyze how the index pairs [Merchant Index, Master Layout Index] rank:
    // 1. group_va     -> pmType "VIRTUAL_ACCOUNT" -> Merchant Index 0, Master Index 3 -> [0, 3] (1st place)
    // 2. group_qr     -> pmType "QR_CODE"         -> Merchant Index 1, Master Index 2 -> [1, 2] (2nd place)
    // 3. group_cards  -> pmType "CARDS"           -> Merchant Index 2, Master Index 1 -> [2, 1] (3rd place)
    // 4. group_wallet -> pmType EWALLET           -> Merchant Index 3, Master Index 0 -> [3, 0] (4th place)
    // 5. group_broken -> pmType null is completely dropped.

    val expectedOrder = listOf("group_va", "group_qr", "group_cards", "group_wallet")
    assertEquals(expectedOrder, orderedKeys)

    // Verify that the invalid channel was dropped and we only have 4 active groups mapped
    assertEquals(4, groups.keys.size)
  }
}
