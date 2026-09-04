package co.xendit.components.telemetry

internal object TelemetryHostResolver {
  // 1:1 with Web SDK hosts.json → telemetryHosts[pl/pd/sl/sd] via env.
  // Do NOT use telemetry.* hosts here; the telemetry endpoint lives on log.*
  // (see XenditComponentsWeb/hosts.json for the canonical list).
  private val hosts = mapOf(
    "pl" to "https://log.xendit.co",
    "pd" to "https://log-dev.xendit.co",
    "sl" to "https://log.stg.tidnex.dev",
    "sd" to "https://log-dev.stg.tidnex.dev",
  )

  fun fromHostId(hostId: String): String? = hosts[hostId.lowercase()]
}
