package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
internal fun DashedDivider(
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