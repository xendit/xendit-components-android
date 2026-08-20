package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import co.xendit.components.ui.XenditTestTags
import co.xendit.components.ui.style.xenditAppearance

@Composable
internal fun CheckboxWithText(
  checked: Boolean,
  text: String,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
  textColor: Color = xenditAppearance.colorText,
  testTag: String = XenditTestTags.SAVE_CARD_CHECKBOX,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .toggleable(
        value = checked,
        enabled = enabled,
        role = Role.Checkbox,
        onValueChange = onCheckedChange
      )
      .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
  ) {
    Checkbox(
      checked = checked,
      onCheckedChange = null,
      enabled = enabled
    )
    Text(
      text = text,
      style = textStyle,
      color = textColor,
      modifier = Modifier.padding(start = 8.dp)
    )
  }
}
