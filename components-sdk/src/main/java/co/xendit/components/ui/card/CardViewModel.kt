package co.xendit.components.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.xendit.components.data.encryption.XenditEncryption
import co.xendit.components.data.model.CardDetails
import co.xendit.components.data.model.InstallmentPlan
import co.xendit.components.data.model.PaymentOptionsRequest
import co.xendit.components.data.network.repo.session.XenditRepository
import co.xendit.components.ui.helper.FormCheckerUtil.isValidCreditCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import co.xendit.components.util.AmountFormat
import java.math.BigDecimal

internal data class CardState(
  val cardDetails: CardDetails? = null,
  val installmentPlans: List<InstallmentPlan>? = null
)

internal sealed interface CardIntent {
  data class ConfigureSession(
    val sessionAuthKey: String,
    val publicKey: String,
    val paymentSessionId: String
  ) : CardIntent

  data class CardNumberChanged(val cardNumber: String) : CardIntent
  data object Reset : CardIntent
}

internal class CardViewModel(
  private val xenditRepository: XenditRepository
) : ViewModel() {
  private val _state = MutableStateFlow(CardState())
  val state: StateFlow<CardState> = _state.asStateFlow()

  private var sessionAuthKey: String? = null
  private var publicKey: String? = null
  private var paymentSessionId: String? = null

  private var cardInfoJob: Job? = null

  fun dispatch(intent: CardIntent) {
    when (intent) {
      is CardIntent.ConfigureSession -> {
        sessionAuthKey = intent.sessionAuthKey
        publicKey = intent.publicKey
        paymentSessionId = intent.paymentSessionId
        _state.value = CardState()
      }

      is CardIntent.CardNumberChanged -> onCardNumberChangedInternal(intent.cardNumber)
      CardIntent.Reset -> resetInternal()
    }
  }

  private fun resetInternal() {
    cardInfoJob?.cancel()
    _state.value = CardState()
  }

  private fun onCardNumberChangedInternal(cardNumber: String) {
    cardInfoJob?.cancel()

    if (cardNumber.length < 6) {
      _state.update { it.copy(cardDetails = null, installmentPlans = null) }
      return
    }

    val key = publicKey ?: return
    val authKey = sessionAuthKey ?: return
    val paySid = paymentSessionId ?: return

    cardInfoJob =
      viewModelScope.launch {
        try {
          delay(300)

          val cleanedCardNumber = cardNumber.replace("\\s".toRegex(), "")
          val encryptedCardNumber = XenditEncryption.encrypt(cleanedCardNumber, key, paySid)

          val cardInfoResponse =
            xenditRepository.getCardInfo(
              sessionId = authKey,
              encryptedCardNumber = encryptedCardNumber
            )

          val newCardDetails = if (cardInfoResponse.isSuccessful) cardInfoResponse.body() else null

          if (isValidCreditCard(cleanedCardNumber)) {
            val optionsRequest =
              PaymentOptionsRequest(
                channelCode = "CARDS",
                channelProperties = mapOf("card_number" to encryptedCardNumber)
              )
            val optionsResponse =
              runCatching {
                xenditRepository.getPaymentOptions(
                  sessionId = authKey,
                  request = optionsRequest
                )
              }.getOrNull()

            val newInstallments =
              if (optionsResponse?.isSuccessful == true) {
                val plans = optionsResponse.body()?.installmentPlans
                if (!plans.isNullOrEmpty()) {
                  val amount = optionsResponse.body()?.amount ?: plans.first().totalAmount ?: BigDecimal.ZERO
                  val currencyCode = optionsResponse.body()?.currency ?: "IDR"
                  val formattedAmount = AmountFormat.format(amount, currencyCode)
                  val dummyPlan =
                    InstallmentPlan(
                      interval = "MONTH",
                      intervalCount = 1,
                      terms = 0,
                      installmentAmount = amount,
                      totalAmount = amount,
                      description = formattedAmount,
                      interestRate = 0.0
                    )
                  listOf(dummyPlan) + plans
                } else {
                  null
                }
              } else {
                null
              }
            _state.update { it.copy(installmentPlans = newInstallments) }
          }
          _state.update { it.copy(cardDetails = newCardDetails) }
        } catch (_: Exception) {
          _state.update { it.copy(cardDetails = null, installmentPlans = null) }
        }
      }
  }

}


