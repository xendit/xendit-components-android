package co.xendit.components.ui.components.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import co.xendit.components.ui.style.xenditAppearance
import coil.ImageLoader
import coil.compose.AsyncImage

@Composable
internal fun ChannelLogo(
  logoUrl: String,
  imageLoader: ImageLoader,
) {
  val appearance = xenditAppearance

  Box(
    modifier = Modifier
      .size(width = 36.dp, height = 24.dp)
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
      contentScale = ContentScale.Fit,
      modifier = Modifier
        .padding(4.dp)
    )
  }
}
