package co.xendit.components.ui.qrcode

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.xendit.components.R
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.ui.style.xenditAppearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QrPaymentUI(
  channels: List<BffChannel>,
  selectedChannel: BffChannel?,
  onSelectChannel: (String) -> Unit,
  onFormStateChanged: (Map<String, String>, List<ChannelFormField>) -> Unit,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val formValues = remember { mutableStateMapOf<String, String>() }
  val visibleFields = remember { mutableStateOf<List<ChannelFormField>>(emptyList()) }

  LaunchedEffect(selectedChannel?.channelCode) {
    formValues.clear()
    visibleFields.value = emptyList()
    onFormStateChanged(emptyMap(), emptyList())
  }

  Column(
    modifier = modifier.fillMaxWidth()
  ) {
    DashedDivider(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      color = appearance.colorBorder
    )
    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_qris),
        contentDescription = null,
        tint = appearance.colorTextSecondary,
        modifier = Modifier.size(28.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        selectedChannel?.instructions?.forEachIndexed { index, instruction ->
          Text(
            text = instruction,
            style = MaterialTheme.typography.titleSmall.takeIf { index == 0 } ?: MaterialTheme.typography.bodySmall,
            color = appearance.colorText.takeIf { index == 0 } ?: appearance.colorTextSecondary
          )
          if (index != selectedChannel.instructions.size - 1) {
            Spacer(modifier = Modifier.height(4.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun DashedDivider(
  modifier: Modifier = Modifier,
  color: Color
) {
  Canvas(modifier = modifier.height(1.dp)) {
    val y = size.height / 2f
    drawLine(
      color = color,
      start = Offset(0f, y),
      end = Offset(size.width, y),
      strokeWidth = size.height,
      cap = StrokeCap.Round,
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
  }
}
