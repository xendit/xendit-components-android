package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.xendit.components.ui.style.xenditAppearance

@Composable
internal fun XenditDropdownField(
  value: String,
  placeholder: String,
  isExpanded: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape? = null,
  borderColor: Color? = null,
  contentColor: Color? = null,
  placeholderColor: Color? = null,
  leadingContent: (@Composable (() -> Unit))? = null,
) {
  val appearance = xenditAppearance
  val resolvedShape = shape ?: MaterialTheme.shapes.small
  val resolvedBorderColor = borderColor ?: appearance.colorBorder
  val resolvedContentColor = contentColor ?: appearance.colorTextSecondary
  val resolvedPlaceholderColor = placeholderColor ?: appearance.colorTextPlaceholder

  Row(
    modifier = modifier
      .heightIn(min = 56.dp)
      .clip(resolvedShape)
      .background(appearance.colorBackground, resolvedShape)
      .border(1.dp, resolvedBorderColor, resolvedShape)
      .clickable(enabled = enabled, onClick = onClick)
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
