package co.xendit.components.core

import co.xendit.components.BuildConfig
import co.xendit.components.telemetry.SessionTelemetry

internal object TelemetrySdkComponent {
  val sessionTelemetry: SessionTelemetry by lazy {
    SessionTelemetry(
      okHttpClient = CoreSdkComponent.okHttpTelemetry,
      gson = CoreSdkComponent.gson,
      logTelemetryEvents = BuildConfig.DEBUG,
    )
  }
}
