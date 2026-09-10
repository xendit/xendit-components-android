package co.xendit.components

import androidx.annotation.Keep

@Keep
class XenditComponentsSession internal constructor(
  private val dismissAction: () -> Unit,
  private val wipeAction: () -> Unit
) {
  fun dismiss() {
    dismissAction()
  }

  fun wipeAllSensitiveData() {
    wipeAction()
  }

  fun performSensitiveDataGcPass() {
    runCatching {
      Runtime.getRuntime().gc()
      Runtime.getRuntime().runFinalization()
      Runtime.getRuntime().gc()
    }
  }
}
