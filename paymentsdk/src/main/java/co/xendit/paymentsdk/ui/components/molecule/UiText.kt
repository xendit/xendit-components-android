package co.xendit.paymentsdk.ui.components.molecule

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

internal sealed class UiText {
  class DynamicString(val value: String) : UiText()
  class StringResource(
    @StringRes val resId: Int,
    vararg val args: Any
  ) : UiText()

  @Composable
  fun asString(): String {
    return when (this) {
      is DynamicString -> value
      is StringResource -> stringResource(id = resId, *args.map {
        if (it is UiText) it.asString() else it
      }.toTypedArray())
    }
  }

  fun asString(context: Context?): String {
    return when (this) {
      is DynamicString -> value
      is StringResource -> context?.getString(resId, *args.map {
        if (it is UiText) it.asString(context) else it
      }.toTypedArray()) ?: ""
    }
  }
}