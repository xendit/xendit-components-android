package co.xendit.components.ui.method

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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.xendit.components.R
import co.xendit.components.data.model.BffBusiness
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.BffChannelUiGroup
import co.xendit.components.data.model.BffSession
import co.xendit.components.data.model.BffSessionAllowSavePaymentMethod
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.CardDetails
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.data.model.PaymentDraft
import co.xendit.components.XenditComponents
import co.xendit.components.ui.ChannelVariantChannels
import co.xendit.components.ui.card.CardPaymentUI
import co.xendit.components.ui.ewallet.EwalletPaymentUI
import co.xendit.components.ui.helper.SdkImageLoader
import co.xendit.components.ui.qrcode.QrPaymentUI
import co.xendit.components.ui.style.xenditAppearance
import co.xendit.components.ui.ui_util.CustomShape.createTopRoundedOpenShape
import coil.compose.AsyncImage

@Composable
internal fun PaymentMethodsUI(
  session: BffSession?,
  bffBusiness: BffBusiness?,
  merchantPreferredPaymentMethod: List<String>? = null,
  channels: List<BffChannel>,
  channelUiGroups: List<BffChannelUiGroup>? = null,
  channelVariantsByDisplayCode: Map<String, ChannelVariantChannels> = emptyMap(),
  expandedUiGroup: String?,
  selectedChannel: BffChannel?,
  paymentDrafts: Map<String, PaymentDraft> = emptyMap(),
  cardDetails: CardDetails?,
  installmentPlans: List<InstallmentPlan>?,
  sessionType: BffSessionType?,
  allowSavePaymentMethod: BffSessionAllowSavePaymentMethod?,
  onToggleGroup: (String) -> Unit,
  onSelectChannel: (String) -> Unit,
  onCardNumberChanged: (String) -> Unit,
  onFormChanged: (String?, Map<String, String>, List<ChannelFormField>, Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val isSaveOptionalSession =
    sessionType == BffSessionType.PAY && allowSavePaymentMethod == BffSessionAllowSavePaymentMethod.OPTIONAL
  fun canShowSaveCheckbox(channel: BffChannel?): Boolean {
    if (!isSaveOptionalSession || channel == null) return false
    return channel.allowSave || channelVariantsByDisplayCode[channel.channelCode]?.saveChannel != null
  }

  fun effectiveSelectedChannel(displaySelected: BffChannel?): BffChannel? {
    if (displaySelected == null) return null
    val draft = paymentDrafts[displaySelected.channelCode]
    val saveChecked = draft?.savePaymentMethod ?: false
    val variants = channelVariantsByDisplayCode[displaySelected.channelCode]
    return when {
      saveChecked && variants?.saveChannel != null -> variants.saveChannel
      !saveChecked && variants?.nonSaveChannel != null -> variants.nonSaveChannel
      else -> displaySelected
    }
  }

  val displaySelectedChannel = selectedChannel
  val effectiveSelected = effectiveSelectedChannel(displaySelectedChannel)
  val selectedDraft = displaySelectedChannel?.let { paymentDrafts[it.channelCode] }
  val uiGroupMetaById =
    remember(channelUiGroups) { channelUiGroups.orEmpty().associateBy { it.id } }
  val supportedUiGroups = remember { XenditComponents.UiGroup.SUPPORTED }

  val groups = remember(channels) {
    channels.groupBy { it.uiGroup }.filter { it.key in supportedUiGroups }
  }
  val filteredUiGroup = remember(groups.keys, merchantPreferredPaymentMethod, channelUiGroups) {
    val ordered =
      channelUiGroups
        ?.map { it.id }
        ?.filter { it in groups.keys }
        ?: groups.keys.toList()
    if (merchantPreferredPaymentMethod.isNullOrEmpty()) {
      ordered
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
        val groupMeta = uiGroupMetaById[uiGroup]
        val fallback = fallbackDisplayNameIconForUiGroup(uiGroup)
        val displayName = groupMeta?.label?.takeIf { it.isNotBlank() } ?: fallback.first
        val iconUrl = null //groupMeta?.iconUrl?.takeIf { it.isNotBlank() } cant use this now
        val groupChannels = groups[uiGroup].orEmpty()

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
              text = displayName,
              leftIcon = fallback.second,
              leftIconUrl = iconUrl,
              isExpanded = isExpanded,
              isSelected = isExpanded,
              onToggle = {
                onToggleGroup(uiGroup)
              }
            )

            // expanded content here
            if (isExpanded) {
              Spacer(modifier = Modifier.height(8.dp))
              when (uiGroup) {
                XenditComponents.UiGroup.CARDS -> {
                  val initialValues = selectedDraft?.formValues.orEmpty()
                  CardPaymentUI(
                    session = session,
                    channelData = effectiveSelected,
                    cardDetails = cardDetails,
                    initialValues = initialValues,
                    installmentPlans = installmentPlans,
                    onCardNumberChanged = onCardNumberChanged,
                    onFormStateChanged = { formValues, visibleFields, isSaveChecked ->
                      onFormChanged(
                        displaySelectedChannel?.channelCode,
                        formValues,
                        visibleFields,
                        isSaveChecked
                      )
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                    showSaveCheckbox = canShowSaveCheckbox(displaySelectedChannel)
                  )
                }

                XenditComponents.UiGroup.EWALLET -> {
                  val draft = selectedDraft
                  val showSaveCheckbox = canShowSaveCheckbox(displaySelectedChannel)
                  EwalletPaymentUI(
                    channels = groupChannels,
                    bffBusiness = bffBusiness,
                    selectedChannel = effectiveSelected,
                    initialValues = draft?.formValues.orEmpty(),
                    initialVisibleFields = draft?.visibleFields ?: emptyList(),
                    initialSaveChecked =
                      if (showSaveCheckbox) (draft?.savePaymentMethod ?: false) else false,
                    onSelectChannel = onSelectChannel,
                    onFormStateChanged = { formValues, visibleFields, isSaveChecked ->
                      onFormChanged(
                        displaySelectedChannel?.channelCode,
                        formValues,
                        visibleFields,
                        isSaveChecked
                      )
                    },
                    showSaveCheckbox = showSaveCheckbox,
                    modifier = Modifier.padding(bottom = 8.dp)
                  )
                }

                XenditComponents.UiGroup.QR_CODE -> {
                  QrPaymentUI(
                    channels = groupChannels,
                    selectedChannel = effectiveSelected,
                    onSelectChannel = onSelectChannel,
                    onFormStateChanged = { formValues, visibleFields ->
                      onFormChanged(
                        effectiveSelected?.channelCode,
                        formValues,
                        visibleFields,
                        false
                      )
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                  )
                }
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
  leftIconUrl: String?,
  isExpanded: Boolean,
  isSelected: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val context = LocalContext.current
  val imageLoader = remember { SdkImageLoader.get(context) }
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
    if (!leftIconUrl.isNullOrBlank()) {
      AsyncImage(
        model = leftIconUrl,
        imageLoader = imageLoader,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(20.dp)
      )
    } else {
      Icon(
        painter = painterResource(id = leftIcon),
        contentDescription = null,
        tint = contentColor
      )
    }

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

private fun fallbackDisplayNameIconForUiGroup(uiGroup: String): Pair<String, Int> {
  return when (uiGroup.lowercase()) {
    XenditComponents.UiGroup.CARDS -> "Cards" to R.drawable.ic_cards
    XenditComponents.UiGroup.EWALLET -> "E-Wallet" to R.drawable.ic_e_wallet
    XenditComponents.UiGroup.QR_CODE -> "QR Code" to R.drawable.ic_qris
    else -> uiGroup.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } to R.drawable.ic_cards // Fallback icon
  }
}
