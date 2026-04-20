package co.xendit.paymentsdk.core.network.provider

class HeaderProvider {
  private var origin: String = "https://demo-store.xendit.co"

  fun setOrigin(origin: String) {
    this.origin = origin
  }

  fun getOrigin(): String {
    return origin
  }
}
