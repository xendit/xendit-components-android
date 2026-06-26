package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.xendit.components.data.model.DropdownOption
import co.xendit.components.ui.style.xenditAppearance

@Composable
internal fun XenditDropdownHeaderField(
  value: String,
  placeholder: String,
  isExpanded: Boolean,
  modifier: Modifier = Modifier,
  shape: Shape? = null,
  borderColor: Color? = null,
  contentColor: Color? = null,
  placeholderColor: Color? = null,
  leadingContent: (@Composable (() -> Unit))? = null,
) {
  val appearance = xenditAppearance
  val resolvedShape = shape ?: MaterialTheme.shapes.small
  val resolvedBorderColor = borderColor ?: appearance.colorBorder
  val resolvedContentColor = contentColor ?: appearance.colorText
  val resolvedPlaceholderColor = placeholderColor ?: appearance.colorTextPlaceholder

  Row(
    modifier = modifier
      .heightIn(min = 56.dp)
      .clip(resolvedShape)
      .background(appearance.colorBackground, resolvedShape)
      .border(1.dp, resolvedBorderColor, resolvedShape)
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    leadingContent?.invoke()

    Text(
      text = value.ifBlank { placeholder },
      style = MaterialTheme.typography.bodyLarge,
      color = appearance.colorText.takeIf { value.isNotBlank() } ?: resolvedPlaceholderColor,
      modifier = Modifier.weight(1f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    Icon(
      imageVector = Icons.Default.KeyboardArrowDown,
      contentDescription = null,
      tint = resolvedContentColor,
      modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun XenditDropdownField(
  dropdownOptions: List<DropdownOption>, // Replace with your actual Channel data class type
  selectedOption: DropdownOption?,
  onChannelSelected: (DropdownOption) -> Unit,
  placeholderText: String,
  modifier: Modifier = Modifier,
  noBorder: Boolean = false,
) {
  // Internal state managed inside the custom composable
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { if (dropdownOptions.size > 1) expanded = !expanded },
    modifier = modifier.fillMaxWidth()
  ) {
    XenditDropdownHeaderField(
      value = selectedOption?.label ?: "",
      placeholder = placeholderText,
      isExpanded = expanded,
      modifier = Modifier
        .menuAnchor(
          type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
          enabled = dropdownOptions.size > 1
        )
        .fillMaxWidth(),
      borderColor = if (noBorder) Color.Transparent else null
    )

    if (dropdownOptions.size > 1) {
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        dropdownOptions.forEach { option ->
          DropdownMenuItem(
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(option.label)
              }
            },
            onClick = {
              onChannelSelected(option)
              expanded = false
            }
          )
        }
      }
    }
  }
}
