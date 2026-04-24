package co.xendit.components.core.network.provider

internal class HeaderProvider {
  private var origin: String = "https://demo-store.xendit.co"

  fun setOrigin(origin: String) {
    this.origin = origin
  }

  fun getOrigin(): String {
    return origin
  }
}
