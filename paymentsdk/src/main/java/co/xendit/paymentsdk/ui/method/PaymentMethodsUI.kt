package co.xendit.paymentsdk.ui.method

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.xendit.paymentsdk.data.model.BffChannel
import co.xendit.paymentsdk.data.model.BffSession
import co.xendit.paymentsdk.data.model.CardDetails
import co.xendit.paymentsdk.data.model.ChannelFormField
import co.xendit.paymentsdk.data.model.InstallmentPlan
import co.xendit.paymentsdk.ui.card.CardPaymentUI
import co.xendit.paymentsdk.ui.qrcode.QrPaymentUI
import co.xendit.paymentsdk.ui.style.xenditAppearance

val SUPPORTED_PAYMENT_METHOD = listOf("cards", "qr_code")

@Preview
@Composable
fun tryalaccordion() {
  val items = listOf("Cards", "E-Wallet", "QR Code")

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      // Main container border
      .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
      .clip(RoundedCornerShape(12.dp))
      .background(Color.White)
  ) {

    Box(
      modifier = Modifier
        .fillMaxWidth()
        // Apply a top border to every item EXCEPT the first one
        // to create the "stacked" effect
        .offset(y = 0.dp)
    ) {
      Column() {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Your Icon, Text, and Arrow here...
          Icon(Icons.Default.Menu, contentDescription = null)
          Text(
            text = "title",
            modifier = Modifier
              .weight(1f)
              .padding(start = 16.dp)
          )
          Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }

        // expanded content here

      }
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        // Apply a top border to every item EXCEPT the first one
        // to create the "stacked" effect
        .offset(y = 0.dp)
        .then(
          Modifier.border(
            width = 1.dp,
            color = Color(0xFFE0E0E0),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
          )
        )
    ) {
      Column() {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Your Icon, Text, and Arrow here...
          Icon(Icons.Default.Menu, contentDescription = null)
          Text(
            text = "title",
            modifier = Modifier
              .weight(1f)
              .padding(start = 16.dp)
          )
          Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }

        // expanded content here

      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        // Apply a top border to every item EXCEPT the first one
        // to create the "stacked" effect
        .offset(y = 0.dp)
        .then(
          Modifier.border(
            width = 1.dp,
            color = Color(0xFFE0E0E0),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
          )
        )
    ) {
      Column() {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Your Icon, Text, and Arrow here...
          Icon(Icons.Default.Menu, contentDescription = null)
          Text(
            text = "title",
            modifier = Modifier
              .weight(1f)
              .padding(start = 16.dp)
          )
          Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }

        // expanded content here

      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        // Apply a top border to every item EXCEPT the first one
        // to create the "stacked" effect
        .offset(y = 0.dp)
        .then(
          Modifier.border(
            width = 1.dp,
            color = Color(0xFFE0E0E0),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
          )
        )
    ) {
      Column() {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Your Icon, Text, and Arrow here...
          Icon(Icons.Default.Menu, contentDescription = null)
          Text(
            text = "title",
            modifier = Modifier
              .weight(1f)
              .padding(start = 16.dp)
          )
          Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }

        // expanded content here

      }
    }

  }

}

@Composable
fun PaymentMethodsUI(
  session: BffSession?,
  merchantPreferredPaymentMethod: List<String>? = null,
  channels: List<BffChannel>,
  expandedUiGroup: String?,
  selectedChannel: BffChannel?,
  cardDetails: CardDetails?,
  installmentPlans: List<InstallmentPlan>?,
  sessionType: String?,
  allowSavePaymentMethod: String?,
  onToggleGroup: (String) -> Unit,
  onSelectChannel: (String) -> Unit,
  onCardNumberChanged: (String) -> Unit,
  onFormChanged: (String, Map<String, String>, List<ChannelFormField>, Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val groups = remember(channels) {
    Log.d("GROUP PAYMENT", channels.groupBy { it.uiGroup }.keys.toString())
    channels.groupBy { it.uiGroup }.filter { it.key in SUPPORTED_PAYMENT_METHOD }
  }
  val filteredUiGroup = remember(groups.keys) {
    if (merchantPreferredPaymentMethod.isNullOrEmpty()) {
      groups.keys
    } else {
      merchantPreferredPaymentMethod.filter { it in groups.keys }
    }
  }

  Column() {
    Text(
      text = "Payment Method",
      style = MaterialTheme.typography.headlineSmall,
      color = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    )
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        // Main container border
        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
        .clip(RoundedCornerShape(12.dp))
        .background(Color.White)
    ) {
      val group = mutableListOf<String>()
      group.addAll(filteredUiGroup)
      group.addAll(filteredUiGroup)
      group.addAll(filteredUiGroup)

      group.forEachIndexed { index, uiGroup ->
        val isExpanded = expandedUiGroup == uiGroup
        val displayName = displayNameForUiGroup(uiGroup)
        val groupChannels = groups[uiGroup].orEmpty()
        val currentSelected =
          selectedChannel?.takeIf { it.uiGroup == uiGroup } ?: groupChannels.firstOrNull()

        Box(
          modifier = Modifier
            .fillMaxWidth()
            // Apply a top border to every item EXCEPT the first one
            // to create the "stacked" effect
            .offset(y = if (index > 0) (-1).dp else 0.dp)
            .then(
              if (index > 0) {
                Modifier.border(
                  width = 1.dp,
                  color = Color(0xFFE0E0E0),
                  shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
              } else Modifier
            )
        ) {
          Column() {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onToggleGroup(uiGroup)
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Your Icon, Text, and Arrow here...
              Icon(Icons.Default.Menu, contentDescription = null)
              Text(
                text = displayName,
                modifier = Modifier
                  .weight(1f)
                  .padding(start = 16.dp)
              )
              Icon(
                Icons.Default.KeyboardArrowDown,
                modifier = Modifier.rotate(if (isExpanded) 180f else 0f),
                contentDescription = null
              )
            }

            // expanded content here
            if (isExpanded) {
              Spacer(modifier = Modifier.height(8.dp))
              when {
                uiGroup == "cards" && currentSelected != null -> {
                  val showSaveCheckbox =
                    sessionType == "PAY" && allowSavePaymentMethod == "OPTIONAL"
                  CardPaymentUI(
                    session = session,
                    channelData = currentSelected,
                    cardDetails = cardDetails,
                    installmentPlans = installmentPlans,
                    onCardNumberChanged = onCardNumberChanged,
                    onFormStateChanged = { formValues, visibleFields, isSaveChecked ->
                      onFormChanged(
                        currentSelected.channelCode,
                        formValues,
                        visibleFields,
                        isSaveChecked
                      )
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                    showSaveCheckbox = showSaveCheckbox
                  )
                }

                uiGroup == "qr_code" && currentSelected != null -> {
                  QrPaymentUI(
                    channels = groupChannels,
                    selectedChannel = currentSelected,
                    onSelectChannel = onSelectChannel,
                    onFormStateChanged = { formValues, visibleFields ->
                      onFormChanged(currentSelected.channelCode, formValues, visibleFields, false)
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                  )
                }

                else -> {
                  Text(
                    text = "This payment method is not supported yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = appearance.colorTextSecondary
                      ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                  )
                }
              }
            }
          }

        }
//      Surface(
//        modifier = Modifier
//          .fillMaxWidth()
//          .padding(vertical = 6.dp),
//        shape = RoundedCornerShape(12.dp),
//        color = appearance.colorBackground ?: MaterialTheme.colorScheme.surface,
//        tonalElevation = 0.dp,
//        shadowElevation = 0.dp
//      ) {
//        Column(
//          modifier = Modifier
//            .fillMaxWidth()
//            .background(
//              (appearance.colorBorder ?: MaterialTheme.colorScheme.outline)
//            )
//            .padding(1.dp)
//            .background(
//              appearance.colorBackground ?: MaterialTheme.colorScheme.surface,
//              RoundedCornerShape(12.dp)
//            )
//        ) {
//          Row(
//            modifier = Modifier
//              .fillMaxWidth()
//              .clickable { onToggleGroup(uiGroup) }
//              .padding(horizontal = 16.dp, vertical = 14.dp),
//            verticalAlignment = Alignment.CenterVertically
//          ) {
//            Text(
//              text = displayName,
//              style = MaterialTheme.typography.titleMedium,
//              color = appearance.colorText ?: MaterialTheme.colorScheme.onSurface,
//              modifier = Modifier.weight(1f)
//            )
//            Icon(
//              imageVector = Icons.Filled.ArrowDropDown,
//              contentDescription = null,
//              tint = appearance.colorTextSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant,
//              modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
//            )
//          }
//
//
//        }
//      }
      }
    }
  }
}

private fun displayNameForUiGroup(uiGroup: String): String {
  return when (uiGroup.lowercase()) {
    "cards" -> "Cards"
    "ewallet", "e-wallet" -> "E-Wallet"
    "qrcode", "qr_code", "qr" -> "QR Code"
    else -> uiGroup.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
  }
}
