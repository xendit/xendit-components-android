package co.xendit.components.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import co.xendit.components.R
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.BffSession
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.ui.components.molecule.ChannelLogo
import co.xendit.components.ui.components.molecule.CheckboxWithText
import co.xendit.components.ui.components.molecule.DashedDivider
import co.xendit.components.ui.components.molecule.XenditDropdownField
import co.xendit.components.ui.helper.SdkImageLoader
import co.xendit.components.ui.style.xenditAppearance
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentChannelSelectionUI(
  headerText: String,
  placeholderText: String,
  channels: List<BffChannel>,
  session: BffSession? = null,
  selectedChannel: BffChannel?,
  contentChannel: BffChannel? = selectedChannel,
  initialValues: Map<String, String> = emptyMap(),
  initialVisibleFields: List<ChannelFormField> = emptyList(),
  initialSaveChecked: Boolean = false,
  onSelectChannel: (String) -> Unit,
  onFormStateChanged: (Map<String, String>, List<ChannelFormField>, Boolean) -> Unit = { _, _, _ -> },
  onSaveCheck: (Boolean, Map<String, String>, List<ChannelFormField>) -> Unit =
    { isSaveChecked, values, visibleFields ->
      onFormStateChanged(values, visibleFields, isSaveChecked)
    },
  saveCheckboxText: String? = null,
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
    contentChannel?.channelCode
  ) {
    formValues.clear()
    formValues.putAll(initialValues)
    visibleFields.value = initialVisibleFields
    isSaveChecked.value = initialSaveChecked
    if (contentChannel != null) {
      onFormStateChanged(formValues.toMap(), visibleFields.value, isSaveChecked.value)
    }
  }

  Column(
    modifier = modifier
      .padding(horizontal = 16.dp)
      .padding(bottom = 12.dp)
  ) {
    Text(
      text = headerText,
      style = MaterialTheme.typography.bodyLarge,
      color = appearance.colorText
    )
    Spacer(modifier = Modifier.height(8.dp))

    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { if (channels.size > 1) expanded = !expanded },
      modifier = Modifier.fillMaxWidth()
    ) {
      XenditDropdownField(
        value = selectedChannel?.brandName ?: "",
        placeholder = placeholderText,
        isExpanded = expanded,
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
            enabled = channels.size > 1
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
                Row {
                  if (channel.brandLogoUrl != null) {
                    ChannelLogo(
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

    val channelFormFields = contentChannel?.form.orEmpty()
    if (channelFormFields.isNotEmpty()) {
      Spacer(modifier = Modifier.height(12.dp))
      key(contentChannel?.channelCode) {
        DynamicForm(
          session = session,
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

    if (!saveCheckboxText.isNullOrBlank() && contentChannel != null) {
      Spacer(modifier = Modifier.height(16.dp))
      CheckboxWithText(
        checked = isSaveChecked.value,
        text = saveCheckboxText,
        onCheckedChange = { nextChecked ->
          isSaveChecked.value = nextChecked
          onSaveCheck(isSaveChecked.value, formValues.toMap(), visibleFields.value)
        },
        textColor = appearance.colorText
      )
    }

    if (!contentChannel?.instructions.isNullOrEmpty()) {
      Spacer(modifier = Modifier.height(16.dp))
      DashedDivider(
        modifier = Modifier.fillMaxWidth(),
        color = appearance.colorBorder
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
          painter = painterResource(R.drawable.img_phone_app),
          contentDescription = null,
          modifier = Modifier.size(60.dp)
        )
        Column {
          contentChannel.instructions.forEachIndexed { index, instruction ->
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
