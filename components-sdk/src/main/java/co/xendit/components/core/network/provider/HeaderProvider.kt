package co.xendit.components.core.network.provider

import co.xendit.components.BuildConfig

internal class HeaderProvider {
  private var merchantAppId: String = "co.example.components"

  fun setMerchantAppId(hostId: String) {
    this.merchantAppId = hostId
  }

  fun getSdkVersion(): String {
    return BuildConfig.VERSION_NAME
  }

  fun getMerchantAppId(): String {
    return "android:${merchantAppId}"
  }
}
