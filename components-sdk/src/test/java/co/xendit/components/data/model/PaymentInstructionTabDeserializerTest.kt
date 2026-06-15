package co.xendit.components.data.model

import co.xendit.components.core.CoreSdkComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PaymentInstructionTabDeserializerTest {
  @Test
  fun deserialize_whenContentContainsNestedArrays_parsesIntoBlocks() {
    val json =
      """
      {
        "title": "Payment Instructions",
        "content": [
          [
            { "type": "text", "text": "Note" },
            { "type": "bullets", "items": ["A", "B"] }
          ],
          { "type": "text", "text": "Step 2" }
        ]
      }
      """.trimIndent()

    val tab = CoreSdkComponent.gson.fromJson(json, PaymentInstructionTab::class.java)
    assertNotNull(tab)
    assertEquals("Payment Instructions", tab.title)
    assertEquals(2, tab.content.size)
    assertEquals(2, tab.content[0].steps.size)
    assertEquals("text", tab.content[0].steps[0].type)
    assertEquals("bullets", tab.content[0].steps[1].type)
    assertEquals(1, tab.content[1].steps.size)
    assertEquals("Step 2", tab.content[1].steps[0].text)
  }

  @Test
  fun deserialize_whenContentIsObject_wrapsIntoSingleBlock() {
    val json =
      """
      {
        "title": "Payment Instructions",
        "content": { "type": "text", "text": "Only step" }
      }
      """.trimIndent()

    val tab = CoreSdkComponent.gson.fromJson(json, PaymentInstructionTab::class.java)
    assertNotNull(tab)
    assertEquals(1, tab.content.size)
    assertEquals(1, tab.content[0].steps.size)
    assertEquals("Only step", tab.content[0].steps[0].text)
  }
}

