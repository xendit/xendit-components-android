package co.xendit.components.ui.helper

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder

internal object SdkImageLoader {
  private var loader: ImageLoader? = null

  fun get(context: Context): ImageLoader {
    return loader ?: synchronized(this) {
      loader ?: ImageLoader.Builder(context)
        .components { add(SvgDecoder.Factory()) }
        .build().also { loader = it }
    }
  }
}