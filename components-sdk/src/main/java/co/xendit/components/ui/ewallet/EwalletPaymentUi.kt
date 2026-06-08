package co.xendit.components.ui.ewallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.xendit.components.R
import co.xendit.components.data.model.BffBusiness
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.ui.components.DynamicForm
import co.xendit.components.ui.components.molecule.CheckboxWithText
import co.xendit.components.ui.components.molecule.DashedDivider
import co.xendit.components.ui.components.molecule.XenditDropdownField
import co.xendit.components.ui.helper.SdkImageLoader
import co.xendit.components.ui.style.xenditAppearance
import coil.ImageLoader
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EwalletPaymentUI(
  channels: List<BffChannel>,
  bffBusiness: BffBusiness?,
  selectedChannel: BffChannel?,
  effectiveChannel: BffChannel? = selectedChannel,
  initialValues: Map<String, String> = emptyMap(),
  initialVisibleFields: List<ChannelFormField> = emptyList(),
  initialSaveChecked: Boolean = false,
  onSelectChannel: (String) -> Unit,
  onFormStateChanged: (Map<String, String>, List<ChannelFormField>, Boolean) -> Unit = { _, _, _ -> },
  onSaveCheck: (Boolean, Map<String, String>, List<ChannelFormField>) -> Unit = { _, _, _ -> },
  showSaveCheckbox: Boolean = false,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val appearance = xenditAppearance
  var expanded by remember { mutableStateOf(false) }
  val formValues = remember { mutableStateMapOf<String, String>() }
  val visibleFields = remember { mutableStateOf<List<ChannelFormField>>(emptyList()) }
  val imageLoader = remember { SdkImageLoader.get(context) }
  val isSaveChecked = remember { mutableStateOf(false) }

  LaunchedEffect(
    effectiveChannel?.channelCode,
    initialValues,
    initialVisibleFields,
    initialSaveChecked
  ) {
    formValues.clear()
    formValues.putAll(initialValues)
    visibleFields.value = initialVisibleFields
    isSaveChecked.value = initialSaveChecked
    if (effectiveChannel != null) {
      onFormStateChanged(formValues.toMap(), visibleFields.value, isSaveChecked.value)
    }
  }

  Column(
    modifier = modifier
      .padding(horizontal = 16.dp)
      .padding(bottom = 12.dp)
  ) {
    Text(
      text = stringResource(id = R.string.ewallet_pay_with),
      style = MaterialTheme.typography.bodyLarge,
      color = appearance.colorText
    )
    Spacer(modifier = Modifier.height(8.dp))

    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = {},
      modifier = Modifier.fillMaxWidth()
    ) {
      XenditDropdownField(
        value = selectedChannel?.brandName ?: "",
        placeholder = stringResource(id = R.string.ewallet_select_placeholder),
        isExpanded = expanded,
        onClick = {
          if (channels.size > 1) {
            expanded = if (expanded) false else true
          }
        },
        enabled = channels.size > 1,
        leadingContent = selectedChannel?.let { channel ->
          {
            Row(
              modifier = Modifier.height(IntrinsicSize.Min),
              verticalAlignment = Alignment.CenterVertically
            ) {
              AsyncImage(
                model = channel.brandLogoUrl,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 36.dp, height = 24.dp)
              )
              VerticalDivider(
                modifier = Modifier.padding(horizontal = 8.dp),
                thickness = 1.dp,
                color = appearance.colorBorder
              )
            }
          }
        },
        modifier = Modifier
          .menuAnchor(
            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
            enabled = false
          )
          .fillMaxWidth()
      )

      if (channels.size > 1) {
        ExposedDropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false }
        ) {
          channels.forEach { channel ->
            DropdownMenuItem(
              text = {
                Row() {
                  if (channel.brandLogoUrl != null) {
                    EwalletLogo(
                      logoUrl = channel.brandLogoUrl,
                      imageLoader = imageLoader,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                  }
                  Text(channel.brandName)
                }

              },
              onClick = {
                onSelectChannel(channel.channelCode)
                expanded = false
              }
            )
          }
        }
      }
    }

    val channelFormFields = effectiveChannel?.form.orEmpty()
    if (channelFormFields.isNotEmpty()) {
      Spacer(modifier = Modifier.height(12.dp))
      key(effectiveChannel?.channelCode) {
        DynamicForm(
          fields = channelFormFields,
          cardDetails = null,
          initialValues = initialValues,
          installmentPlans = null,
          onValuesChanged = { updated ->
            formValues.clear()
            formValues.putAll(updated)
            onFormStateChanged(formValues.toMap(), visibleFields.value, isSaveChecked.value)
          },
          onCardNumberChanged = {},
          onVisibleFieldsChanged = {
            visibleFields.value = it
            onFormStateChanged(formValues.toMap(), visibleFields.value, isSaveChecked.value)
          },
          bffCardInfo = null
        )
      }
    }

    if (showSaveCheckbox && effectiveChannel != null) {
      val channel = effectiveChannel
      Spacer(modifier = Modifier.height(16.dp))
      CheckboxWithText(
        checked = isSaveChecked.value,
        text = stringResource(
          id = R.string.ewallet_link_for_future_purchase,
          channel.brandName,
          bffBusiness?.name.orEmpty()
        ),
        onCheckedChange = { nextChecked ->
          isSaveChecked.value = nextChecked
          onSaveCheck(isSaveChecked.value, formValues.toMap(), visibleFields.value)
        },
        textColor = appearance.colorText
      )
    }

    if (!effectiveChannel?.instructions.isNullOrEmpty()) {
      Spacer(modifier = Modifier.height(16.dp))
      DashedDivider(
        modifier = Modifier
          .fillMaxWidth(),
        color = appearance.colorBorder
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Image(
          painter = painterResource(R.drawable.img_phone_app),
          contentDescription = null,
          modifier = Modifier.size(60.dp)
        )
        Column() {
          effectiveChannel.instructions.forEachIndexed { index, instruction ->
            Text(
              text = instruction,
              style = MaterialTheme.typography.titleSmall.takeIf { index == 0 }
                ?: MaterialTheme.typography.bodySmall,
              color = appearance.colorText.takeIf { index == 0 } ?: appearance.colorTextSecondary
            )
          }
        }
      }
    }

  }
}

@Composable
internal fun EwalletLogo(
  logoUrl: String,
  imageLoader: ImageLoader,
) {
  val appearance = xenditAppearance

  Box(
    modifier = Modifier
      .size(width = 36.dp, height = 24.dp)
      // 1. Apply border and background to the container
      .border(
        width = 1.dp,
        color = appearance.colorBorder,
        shape = RoundedCornerShape(4.dp)
      )
      .background(
        color = appearance.colorBackground,
        shape = RoundedCornerShape(4.dp)
      )
      .clip(RoundedCornerShape(4.dp)),
    contentAlignment = Alignment.Center
  ) {
    AsyncImage(
      model = logoUrl,
      imageLoader = imageLoader,
      contentDescription = null,
      // 2. Use Fit or Inside to prevent "trimming"
      contentScale = ContentScale.Fit,
      modifier = Modifier
        // 3. Add small padding so the logo doesn't touch the border
        .padding(4.dp)
    )
  }
}
