package co.xendit.components.ui

internal data class PaymentExecutionContext(
  val sessionAuthKey: String,
  val publicKey: String,
  val paymentSessionId: String
)

internal class PaymentSessionRuntime {
  var publicKey: String? = null
    private set
  var sessionAuthKey: String? = null
    private set
  var paymentSessionId: String? = null
    private set
  var lastPaymentRequestId: String? = null
    private set
  var lastSessionTokenRequestId: String? = null
    private set

  private val lastSelectedChannelCodeByUiGroup: MutableMap<String, String> = mutableMapOf()

  fun initialize(
    sessionAuthKey: String,
    publicKey: String
  ) {
    clear()
    this.sessionAuthKey = sessionAuthKey
    this.publicKey = publicKey
  }

  fun updatePaymentSessionId(paymentSessionId: String?) {
    this.paymentSessionId = paymentSessionId
  }

  fun recordSubmittedEntity(
    paymentRequestId: String?,
    sessionTokenRequestId: String?
  ) {
    lastPaymentRequestId = paymentRequestId
    lastSessionTokenRequestId = sessionTokenRequestId
  }

  fun rememberSelectedChannel(
    uiGroup: String,
    channelCode: String
  ) {
    lastSelectedChannelCodeByUiGroup[uiGroup] = channelCode
  }

  fun lastSelectedChannel(uiGroup: String): String? = lastSelectedChannelCodeByUiGroup[uiGroup]

  fun executionContext(): PaymentExecutionContext? {
    val authKey = sessionAuthKey ?: return null
    val key = publicKey ?: return null
    val paySid = paymentSessionId ?: return null
    return PaymentExecutionContext(
      sessionAuthKey = authKey,
      publicKey = key,
      paymentSessionId = paySid
    )
  }

  fun clear() {
    sessionAuthKey = null
    publicKey = null
    paymentSessionId = null
    lastPaymentRequestId = null
    lastSessionTokenRequestId = null
    lastSelectedChannelCodeByUiGroup.clear()
  }
}
