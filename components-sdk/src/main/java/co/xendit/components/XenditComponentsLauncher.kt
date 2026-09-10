package co.xendit.components

import androidx.activity.ComponentActivity
import androidx.annotation.Keep
import co.xendit.components.data.model.XenditPaymentResult
import co.xendit.components.ui.style.XenditAppearance

internal data class XenditLauncherConfiguration(
  val appearance: XenditAppearance?,
  val merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>?
)

internal data class XenditPresentRequest(
  val activity: ComponentActivity,
  val configuration: XenditLauncherConfiguration,
  val componentsSdkKey: String,
  val merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>?,
  val onPaymentResult: (XenditPaymentResult) -> Unit
)

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

@Keep
class XenditComponentsLauncher internal constructor(
  private val activity: ComponentActivity,
  private val configuration: XenditLauncherConfiguration,
  private val presenter: (XenditPresentRequest) -> XenditComponentsSession
) {
  fun present(
    componentsSdkKey: String,
    merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>? = configuration.merchantPreferredPaymentMethod,
    onPaymentResult: (XenditPaymentResult) -> Unit
  ): XenditComponentsSession {
    return presenter(
      XenditPresentRequest(
        activity = activity,
        configuration = configuration,
        componentsSdkKey = componentsSdkKey,
        merchantPreferredPaymentMethod = merchantPreferredPaymentMethod,
        onPaymentResult = onPaymentResult
      )
    )
  }
}
