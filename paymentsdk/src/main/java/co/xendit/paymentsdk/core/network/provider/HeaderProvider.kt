package co.xendit.paymentsdk.core.network.provider

class HeaderProvider {
  // Static data, set once
  private var partnerId: String? = null
  private var deviceId: String? = null
  private var origin: String = "https://demo-store.xendit.co"

  // Dynamic data, set per transaction
  var bssSessionTokenGenerator: suspend () -> String? = { null }

  fun initializeStaticHeaders(partnerId: String, deviceId: String) {
    this.partnerId = partnerId
    this.deviceId = deviceId
  }

  fun setRefreshSessionToken(bssSessionTokenGenerator: suspend () -> String? = { null }) {
    this.bssSessionTokenGenerator = bssSessionTokenGenerator
  }

  fun setOrigin(origin: String) {
    this.origin = origin
  }

  // The interceptor will call these getters
  fun getPartnerId() = partnerId
  fun getDeviceId() = deviceId
  fun getOrigin() = origin
}
