package co.xendit.components.ui.ui_util

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

internal object CustomShape {

  fun createTopRoundedOpenShape(radius: Dp) = object : Shape {
    override fun createOutline(
      size: Size,
      layoutDirection: LayoutDirection,
      density: Density
    ): Outline {
      val rPx = with(density) { radius.toPx() }
      val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, rPx)
        arcTo(
          rect = Rect(0f, 0f, rPx * 2, rPx * 2),
          startAngleDegrees = 180f,
          sweepAngleDegrees = 90f,
          forceMoveTo = false
        )
        lineTo(size.width - rPx, 0f)
        arcTo(
          rect = Rect(size.width - rPx * 2, 0f, size.width, rPx * 2),
          startAngleDegrees = 270f,
          sweepAngleDegrees = 90f,
          forceMoveTo = false
        )
        lineTo(size.width, size.height)
      }
      return Outline.Generic(path)
    }
  }

  val customCornersShapeLeft = RoundedCornerShape(
    topStart = 12.dp,
    topEnd = 0.dp,
    bottomEnd = 0.dp,
    bottomStart = 12.dp
  )

  val customCornersShapeRight = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 12.dp,
    bottomEnd = 12.dp,
    bottomStart = 0.dp
  )
}
