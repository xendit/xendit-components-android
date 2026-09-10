package co.xendit.components

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import co.xendit.components.core.CoreSdkComponent
import co.xendit.components.data.model.XenditError
import co.xendit.components.data.model.XenditPaymentResult
import co.xendit.components.telemetry.TelemetryHostResolver
import co.xendit.components.ui.PaymentContainerHost
import co.xendit.components.ui.PaymentContainerPresentation
import co.xendit.components.ui.PaymentContainerSessionController
import co.xendit.components.ui.style.XenditAppearance
import co.xendit.components.ui.theme.XenditTheme
import co.xendit.components.util.XLogger

internal class XenditComponentsSessionStarter(
  private val resolveBaseUrlForHostId: (String) -> String,
  private val parseSdkKey: (String) -> XenditComponents.Keys,
  private val cleanupActiveSession: (XenditComponents.ActivePresentationSession?) -> Unit,
  private val setActiveSession: (XenditComponents.ActivePresentationSession?) -> Unit,
  private val currentSessionTelemetry: () -> co.xendit.components.telemetry.SessionTelemetry?
) {
  fun start(
    request: XenditSessionStartRequest,
    currentAppearance: XenditAppearance?
  ): XenditComponentsSession {
    val activity = request.activity
    CoreSdkComponent.init(activity.applicationContext)
    CoreSdkComponent.headerProvider.setMerchantAppId(activity.packageName ?: "")

    val keys =
      try {
        parseSdkKey(request.componentsSdkKey)
      } catch (e: Exception) {
        XLogger.e("Failed to parse SDK Key", e)
        request.onPaymentResult.invoke(
          XenditPaymentResult.Failed(
            XenditError(
              code = "001",
              message = e.toString(),
              cause = e
            )
          )
        )
        return XenditComponentsSession(dismissAction = {}, wipeAction = {})
      }

    CoreSdkComponent.setBaseUrl(resolveBaseUrlForHostId(keys.hostId))

    cleanupActiveSession(null)
    val controller = PaymentContainerSessionController()
    val sessionHandle =
      XenditComponentsSession(
        dismissAction = { controller.requestDismiss() },
        wipeAction = {
          controller.requestWipe()
          runCatching { currentSessionTelemetry()?.discardAll() }
        }
      )
    val session =
      XenditComponents.ActivePresentationSession(
        activity = activity,
        sessionHandle = sessionHandle,
        controller = controller,
        composeView =
          ComposeView(activity).apply {
            setViewTreeLifecycleOwner(activity)
            setViewTreeViewModelStoreOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
          },
        onPaymentResult = request.onPaymentResult
      )
    setActiveSession(session)

    val telemetryHost = TelemetryHostResolver.fromHostId(keys.hostId)
    runCatching {
      currentSessionTelemetry()?.let { tm ->
        tm.discardAll()
        tm.bindSession(host = telemetryHost, sessionId = null, authId = keys.sessionAuthKey)
      }
    }

    val callbacks =
      object : android.content.ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
          if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            session.controller.requestWipe()
            runCatching { currentSessionTelemetry()?.discardAll() }
          }
        }

        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) = Unit

        @Deprecated("Android deprecated this callback; retained for legacy memory-pressure handling.")
        override fun onLowMemory() {
          session.controller.requestWipe()
          runCatching { currentSessionTelemetry()?.discardAll() }
        }
      }
    session.componentCallbacks = callbacks
    runCatching { activity.registerComponentCallbacks(callbacks) }

    val sharedFlushObserver = object : DefaultLifecycleObserver {
      override fun onStop(owner: LifecycleOwner) {
        runCatching { currentSessionTelemetry()?.flush() }
      }
    }

    session.processLifecycleObserver = sharedFlushObserver
    runCatching {
      androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(sharedFlushObserver)
    }

    session.lifecycleObserver = object : DefaultLifecycleObserver by sharedFlushObserver {
      override fun onDestroy(owner: LifecycleOwner) {
        runCatching { currentSessionTelemetry()?.flush() }
        cleanupActiveSession(session)
      }
    }
    activity.lifecycle.addObserver(checkNotNull(session.lifecycleObserver))

    session.composeView.setContent {
      XenditTheme(style = currentAppearance ?: XenditAppearance()) {
        PaymentContainerHost(
          controller = controller,
          presentation = PaymentContainerPresentation.Dialog,
          sessionAuthKey = keys.sessionAuthKey,
          publicKey = keys.publicKey,
          merchantPreferredPaymentMethod = request.merchantPreferredPaymentMethod
            ?: request.configuration.merchantPreferredPaymentMethod,
          style = currentAppearance ?: XenditAppearance(),
          onResult = session.onPaymentResult,
          onCleanup = { cleanupActiveSession(session) }
        )
      }
    }

    activity.addContentView(
      session.composeView,
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    )
    return sessionHandle
  }
}
