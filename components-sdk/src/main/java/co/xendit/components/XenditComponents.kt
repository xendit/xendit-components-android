package co.xendit.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.VisibleForTesting
import androidx.annotation.Keep
import androidx.compose.ui.platform.ComposeView
import co.xendit.components.util.XLogger
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import co.xendit.components.core.CoreSdkComponent
import co.xendit.components.core.model.FallbackValue
import co.xendit.components.data.model.XenditPaymentResult
import co.xendit.components.data.model.XenditError
import co.xendit.components.ui.PaymentContainerHost
import co.xendit.components.ui.PaymentContainerPresentation
import co.xendit.components.ui.style.XenditAppearance
import co.xendit.components.ui.theme.XenditTheme
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** Main SDK entry point for displaying payment UI */
@Keep
enum class XenditComponentsPaymentType(val value: String) {
  @SerializedName("CARDS")
  CARDS("CARDS"),

  @SerializedName(value = "EWALLET", alternate = ["E_WALLET"])
  EWALLET("EWALLET"),

  @SerializedName("QR_CODE")
  QR_CODE("QR_CODE"),

  @SerializedName("BANK_TRANSFER")
  BANK_TRANSFER("BANK_TRANSFER"),

  @SerializedName("DIRECT_DEBIT")
  DIRECT_DEBIT("DIRECT_DEBIT"),

  @SerializedName("VIRTUAL_ACCOUNT")
  VIRTUAL_ACCOUNT("VIRTUAL_ACCOUNT"),

  @SerializedName("OVER_THE_COUNTER")
  OVER_THE_COUNTER("OVER_THE_COUNTER"),

  @FallbackValue
  @SerializedName("UNKNOWN")
  UNKNOWN("UNKNOWN");

  companion object {
    val SUPPORTED: List<XenditComponentsPaymentType> =
      listOf(CARDS, EWALLET, QR_CODE)
  }
}

object XenditComponents {

  private var composeView: ComposeView? = null
  private var currentCallback: ((XenditPaymentResult) -> Unit)? = null
  private var xenditAppearance: XenditAppearance? = null
  private var merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>? = null
  private var lifecycleOwner: LifecycleOwner? = null
  private var lifecycleObserver: DefaultLifecycleObserver? = null
  private val scope = CoroutineScope(Dispatchers.Main)

  /**
   * Global configuration for the SDK appearance. This is called before show() to apply custom styles.
   */
  fun initialize(
    appearance: XenditAppearance? = null,
    merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>? = null,
  ) {
    this.xenditAppearance = appearance
    this.merchantPreferredPaymentMethod = merchantPreferredPaymentMethod
  }

  /** Internal data class to holding parsed keys. */
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal data class Keys(
    val sessionAuthKey: String,
    val hostId: String,
    val publicKey: String,
    val signature: String,
    val terminalId: String? = null
  )

  /**
   * Parses the component SDK key. Format: session_auth_key-host_id-public_key-signature Example:
   * session-123-prod-PK123-SIG123
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
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

  private fun resolveBaseUrlForHostId(hostId: String): String {
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
    merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>? = null,
    onPaymentResult: (XenditPaymentResult) -> Unit
  ) {
    if (activity !is Activity) {
      throw IllegalArgumentException("Context must be an Activity to show the Payment SDK.")
    }

    CoreSdkComponent.headerProvider.setMerchantAppId(activity.packageName ?: "")

    this.merchantPreferredPaymentMethod = merchantPreferredPaymentMethod

    val keys =
      try {
        parseSdkKey(componentsSdkKey)
      } catch (e: Exception) {
        XLogger.e("Failed to parse SDK Key", e)
        onPaymentResult.invoke(
          XenditPaymentResult.Failed(
            XenditError(
              code = "001",
              message = e.toString(),
              cause = e
            )
          )
        )
        return
      }

    CoreSdkComponent.setBaseUrl(resolveBaseUrlForHostId(keys.hostId))

    cleanup()

    lifecycleOwner = activity
    lifecycleObserver =
      object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
          cleanup()
        }
      }
    activity.lifecycle.addObserver(checkNotNull(lifecycleObserver))

    currentCallback = onPaymentResult

    composeView =
      ComposeView(activity).apply {
        setViewTreeLifecycleOwner(activity)
        setViewTreeViewModelStoreOwner(activity)
        setViewTreeSavedStateRegistryOwner(activity)
      }

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

  /** Dismisses the payment bottom sheet manually */
  fun dismiss() {
    currentCallback?.invoke(XenditPaymentResult.Canceled)
    cleanup()
  }

  private fun cleanup() {
    val owner = lifecycleOwner
    val observer = lifecycleObserver
    if (owner != null && observer != null) {
      owner.lifecycle.removeObserver(observer)
    }
    lifecycleOwner = null
    lifecycleObserver = null
    composeView?.let { view -> (view.parent as? ViewGroup)?.removeView(view) }
    composeView = null
    currentCallback = null
  }

  private fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
      if (context is ComponentActivity) return context
      context = context.baseContext
    }
    return null
  }
}
