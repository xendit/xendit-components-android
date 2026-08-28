package co.xendit.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import co.xendit.components.core.CoreSdkComponent
import co.xendit.components.core.TelemetrySdkComponent
import co.xendit.components.core.model.FallbackValue
import co.xendit.components.data.model.XenditError
import co.xendit.components.data.model.XenditPaymentResult
import co.xendit.components.telemetry.TelemetryHostResolver
import co.xendit.components.ui.PaymentContainerHost
import co.xendit.components.ui.PaymentContainerHostSignals
import co.xendit.components.ui.PaymentContainerPresentation
import co.xendit.components.ui.style.XenditAppearance
import co.xendit.components.ui.theme.XenditTheme
import co.xendit.components.util.XLogger
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
      listOf(
        CARDS,
        EWALLET,
        QR_CODE,
        BANK_TRANSFER,
        DIRECT_DEBIT,
        VIRTUAL_ACCOUNT,
        OVER_THE_COUNTER
      )
    val BLACKLISTED_CHANNEL = listOf(
      "BRI_DIRECT_DEBIT"
    )
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

  private var activeComponentsSdkKey: String? = null
  private var activeActivity: ComponentActivity? = null
  private var activeMerchantPreferredPm: List<XenditComponentsPaymentType>? = null
  private var componentCallbacks: android.content.ComponentCallbacks2? = null
  private var processLifecycleObserver: DefaultLifecycleObserver? = null

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
   * After the payment flow finishes (onPaymentResult callback), the SDK has already wiped
   * all in-memory PAN/CVV buffers it owns. For pen-test / heap-dump compliance, call
   * [performSensitiveDataGcPass] from your app layer once you receive the
   * [XenditPaymentResult] and finish processing / storing it — that prompts ART to collect
   * any short-lived transient String copies the composable frames / Retrofit serializers
   * produced during the submit window.
   *
   * @param activity The Android Context (e.g., Activity or Application Context).
   * @param componentsSdkKey The Session ID or Components SDK Key obtained from your backend.
   * @param merchantPreferredPaymentMethod Optional ordered list of preferred payment methods
   *   to surface first.
   * @param onPaymentResult Callback triggered when a payment finishes (Success, Error, or
   *   Canceled).
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

    CoreSdkComponent.init(activity.applicationContext)
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

    activeComponentsSdkKey = componentsSdkKey
    activeActivity = activity
    activeMerchantPreferredPm = merchantPreferredPaymentMethod

    // ===== Telemetry: bind host + session auth key early, payment_session_id from FetchSession later.
    val telemetryHost = TelemetryHostResolver.fromHostId(keys.hostId)
    runCatching {
      safeSessionTelemetry()?.let { tm ->
        tm.discardAll()
        tm.bindSession(host = telemetryHost, sessionId = null, authId = keys.sessionAuthKey)
      }
    }
    // ===== End telemetry setup


    // ===== Mitigation 3: Aggressively purge state when Android signals memory pressure =====
    val callbacks =
      object : android.content.ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
          // TRIM_MEMORY_BACKGROUND = process entered cached state;
          // TRIM_MEMORY_MODERATE/COMPLETE = OS needs RAM now.
          // On any of these, do a full wipe (including form values):
          if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            PaymentContainerHostSignals.onWipeTriggerStatic?.invoke()
            runCatching { safeSessionTelemetry()?.discardAll() }
          }
        }

        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) = Unit
        override fun onLowMemory() {
          PaymentContainerHostSignals.onWipeTriggerStatic?.invoke()
          runCatching { safeSessionTelemetry()?.discardAll() }
        }
      }
    this.componentCallbacks = callbacks
    runCatching { activity.registerComponentCallbacks(callbacks) }

    lifecycleOwner = activity

    // Single onStop flush callback, reused for both lifecycle owners to avoid duplicate code.
    val sharedFlushObserver = object : DefaultLifecycleObserver {
      override fun onStop(owner: LifecycleOwner) {
        runCatching { safeSessionTelemetry()?.flush() }
      }
    }

    // Process-scoped observer (app-wide background). Mirrors Web visibilitychange→hidden flush.
    this.processLifecycleObserver = sharedFlushObserver
    runCatching {
      androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(sharedFlushObserver)
    }

    // Activity-scoped observer: extends sharedFlushObserver with onDestroy -> flush + cleanup().
    lifecycleObserver = object : DefaultLifecycleObserver by sharedFlushObserver {
      override fun onDestroy(owner: LifecycleOwner) {
        runCatching { safeSessionTelemetry()?.flush() }
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
    PaymentContainerHostSignals.onDismissRequestedStatic?.invoke()
  }

  /**
   * Synchronously wipes every sensitive buffer the SDK currently holds (PAN / CVV TextField
   * states, card details, auth keys, draft form values). This is already invoked automatically
   * by the SDK at the end of every payment flow (including Cancel / Dismiss) and on Android
   * [ComponentCallbacks2.TRIM_MEMORY_BACKGROUND]. You only need to call this yourself if you
   * keep the SDK retained across long user journeys and want to drop sensitive state at an
   * intermediate checkpoint (e.g. after the user navigates away from the card screen).
   *
   * After wiping, call [performSensitiveDataGcPass] from the merchant app layer to prompt ART
   * to collect any transient short-lived String copies left on the heap.
   */
  @Keep
  fun wipeAllSensitiveData() {
    PaymentContainerHostSignals.onWipeTriggerStatic?.invoke()
    runCatching { safeSessionTelemetry()?.discardAll() }
  }


  /**
   * Runs an ART garbage-collection pass from the **merchant app layer** to collect any
   * short-lived PAN / CVV transient String copies produced by composable frame rendering or
   * network payload serialisation.
   *
   * The SDK itself never calls `Runtime.gc()` internally. Use this helper once you receive
   * the [XenditPaymentResult] callback (or after you call [wipeAllSensitiveData] at an
   * intermediate checkpoint) to clean up the transient strings that have not yet been
   * reclaimed by ART's normal collection cadence. A typical pen-test-safe sequence looks
   * like:
   *
   * ```
   * XenditComponents.present(activity, key) { result ->
   *     // persist / log result first, then:
   *     XenditComponents.wipeAllSensitiveData()
   *     XenditComponents.performSensitiveDataGcPass()
   * }
   * ```
   */
  @Keep
  fun performSensitiveDataGcPass() {
    runCatching {
      Runtime.getRuntime().gc()
      Runtime.getRuntime().runFinalization()
      Runtime.getRuntime().gc()
    }
  }

  private fun cleanup() {
    PaymentContainerHostSignals.onWipeTriggerStatic = null
    PaymentContainerHostSignals.onDismissRequestedStatic = null

    val procObs = processLifecycleObserver
    if (procObs != null) {
      runCatching {
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.removeObserver(procObs)
      }
    }
    processLifecycleObserver = null

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

    val activityForCallbacks = activeActivity

    // Unregister ComponentCallbacks2 (onTrimMemory / onLowMemory hook)
    val cb = componentCallbacks
    if (cb != null && activityForCallbacks != null) {
      runCatching { activityForCallbacks.unregisterComponentCallbacks(cb) }
    }
    componentCallbacks = null

    activeComponentsSdkKey = null
    activeMerchantPreferredPm = null
    activeActivity = null

    runCatching {
      safeSessionTelemetry()?.let { tm ->
        tm.flush()
        tm.discardAll()
      }
    }

  }

  private fun safeSessionTelemetry(): co.xendit.components.telemetry.SessionTelemetry? {
    return if (CoreSdkComponent.isInitialized()) TelemetrySdkComponent.sessionTelemetry else null
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
