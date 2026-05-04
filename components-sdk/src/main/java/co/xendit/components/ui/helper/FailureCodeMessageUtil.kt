package co.xendit.components.ui.helper

import android.content.Context
import androidx.annotation.StringRes
import co.xendit.components.R
import java.util.Locale

internal object FailureCodeMessageUtil {
  fun resolveFailureMessage(
    context: Context,
    failureCode: String?,
  ): String {
    val raw = failureCode?.trim().orEmpty()
    val normalized = raw.uppercase(Locale.US)
    val resId = failureCodeToResIdOrNull(normalized)

    return if (resId != null) {
      context.getString(resId)
    } else {
      val code = raw.ifBlank { "UNKNOWN" }
      context.getString(R.string.sessionfailure_code_unknown)
        .replace("{{failureCode}}", code)
    }
  }

  @StringRes
  private fun failureCodeToResIdOrNull(
    normalizedFailureCode: String,
  ): Int? {
    return when (normalizedFailureCode) {
      "ACCOUNT_ACCESS_BLOCKED" -> R.string.sessionfailure_code_account_access_blocked
      "ACCOUNT_ALREADY_LINKED" -> R.string.sessionfailure_code_account_already_linked
      "ACCOUNT_NOT_ACTIVATED" -> R.string.sessionfailure_code_account_not_activated
      "AUTHENTICATION_FAILED" -> R.string.sessionfailure_code_authentication_failed
      "CAPTURE_AMOUNT_EXCEEDED" -> R.string.sessionfailure_code_capture_amount_exceeded
      "CARD_DECLINED" -> R.string.sessionfailure_code_card_declined
      "CHANNEL_UNAVAILABLE" -> R.string.sessionfailure_code_channel_unavailable
      "DECLINED_BY_ISSUER" -> R.string.sessionfailure_code_declined_by_issuer
      "DECLINED_BY_PROCESSOR" -> R.string.sessionfailure_code_declined_by_processor
      "EXPIRED_CARD" -> R.string.sessionfailure_code_expired_card
      "EXPIRED_OTP" -> R.string.sessionfailure_code_expired_otp
      "FAILURE_DETAILS_UNAVAILABLE" -> R.string.sessionfailure_code_failure_details_unavailable
      "INACTIVE_OR_UNAUTHORIZED_CARD" -> R.string.sessionfailure_code_inactive_or_unauthorized_card
      "INSUFFICIENT_BALANCE" -> R.string.sessionfailure_code_insufficient_balance
      "INVALID_ACCOUNT_DETAILS" -> R.string.sessionfailure_code_invalid_account_details
      "INVALID_CVV" -> R.string.sessionfailure_code_invalid_cvv
      "INVALID_MERCHANT_SETTINGS" -> R.string.sessionfailure_code_invalid_merchant_settings
      "INVALID_OTP" -> R.string.sessionfailure_code_invalid_otp
      "INVALID_TOKEN" -> R.string.sessionfailure_code_invalid_token
      "ISSUER_UNAVAILABLE" -> R.string.sessionfailure_code_issuer_unavailable
      "OTP_ATTEMPT_COUNTS_EXCEEDED" -> R.string.sessionfailure_code_otp_attempt_counts_exceeded
      "PARTNER_TIMEOUT_ERROR" -> R.string.sessionfailure_code_partner_timeout_error
      "PAYMENT_AMOUNT_LIMITS_EXCEEDED" -> R.string.sessionfailure_code_payment_amount_limits_exceeded
      "PAYMENT_ATTEMPT_COUNTS_EXCEEDED" -> R.string.sessionfailure_code_payment_attempt_counts_exceeded
      "PAYMENT_REQUEST_EXPIRED" -> R.string.sessionfailure_code_payment_request_expired
      "PROCESSOR_ERROR" -> R.string.sessionfailure_code_processor_error
      "SERVER_ERROR" -> R.string.sessionfailure_code_server_error
      "STOLEN_CARD" -> R.string.sessionfailure_code_stolen_card
      "SUSPECTED_FRAUDULENT" -> R.string.sessionfailure_code_suspected_fraudulent
      "TIMEOUT_ERROR" -> R.string.sessionfailure_code_timeout_error
      "USER_DECLINED_PAYMENT" -> R.string.sessionfailure_code_user_declined_payment
      "USER_DEVICE_UNREACHABLE" -> R.string.sessionfailure_code_user_device_unreachable
      "USER_DID_NOT_AUTHORIZE" -> R.string.sessionfailure_code_user_did_not_authorize
      else -> null
    }
  }
}
