package co.xendit.components.ui

import co.xendit.components.XenditComponentsPaymentType.Companion.BLACKLISTED_CHANNEL
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.BffSessionAllowSavePaymentMethod
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.PaymentSessionStatus
import co.xendit.components.data.model.SessionResponse
import co.xendit.components.data.network.repo.session.XenditRepository
import co.xendit.components.data.network.repo.session.XenditRepositoryResult

internal data class CheckoutSessionData(
  val sessionResponse: SessionResponse,
  val channels: List<BffChannel>,
  val channelVariantsByDisplayCode: Map<String, ChannelVariantChannels>,
  val selectableChannelCodes: List<String>,
  val paymentSessionId: String?,
  val sessionType: BffSessionType?,
  val allowSavePaymentMethod: BffSessionAllowSavePaymentMethod?,
  val sessionStatus: PaymentSessionStatus?
)

internal sealed interface PaymentSessionLoadResult {
  data class Success(val data: CheckoutSessionData) : PaymentSessionLoadResult
  data class Failure(
    val errorCode: String?,
    val message: String
  ) : PaymentSessionLoadResult
}

internal class PaymentSessionLoader(
  private val repository: XenditRepository
) {
  suspend fun load(sessionAuthKey: String): PaymentSessionLoadResult {
    return when (val response = repository.getSession(sessionAuthKey)) {
      is XenditRepositoryResult.Success -> {
        val body = response.data
        val session = body.session
        val channels = body.paymentChannels.orEmpty().filter {
          !BLACKLISTED_CHANNEL.contains(it.channelCode)
        }
        val combined = combinePairedChannels(channels)
        PaymentSessionLoadResult.Success(
          CheckoutSessionData(
            sessionResponse = body,
            channels = channels,
            channelVariantsByDisplayCode = combined.variantsByDisplayCode,
            selectableChannelCodes = channels.map { it.channelCode },
            paymentSessionId = session?.paymentSessionId ?: session?.id,
            sessionType = session?.sessionType,
            allowSavePaymentMethod = session?.allowSavePaymentMethod,
            sessionStatus = session?.status
          )
        )
      }

      is XenditRepositoryResult.Failure -> {
        PaymentSessionLoadResult.Failure(
          errorCode = response.errorCode,
          message = response.message ?: "Failed to fetch session"
        )
      }

      is XenditRepositoryResult.EmptyBody -> {
        PaymentSessionLoadResult.Failure(
          errorCode = "EMPTY_BODY",
          message = "Failed to fetch session"
        )
      }
    }
  }
}
