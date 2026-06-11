package co.xendit.components.data.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

internal class PaymentInstructionTabDeserializer : JsonDeserializer<PaymentInstructionTab> {
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
  ): PaymentInstructionTab {
    val obj = json.asJsonObject
    val title = obj["title"]?.asString.orEmpty()
    val contentElement = obj["content"]
    val blocks = parseBlocks(contentElement, context)
    return PaymentInstructionTab(title = title, content = blocks)
  }

  private fun parseBlocks(
    element: JsonElement?,
    context: JsonDeserializationContext
  ): List<PaymentInstructionStepBlock> {
    if (element == null || element.isJsonNull) return emptyList()
    return when {
      element.isJsonArray -> {
        element.asJsonArray.mapNotNull { item ->
          when {
            item.isJsonObject -> {
              val step = context.deserialize<PaymentInstructionStep>(item, PaymentInstructionStep::class.java)
              PaymentInstructionStepBlock(steps = listOf(step))
            }
            item.isJsonArray -> {
              val steps = collectSteps(item, context)
              PaymentInstructionStepBlock(steps = steps)
            }
            else -> null
          }
        }
      }
      element.isJsonObject -> {
        val step = context.deserialize<PaymentInstructionStep>(element, PaymentInstructionStep::class.java)
        listOf(PaymentInstructionStepBlock(steps = listOf(step)))
      }
      else -> emptyList()
    }
  }

  private fun collectSteps(
    element: JsonElement,
    context: JsonDeserializationContext
  ): List<PaymentInstructionStep> {
    if (!element.isJsonArray) return emptyList()
    val out = mutableListOf<PaymentInstructionStep>()
    element.asJsonArray.forEach { child ->
      when {
        child.isJsonObject -> out += context.deserialize<PaymentInstructionStep>(child, PaymentInstructionStep::class.java)
        child.isJsonArray -> out += collectSteps(child, context)
      }
    }
    return out
  }
}

