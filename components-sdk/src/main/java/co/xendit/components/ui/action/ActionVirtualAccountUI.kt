package co.xendit.components.ui.action

import android.content.ClipData
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import co.xendit.components.R
import co.xendit.components.data.model.PaymentInstructionTab
import co.xendit.components.ui.helper.CurrencyUtil
import co.xendit.components.ui.helper.SdkImageLoader
import co.xendit.components.ui.style.xenditAppearance
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun ActionVirtualAccountUI(
  title: String?,
  subtitle: String?,
  channelName: String,
  channelLogoUrl: String?,
  virtualAccountNumber: String,
  merchantName: String?,
  amount: Long?,
  currency: String?,
  instructions: List<PaymentInstructionTab>?,
  onClose: () -> Unit,
  onPaymentMade: () -> Unit,
  snackbarHostState: SnackbarHostState? = null,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val context = LocalContext.current
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()
  val imageLoader = remember { SdkImageLoader.get(context) }
  val formattedAmount = remember(amount, currency) { CurrencyUtil.formatAmount(amount, currency) }
  val copiedText = stringResource(R.string.sessionaction_va_copied_to_clipboard)

  val sections = remember(instructions) { instructions.orEmpty() }
  var selectedTabIndex by remember(sections.size) { mutableIntStateOf(0) }
  val effectiveSelectedIndex = selectedTabIndex.coerceIn(0, (sections.size - 1).coerceAtLeast(0))
  val selectedSection = sections.getOrNull(effectiveSelectedIndex)

  Surface(
    modifier = Modifier
      .fillMaxWidth(),
    color = appearance.colorBackground,
    tonalElevation = 0.dp
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
          model = channelLogoUrl,
          imageLoader = imageLoader,
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .align(Alignment.Center)
            .height(28.dp)
        )
        IconButton(
          onClick = onClose,
          modifier = Modifier.align(Alignment.CenterEnd)
        ) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = appearance.colorTextSecondary
          )
        }
      }

      Text(
        text = title?.takeIf { it.isNotBlank() } ?: channelName,
        style = MaterialTheme.typography.titleLarge,
        color = appearance.colorText,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )

      if (!subtitle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = appearance.colorTextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = 2.dp,
            color = appearance.colorPrimary,
            shape = RoundedCornerShape(appearance.borderRadius)
          ),
        shape = RoundedCornerShape(appearance.borderRadius),
        color = appearance.colorBackground,
        tonalElevation = 0.dp
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.sessionaction_va_virtual_account_number),
                style = MaterialTheme.typography.bodySmall,
                color = appearance.colorTextSecondary
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = virtualAccountNumber,
                style = MaterialTheme.typography.titleMedium,
                color = appearance.colorText
              )
              if (!merchantName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = merchantName,
                  style = MaterialTheme.typography.bodySmall,
                  color = appearance.colorTextSecondary
                )
              }
            }

            OutlinedButton(
              onClick = {
                copyToClipboard(
                  scope,
                  clipboard,
                  virtualAccountNumber,
                  snackbarHostState,
                  copiedText
                )
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFAFAFA),
                contentColor = appearance.colorText
              ),
              shape = RoundedCornerShape(999.dp)
            ) {
              Text(
                text = stringResource(R.string.sessionaction_va_copy_number),
                style = MaterialTheme.typography.titleSmall
              )
            }
          }

          if (formattedAmount.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.sessionaction_va_amount_to_pay),
                  style = MaterialTheme.typography.bodySmall,
                  color = appearance.colorTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = formattedAmount,
                  style = MaterialTheme.typography.titleMedium,
                  color = appearance.colorText
                )
              }

              OutlinedButton(
                onClick = {
                  copyToClipboard(scope, clipboard, amount.toString(), snackbarHostState, copiedText)
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFFFAFAFA),
                  contentColor = appearance.colorText
                ),
                shape = RoundedCornerShape(999.dp)
              ) {
                Text(
                  text = stringResource(R.string.sessionaction_va_copy_amount),
                  style = MaterialTheme.typography.titleSmall
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          OutlinedButton(
            onClick = onPaymentMade,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
              containerColor = appearance.colorBackground,
              contentColor = appearance.colorText
            ),
            shape = RoundedCornerShape(appearance.borderRadius)
          ) {
            Text(
              text = stringResource(R.string.sessionaction_simulate_payment),
              style = MaterialTheme.typography.titleSmall
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.sessionaction_simulate_payment_instructions),
            style = MaterialTheme.typography.bodySmall,
            color = appearance.colorTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      if (sections.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Column(
          modifier = Modifier
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

          val content = selectedSection?.content.orEmpty()
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val textSteps =
              content.filter { it.type.equals("text", true) && !it.text.isNullOrBlank() }
                .mapNotNull { it.text }
            textSteps.forEachIndexed { index, text ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
              ) {
                Text(
                  text = "${index + 1}.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = appearance.colorText,
                  modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                  text = parseBoldTags(text),
                  style = MaterialTheme.typography.bodyMedium,
                  color = appearance.colorText
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun copyToClipboard(
  scope: CoroutineScope,
  clipboard: Clipboard,
  value: String,
  snackbarHostState: SnackbarHostState?,
  copiedText: String
) {
  if (value.isBlank()) return
  scope.launch {
    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, value)))
    snackbarHostState?.showSnackbar(copiedText)
  }
}

private fun parseBoldTags(text: String): AnnotatedString {
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
