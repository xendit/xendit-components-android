package co.xendit.components.ui.action

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import co.xendit.components.data.model.PaymentInstructionStep
import co.xendit.components.data.model.PaymentInstructionTab
import co.xendit.components.ui.style.xenditAppearance

@Composable
internal fun PaymentInstructionsContent(
  instructions: List<PaymentInstructionTab>?,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val sections = remember(instructions) { instructions.orEmpty() }
  var selectedTabIndex by remember(sections.size) { mutableIntStateOf(0) }
  val effectiveSelectedIndex = selectedTabIndex.coerceIn(0, (sections.size - 1).coerceAtLeast(0))
  val selectedSection = sections.getOrNull(effectiveSelectedIndex)

  if (sections.isEmpty()) return

  Column(
    modifier = modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
  ) {
    if (sections.size > 1) {
      SecondaryTabRow(
        selectedTabIndex = effectiveSelectedIndex,
        containerColor = appearance.colorBackground,
        contentColor = appearance.colorPrimary
      ) {
        sections.forEachIndexed { index, section ->
          Tab(
            selected = effectiveSelectedIndex == index,
            onClick = { selectedTabIndex = index },
            text = { Text(section.title) }
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }

    val blocks = selectedSection?.content.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      blocks.forEachIndexed { index, block ->
        InstructionBlock(
          number = index + 1,
          steps = block.steps
        )
      }
    }
  }
}

@Composable
private fun InstructionBlock(
  number: Int,
  steps: List<PaymentInstructionStep>,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val leadingText =
    steps.firstOrNull { it.type.equals("text", true) && !it.text.isNullOrBlank() }?.text
  val remaining =
    if (leadingText == null) steps else steps.dropWhile { it.type.equals("text", true) && it.text == leadingText }

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = "$number.",
      style = MaterialTheme.typography.bodyMedium,
      color = appearance.colorText,
      modifier = Modifier.padding(end = 8.dp)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      if (!leadingText.isNullOrBlank()) {
        Text(
          text = parseBoldTags(leadingText),
          style = MaterialTheme.typography.bodyMedium,
          color = appearance.colorText
        )
      }

      remaining.forEach { step ->
        when (step.type.lowercase()) {
          "text" -> {
            val text = step.text
            if (!text.isNullOrBlank()) {
              Text(
                text = parseBoldTags(text),
                style = MaterialTheme.typography.bodyMedium,
                color = appearance.colorText
              )
            }
          }

          "bullets" -> {
            val items = step.items.orEmpty().filter { it.isNotBlank() }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              items.forEach { item ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.Top
                ) {
                  Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = appearance.colorText,
                    modifier = Modifier.padding(end = 8.dp)
                  )
                  Text(
                    text = parseBoldTags(item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = appearance.colorText
                  )
                }
              }
            }
          }

          "heading" -> {
            val heading = step.heading
            if (!heading.isNullOrBlank()) {
              Text(
                text = parseBoldTags(heading),
                style = MaterialTheme.typography.bodyMedium,
                color = appearance.colorText,
                fontWeight = FontWeight.Bold
              )
            }
          }

          else -> Unit
        }
      }
    }
  }
}

internal fun parseBoldTags(text: String): AnnotatedString {
  val regex = Regex("<b>(.*?)</b>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
  return buildAnnotatedString {
    var currentIndex = 0
    regex.findAll(text).forEach { match ->
      val start = match.range.first
      val end = match.range.last + 1
      if (start > currentIndex) {
        append(text.substring(currentIndex, start))
      }
      val boldText = match.groups[1]?.value.orEmpty()
      withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append(boldText)
      }
      currentIndex = end
    }
    if (currentIndex < text.length) {
      append(text.substring(currentIndex))
    }
  }
}

