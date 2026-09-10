package co.xendit.components

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import co.xendit.components.core.CoreSdkComponent
import co.xendit.components.core.TelemetrySdkComponent
import co.xendit.components.core.model.FallbackValue
import co.xendit.components.data.model.XenditPaymentResult
import co.xendit.components.ui.style.XenditAppearance
import com.google.gson.annotations.SerializedName

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

  @SerializedName("GOOGLE_PAY")
  GOOGLE_PAY("GOOGLE_PAY"),

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
        OVER_THE_COUNTER,
        GOOGLE_PAY
      )
    val BLACKLISTED_CHANNEL = listOf(
      "BRI_DIRECT_DEBIT"
    )
  }
}

object XenditComponents {

  private var xenditAppearance: XenditAppearance? = null
  private var merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>? = null
  private var activeSession: ActivePresentationSession? = null

  internal class ActivePresentationSession(
    val activity: ComponentActivity,
    val sessionHandle: XenditComponentsSession,
    val controller: co.xendit.components.ui.PaymentContainerSessionController,
    val composeView: androidx.compose.ui.platform.ComposeView,
    val onPaymentResult: (co.xendit.components.data.model.XenditPaymentResult) -> Unit,
    var componentCallbacks: android.content.ComponentCallbacks2? = null,
    var lifecycleObserver: androidx.lifecycle.DefaultLifecycleObserver? = null,
    var processLifecycleObserver: androidx.lifecycle.DefaultLifecycleObserver? = null,
  )

  private val sessionStarter =
    XenditComponentsSessionStarter(
      resolveBaseUrlForHostId = ::resolveBaseUrlForHostId,
      parseSdkKey = ::parseSdkKey,
      cleanupActiveSession = { session -> if (session == null) cleanup() else cleanup(session) },
      setActiveSession = { activeSession = it },
      currentSessionTelemetry = ::safeSessionTelemetry
    )

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
    launcher(activity).present(
      componentsSdkKey = componentsSdkKey,
      merchantPreferredPaymentMethod = merchantPreferredPaymentMethod,
      onPaymentResult = onPaymentResult
    )
  }

  @Keep
  fun launcher(activity: ComponentActivity): XenditComponentsLauncher {
    return XenditComponentsLauncher(
      activity = activity,
      configuration = currentLauncherConfiguration(),
      startSession = ::startSessionFromLauncher
    )
  }

  private fun startSessionFromLauncher(
    request: XenditSessionStartRequest
  ): XenditComponentsSession {
    this.xenditAppearance = request.configuration.appearance
    this.merchantPreferredPaymentMethod = request.merchantPreferredPaymentMethod
      ?: request.configuration.merchantPreferredPaymentMethod
    return sessionStarter.start(
      request = request,
      currentAppearance = xenditAppearance
    )
  }

  /** Dismisses the payment bottom sheet manually */
  fun dismiss() {
    activeSession?.sessionHandle?.dismiss()
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
    activeSession?.sessionHandle?.wipeAllSensitiveData()
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
    activeSession?.sessionHandle?.performSensitiveDataGcPass()
      ?: XenditComponentsSession(dismissAction = {}, wipeAction = {}).performSensitiveDataGcPass()
  }

  private fun currentLauncherConfiguration(): XenditLauncherConfiguration {
    return XenditLauncherConfiguration(
      appearance = xenditAppearance,
      merchantPreferredPaymentMethod = merchantPreferredPaymentMethod
    )
  }

  private fun cleanup(session: ActivePresentationSession? = activeSession) {
    val target = session ?: return
    if (activeSession === target) {
      activeSession = null
    }

    target.controller.unbind()

    val procObs = target.processLifecycleObserver
    if (procObs != null) {
      runCatching {
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.removeObserver(procObs)
      }
    }
    target.processLifecycleObserver = null

    val observer = target.lifecycleObserver
    if (observer != null) {
      target.activity.lifecycle.removeObserver(observer)
    }
    target.lifecycleObserver = null

    // Unregister ComponentCallbacks2 (onTrimMemory / onLowMemory hook)
    val cb = target.componentCallbacks
    if (cb != null) {
      runCatching { target.activity.unregisterComponentCallbacks(cb) }
    }
    target.componentCallbacks = null
    (target.composeView.parent as? ViewGroup)?.removeView(target.composeView)

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
}
