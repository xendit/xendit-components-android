package co.xendit.components.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.xendit.components.data.model.BffItem

@Composable
internal fun OrderSummaryUI(
  referenceId: String?,
  currency: String?,
  amount: Long?,
  items: List<BffItem>?,
  modifier: Modifier = Modifier
) {
  val currencySymbol = when (currency) {
    "IDR" -> "Rp"
    "USD" -> "$"
    "PHP" -> "₱"
    else -> currency ?: ""
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "Your order summary",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFFF9F9F9))
        .padding(16.dp)
    ) {
      Column {
        if (!items.isNullOrEmpty()) {
          items.forEach { item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            ) {
              Text(
                text = item.name ?: "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = "$currencySymbol${item.netUnitAmount ?: 0}",
                style = MaterialTheme.typography.bodyMedium
              )
            }
          }
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 12.dp),
          color = Color.LightGray,
          thickness = 1.dp
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Total",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
          )
          Text(
            text = "$currencySymbol${amount ?: 0}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "This is a demonstration of the Xendit Components SDK. No actual payment will be processed.",
      style = MaterialTheme.typography.bodySmall,
      color = Color.Gray,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 16.dp)
    )
  }
}
