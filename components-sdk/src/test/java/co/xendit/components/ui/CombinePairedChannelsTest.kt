package co.xendit.components.ui

import co.xendit.components.data.model.BffChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinePairedChannelsTest {

  @Test
  fun combinePairedChannels_whenSaveAndNonSavePair_exist_combinesIntoSingleDisplayChannelAndVariantMap() {
    val nonSave =
      BffChannel(
        brandName = "OVO",
        brandLogoUrl = null,
        brandColor = "#000000",
        pmType = "EWALLET",
        uiGroup = "ewallet",
        channelCode = "EWALLET_OVO",
        allowPayWithoutSave = true,
        allowSave = false,
        minAmount = null,
        maxAmount = null,
        requiresCustomerDetails = false,
        card = null,
        form = null,
        instructions = null
      )
    val save =
      nonSave.copy(
        channelCode = "EWALLET_OVO_SAVE",
        allowSave = true
      )

    val result = combinePairedChannels(listOf(nonSave, save))

    assertEquals(1, result.channels.size)
    assertEquals(nonSave.channelCode, result.channels.first().channelCode)

    val variants = result.variantsByDisplayCode[nonSave.channelCode]
    requireNotNull(variants)
    assertEquals(save.channelCode, variants.saveChannel?.channelCode)
    assertEquals(nonSave.channelCode, variants.nonSaveChannel?.channelCode)
  }

  @Test
  fun combinePairedChannels_whenOnlyOneVariantProvided_keepsChannelAsIs_andNoVariantMapping() {
    val onlyNonSave =
      BffChannel(
        brandName = "DANA",
        brandLogoUrl = null,
        brandColor = "#000000",
        pmType = "EWALLET",
        uiGroup = "ewallet",
        channelCode = "EWALLET_DANA",
        allowPayWithoutSave = true,
        allowSave = false,
        minAmount = null,
        maxAmount = null,
        requiresCustomerDetails = false,
        card = null,
        form = null,
        instructions = null
      )

    val result = combinePairedChannels(listOf(onlyNonSave))

    assertEquals(listOf(onlyNonSave.channelCode), result.channels.map { it.channelCode })
    assertTrue(result.variantsByDisplayCode.isEmpty())
    assertNull(result.variantsByDisplayCode[onlyNonSave.channelCode])
  }
}

