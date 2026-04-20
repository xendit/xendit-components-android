package co.xendit.paymentsdk

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import co.xendit.paymentsdk.core.CoreSdkComponent
import co.xendit.paymentsdk.data.model.XenditPaymentResult
import co.xendit.paymentsdk.data.model.XenditError
import co.xendit.paymentsdk.ui.PaymentContainerHost
import co.xendit.paymentsdk.ui.PaymentContainerPresentation
import co.xendit.paymentsdk.ui.style.XenditAppearance
import co.xendit.paymentsdk.ui.theme.XenditTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** Main SDK entry point for displaying payment UI */
object XenditComponents {

  private var composeView: ComposeView? = null
  private var currentCallback: ((XenditPaymentResult) -> Unit)? = null
  private var xenditAppearance: XenditAppearance? = null
  private var merchantPreferredPaymentMethod: List<String>? = null
  private val scope = CoroutineScope(Dispatchers.Main)

  /**
   * Global configuration for the SDK appearance. Call this before show() to apply custom styles.
   */
  fun initialize(
    appearance: XenditAppearance? = null,
    merchantPreferredPaymentMethod: List<String>? = null,
  ) {
    this.xenditAppearance = appearance
    this.merchantPreferredPaymentMethod = merchantPreferredPaymentMethod
  }

  /** Internal data class to holding parsed keys. */
  internal data class Keys(
    val sessionAuthKey: String,
    val hostId: String,
    val publicKey: String,
    val signature: String,
    val terminalId: String? = null
  )

  /**
   * Parse the component SDK key. Format: session_auth_key-host_id-public_key-signature Example:
   * session-123-prod-PK123-SIG123
   */
  internal fun parseSdkKey(sdkKey: String): Keys {
    val parts = sdkKey.split("-")
    if (parts.size < 5) {
      throw IllegalArgumentException("Invalid SDK Key format")
    }
    val sessionAuthKey = "${parts[0]}-${parts[1]}"
    val hostId = parts[2] // used for host selection, stored if needed
    val publicKey = parts[3]
    val signature = parts[4]

    if (sessionAuthKey.isBlank()) {
      throw IllegalArgumentException("Invalid SDK Key format")
    }

    return Keys(sessionAuthKey, hostId, publicKey, signature)
  }

  internal fun resolveBaseUrlForHostId(hostId: String): String {
    return when (hostId.lowercase()) {
      "pl" -> "https://checkout-ui-gateway.xendit.co"
      "pd" -> "https://checkout-ui-gateway-prod-dev.xendit.co"
      "sl" -> "https://checkout-ui-gateway-live.stg.tidnex.dev"
      "sd" -> "https://checkout-ui-gateway-dev.stg.tidnex.dev"
      else -> "https://checkout-ui-gateway-prod-dev.xendit.co"
    }
  }

  /**
   * Initializes and displays the Xendit Payment SDK UI.
   *
   * @param activity The Android Context (e.g., Activity or Application Context).
   * @param componentsSdkKey The Session ID or Components SDK Key obtained from your backend.
   * @param style Custom styling configuration for the SDK.
   * @param onPaymentResult Callback triggered when a payment finishes (Success, Error, or
   * Canceled).
   */
  fun present(
    activity: ComponentActivity,
    componentsSdkKey: String,
    merchantPreferredPaymentMethod: List<String>? = null,
    onPaymentResult: (XenditPaymentResult) -> Unit
  ) {
    if (activity !is Activity) {
      throw IllegalArgumentException("Context must be an Activity to show the Payment SDK.")
    }
//    CoreSdkComponent.headerProvider.setOrigin(activity.packageName ?: "") // now only use default
    this.merchantPreferredPaymentMethod = merchantPreferredPaymentMethod

    val keys =
      try {
        parseSdkKey(componentsSdkKey)
      } catch (e: Exception) {
        Log.e("PaymentSDK", "Failed to parse SDK Key", e)
        onPaymentResult.invoke(
          XenditPaymentResult.Failed(
            XenditError(
              code = "111",
              message = e.toString(),
              cause = e
            )
          )
        )
        return
      }

    CoreSdkComponent.setBaseUrl(resolveBaseUrlForHostId(keys.hostId))

    // Clear previous if any
    cleanup()

    currentCallback = onPaymentResult

    // Create a new ComposeView for this session
    composeView =
      ComposeView(activity).apply {
        setViewTreeLifecycleOwner(activity)
        setViewTreeViewModelStoreOwner(activity)
        setViewTreeSavedStateRegistryOwner(activity)
      }

    // Set the content
    composeView?.setContent {
      XenditTheme(style = this.xenditAppearance ?: XenditAppearance()) {
        PaymentContainerHost(
          presentation = PaymentContainerPresentation.Dialog,
          sessionAuthKey = keys.sessionAuthKey,
          publicKey = keys.publicKey,
          merchantPreferredPaymentMethod = merchantPreferredPaymentMethod,
          style = xenditAppearance ?: XenditAppearance(),
          onResult = { result -> currentCallback?.invoke(result) },
          onCleanup = { cleanup() }
        )
      }
    }

    // Add view to activity's content
    composeView?.let { view ->
      activity.addContentView(
        view,
        ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
      )
    }
  }

  /** Dismiss the payment bottom sheet manually */
  fun dismiss() {
    currentCallback?.invoke(XenditPaymentResult.Canceled)
    cleanup()
  }

  /** Internal cleanup to remove the view from hierarchy */
  private fun cleanup() {
    composeView?.let { view -> (view.parent as? ViewGroup)?.removeView(view) }
    composeView = null
    currentCallback = null
  }

  /** Find the ComponentActivity from the context */
  private fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
      if (context is ComponentActivity) return context
      context = context.baseContext
    }
    return null
  }
}
