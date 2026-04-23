package co.xendit.paymentsdk.ui.method

import android.util.Log
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.xendit.paymentsdk.R
import co.xendit.paymentsdk.data.model.BffChannel
import co.xendit.paymentsdk.data.model.BffSession
import co.xendit.paymentsdk.data.model.BffSessionAllowSavePaymentMethod
import co.xendit.paymentsdk.data.model.BffSessionType
import co.xendit.paymentsdk.data.model.CardDetails
import co.xendit.paymentsdk.data.model.ChannelFormField
import co.xendit.paymentsdk.data.model.InstallmentPlan
import co.xendit.paymentsdk.ui.card.CardPaymentUI
import co.xendit.paymentsdk.ui.qrcode.QrPaymentUI
import co.xendit.paymentsdk.ui.style.xenditAppearance
import co.xendit.paymentsdk.ui.ui_util.CustomShape.createTopRoundedOpenShape

private data class PaymentMethodRenderer(
  val uiGroup: String,
  val content: @Composable (groupChannels: List<BffChannel>, selectedChannel: BffChannel) -> Unit
)

@Composable
internal fun PaymentMethodsUI(
  session: BffSession?,
  merchantPreferredPaymentMethod: List<String>? = null,
  channels: List<BffChannel>,
  expandedUiGroup: String?,
  selectedChannel: BffChannel?,
  cardDetails: CardDetails?,
  installmentPlans: List<InstallmentPlan>?,
  sessionType: BffSessionType?,
  allowSavePaymentMethod: BffSessionAllowSavePaymentMethod?,
  onToggleGroup: (String) -> Unit,
  onSelectChannel: (String) -> Unit,
  onCardNumberChanged: (String) -> Unit,
  onFormChanged: (String, Map<String, String>, List<ChannelFormField>, Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val rendererMap =
    listOf(
      PaymentMethodRenderer(uiGroup = "cards") { _, currentSelected ->
        val showSaveCheckbox = sessionType == BffSessionType.PAY && allowSavePaymentMethod == BffSessionAllowSavePaymentMethod.OPTIONAL
        CardPaymentUI(
          session = session,
          channelData = currentSelected,
          cardDetails = cardDetails,
          installmentPlans = installmentPlans,
          onCardNumberChanged = onCardNumberChanged,
          onFormStateChanged = { formValues, visibleFields, isSaveChecked ->
            onFormChanged(currentSelected.channelCode, formValues, visibleFields, isSaveChecked)
          },
          modifier = Modifier.padding(bottom = 8.dp),
          showSaveCheckbox = showSaveCheckbox
        )
      },
      PaymentMethodRenderer(uiGroup = "qr_code") { groupChannels, currentSelected ->
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
    ).associateBy { it.uiGroup }

  val groups = remember(channels, rendererMap.keys) {
    Log.d("GROUP PAYMENT", channels.groupBy { it.uiGroup }.keys.toString())
    channels.groupBy { it.uiGroup }.filter { it.key in rendererMap.keys }
  }
  val filteredUiGroup = remember(groups.keys) {
    if (merchantPreferredPaymentMethod.isNullOrEmpty()) {
      groups.keys
    } else {
      merchantPreferredPaymentMethod.filter { it in groups.keys }
    }
  }

  Column() {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .border(
          1.dp,
          appearance.colorBorder,
          RoundedCornerShape(appearance.borderRadius)
        )
        .clip(RoundedCornerShape(appearance.borderRadius))
        .background(appearance.colorBackground)
    ) {
      filteredUiGroup.forEachIndexed { index, uiGroup ->
        val isExpanded = expandedUiGroup == uiGroup
        val displayNameIcon = displayNameIconForUiGroup(uiGroup)
        val groupChannels = groups[uiGroup].orEmpty()
        val currentSelected =
          selectedChannel?.takeIf { it.uiGroup == uiGroup } ?: groupChannels.firstOrNull()

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .offset(y = if (index > 0) (-1).dp else 0.dp)
            .then(
              if (index > 0) {
                Modifier.border(
                  width = 1.dp,
                  color = appearance.colorBorder,
                  shape = createTopRoundedOpenShape(appearance.borderRadius)
                )
              } else Modifier
            )
        ) {
          Column() {
            SelectableHeaderItem(
              text = displayNameIcon.first,
              leftIcon = displayNameIcon.second,
              isExpanded = isExpanded,
              isSelected = isExpanded,
              onToggle = {
                onToggleGroup(uiGroup)
              }
            )

            // expanded content here
            if (isExpanded) {
              Spacer(modifier = Modifier.height(8.dp))
              val renderer = rendererMap[uiGroup]
              if (renderer != null && currentSelected != null) {
                renderer.content(groupChannels, currentSelected)
              } else {
                Text(
                  text = "This payment method is not supported yet.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = appearance.colorTextSecondary,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
              }
            }
          }

        }
      }
    }
  }
}

@Composable
private fun SelectableHeaderItem(
  text: String,
  @DrawableRes leftIcon: Int,
  isExpanded: Boolean,
  isSelected: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val activeColor = appearance.colorPrimary
  val inactiveColor = MaterialTheme.colorScheme.onSurface

  val contentColor = if (isSelected) activeColor else inactiveColor
  val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onToggle() }
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      painter = painterResource(id = leftIcon),
      contentDescription = null,
      tint = contentColor
    )

    Text(
      text = text,
      color = contentColor,
      fontWeight = fontWeight,
      modifier = Modifier
        .weight(1f)
        .padding(start = 16.dp)
    )

    Icon(
      imageVector = Icons.Default.KeyboardArrowDown,
      contentDescription = null,
      tint = contentColor,
      modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
    )
  }
}

private fun displayNameIconForUiGroup(uiGroup: String): Pair<String, Int> {
  return when (uiGroup.lowercase()) {
    "cards" -> "Cards" to R.drawable.ic_cards
    "ewallet", "e-wallet" -> "E-Wallet" to R.drawable.ic_e_wallet
    "qrcode", "qr_code", "qr" -> "QR Code" to R.drawable.ic_qris
    else -> uiGroup.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } to R.drawable.ic_cards // Fallback icon
  }
}
