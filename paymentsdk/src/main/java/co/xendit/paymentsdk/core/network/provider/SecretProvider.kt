package co.xendit.paymentsdk.core.network.provider

class SecretProvider {
  // Static data, set once
  private var clientSecretPrivateKey: String = ""

  fun initializeClientSecretPrivateKey(clientSecretPrivateKey: String) {
    this.clientSecretPrivateKey = clientSecretPrivateKey
  }

  // The interceptor will call these getters
  fun getClientSecretPrivateKey() = clientSecretPrivateKey
}
