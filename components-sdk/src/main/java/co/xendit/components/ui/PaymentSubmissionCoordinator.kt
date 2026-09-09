package co.xendit.components.ui

import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.ChannelFormField
import co.xendit.components.data.model.PaymentAction
import co.xendit.components.data.model.PaymentActionDescriptor
import co.xendit.components.data.model.PaymentRequest
import co.xendit.components.data.model.PaymentRequestStatus
import co.xendit.components.data.model.PaymentResponse
import co.xendit.components.data.model.primaryChannelPropertyKey
import co.xendit.components.data.model.usesPaymentTokenSubmission
import co.xendit.components.data.network.repo.session.XenditRepository
import co.xendit.components.data.network.repo.session.XenditRepositoryResult

internal sealed interface PaymentPresentationTarget {
  data class Redirect(val action: PaymentAction) : PaymentPresentationTarget
  data class PresentToCustomer(val action: PaymentAction) : PaymentPresentationTarget
  data object AwaitingActionList : PaymentPresentationTarget
}

internal data class PaymentSubmissionSuccess(
  val response: PaymentResponse,
  val presentationTarget: PaymentPresentationTarget? = null
)

internal sealed interface PaymentSubmissionResult {
  data class ValidationFailure(
    val validationError: String,
    val userMessage: String
  ) : PaymentSubmissionResult

  data class ApiFailure(
    val errorCode: String,
    val errorMessage: String
  ) : PaymentSubmissionResult

  data class UnexpectedFailure(val errorMessage: String) : PaymentSubmissionResult
  data class Success(val data: PaymentSubmissionSuccess) : PaymentSubmissionResult
}

internal class PaymentSubmissionCoordinator(
  private val repository: XenditRepository
) {
  suspend fun submit(
    sessionType: BffSessionType?,
    context: PaymentExecutionContext,
    formValues: Map<String, String>,
    fields: List<ChannelFormField>,
    buildRequest: suspend (PaymentExecutionContext) -> PaymentRequest
  ): PaymentSubmissionResult {
    val validationError = requiredValidationError(formValues, fields)
    if (validationError != null) {
      return PaymentSubmissionResult.ValidationFailure(
        validationError = validationError,
        userMessage = "Missing required field: $validationError"
      )
    }

    return try {
      val request = buildRequest(context)
      when (
        val response =
          if (sessionType.usesPaymentTokenSubmission()) {
            repository.createPaymentToken(request)
          } else {
            repository.createPaymentRequest(request)
          }
      ) {
        is XenditRepositoryResult.Success -> {
          PaymentSubmissionResult.Success(
            PaymentSubmissionSuccess(
              response = response.data,
              presentationTarget = classifyPresentationTarget(response.data)
            )
          )
        }

        is XenditRepositoryResult.Failure -> {
          PaymentSubmissionResult.ApiFailure(
            errorCode = response.errorCode ?: "-1",
            errorMessage = response.apiError?.errorContent?.message1
              ?: response.message
              ?: "Payment Failed"
          )
        }

        is XenditRepositoryResult.EmptyBody -> {
          PaymentSubmissionResult.ApiFailure(
            errorCode = "EMPTY_BODY",
            errorMessage = "Payment Failed"
          )
        }
      }
    } catch (e: Exception) {
      PaymentSubmissionResult.UnexpectedFailure(
        errorMessage = e.message ?: "Payment Error"
      )
    }
  }

  private fun requiredValidationError(
    formValues: Map<String, String>,
    fields: List<ChannelFormField>
  ): String? {
    return run validation@{
      fields
        .filter { it.required }
        .forEach { field ->
          val key = field.primaryChannelPropertyKey()
          val value = formValues[key]?.takeIf { it.isNotBlank() }
          if (value == null) {
            return@validation "${key.uppercase()}_REQUIRED"
          }
        }
      null
    }
  }

  private fun classifyPresentationTarget(response: PaymentResponse): PaymentPresentationTarget? {
    val actions = response.paymentActions.orEmpty()
    val redirect =
      actions.firstOrNull {
        it.type == "REDIRECT_CUSTOMER" &&
          (it.descriptor == PaymentActionDescriptor.WEB_URL ||
            it.descriptor == PaymentActionDescriptor.DEEPLINK_URL ||
            it.descriptor == PaymentActionDescriptor.WEB_GOOGLE_PAYLINK)
      }

    if (response.status != PaymentRequestStatus.REQUIRES_ACTION) {
      return null
    }

    val presentToCustomer =
      actions.firstOrNull {
        it.type == "PRESENT_TO_CUSTOMER" &&
          it.value != null &&
          (it.descriptor == PaymentActionDescriptor.VIRTUAL_ACCOUNT_NUMBER ||
            it.descriptor == PaymentActionDescriptor.QR_STRING)
      } ?: actions.firstOrNull { it.type == "PRESENT_TO_CUSTOMER" && it.value != null }

    return when {
      redirect?.value != null -> PaymentPresentationTarget.Redirect(redirect)
      presentToCustomer != null -> PaymentPresentationTarget.PresentToCustomer(presentToCustomer)
      actions.isEmpty() -> PaymentPresentationTarget.AwaitingActionList
      else -> null
    }
  }
}
