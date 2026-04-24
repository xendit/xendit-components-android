package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.xendit.components.ui.style.xenditAppearance

@Composable
internal fun GenericHeader(
  title: String,
  onLeftClick: (() -> Unit)? = null,
  leftIcon: ImageVector? = Icons.AutoMirrored.Default.ArrowBack,
  rightIcon: ImageVector? = null,
  onRightClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(56.dp) // Standard TopAppBar height
      .padding(horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Left Icon Slot
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
      if (leftIcon != null && onLeftClick != null) {
        IconButton(onClick = { onLeftClick.invoke() }) {
          Icon(
            imageVector = leftIcon,
            contentDescription = "Back",
            tint = appearance.colorText
          )
        }
      }
    }

    // Title
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
      ),
      modifier = Modifier
        .weight(1f)
        .padding(start = 8.dp),
      color = appearance.colorText
    )

    // Right Icon Slot
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
      if (rightIcon != null) {
        IconButton(onClick = { onRightClick?.invoke() }) {
          Icon(
            imageVector = rightIcon,
            contentDescription = "Action",
            tint = appearance.colorText
          )
        }
      }
    }
  }
}