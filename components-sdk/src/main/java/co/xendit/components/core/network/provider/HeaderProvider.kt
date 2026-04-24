package co.xendit.components.core.network.provider

import co.xendit.components.BuildConfig

internal class HeaderProvider {
  private var origin: String = "https://demo-store.xendit.co"
  private var hostId: String = "co.example.components"

  fun setHostId(hostId: String) {
    this.hostId = hostId
  }

  fun getOrigin(): String {
    return origin
  }

  fun getSdkVersion(): String {
    return "android:1.0.0"
  }

  fun getHostId(): String {
    return hostId
  }
}
