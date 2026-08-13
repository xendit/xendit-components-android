package co.xendit.components.ui.digital_wallet

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import co.xendit.components.R
import co.xendit.components.data.model.BffGooglePay
import co.xendit.components.ui.helper.GooglePayHelper
import co.xendit.components.ui.style.xenditAppearance
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.wallet.PaymentData
import com.google.gson.JsonParser
import java.math.BigDecimal

private tailrec fun Context.findActivity(): Activity? {
  return when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
}

private fun extractPaymentMethodType(paymentDataJson: String): String? {
  return runCatching {
    JsonParser.parseString(paymentDataJson)
      ?.asJsonObject
      ?.getAsJsonObject("paymentMethodData")
      ?.get("type")
      ?.asString
  }.getOrNull()
}

internal data class GooglePayPaymentError(
  val code: String,
  val title: String,
  val message: String
)

private fun mapGooglePayStatusToError(statusCodeRaw: Int?): GooglePayPaymentError? {
  val statusCode = statusCodeRaw ?: -1
  val (code, title, message) = when (statusCode) {
    CommonStatusCodes.CANCELED -> {
      // Matches web code 1:1; explicitly ignored (user voluntarily dismissed the sheet).
      Triple("GOOGLE_PAY_CANCELED", "", "")
    }

    CommonStatusCodes.DEVELOPER_ERROR ->
      Triple(
        "GOOGLE_PAY_DEVELOPER_ERROR",
        "Google Pay Error",
        "Something went wrong with Google Pay. Please try again or use a different payment method."
      )

    CommonStatusCodes.INTERNAL_ERROR ->
      Triple(
        "GOOGLE_PAY_INTERNAL_ERROR",
        "Google Pay Error",
        "Something went wrong with Google Pay. Please try again or use a different payment method."
      )

    else ->
      Triple(
        "GOOGLE_PAY_UNKNOWN_ERROR",
        "Google Pay Error",
        "An unknown error occurred with Google Pay. The error code is $statusCode."
      )
  }
  return if (code == "GOOGLE_PAY_CANCELED") null else GooglePayPaymentError(code, title, message)
}

private fun extractStatusCode(exception: Exception?): Int? = runCatching {
  when (exception) {
    is ApiException -> exception.statusCode
    is ResolvableApiException -> exception.statusCode
    else -> null
  }
}.getOrNull()

private fun handlePaymentData(
  paymentData: PaymentData?,
  googlePay: BffGooglePay?,
  onPaymentDataReceived: (paymentDataJson: String, paymentMethodType: String?) -> Unit
) {
  val json = paymentData?.toJson() ?: return
  val paymentMethodType = extractPaymentMethodType(json)
  onPaymentDataReceived(json, paymentMethodType)
}

@Composable
internal fun GooglePaySection(
  googlePay: BffGooglePay?,
  businessName: String,
  paymentSessionId: String?,
  amount: BigDecimal?,
  currency: String?,
  isTest: Boolean,
  isLoading: Boolean,
  onPaymentDataReceived: (paymentDataJson: String, paymentMethodType: String?) -> Unit,
  onPaymentFailed: (GooglePayPaymentError) -> Unit,
  modifier: Modifier = Modifier
) {
  if (googlePay == null ||
    paymentSessionId == null ||
    amount == null ||
    currency.isNullOrBlank()
  ) return

  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() } ?: return

  val paymentsClient = remember(activity, isTest) {
    GooglePayHelper.createPaymentsClient(activity, isTest)
  }

  var isReady by remember { mutableStateOf(false) }

  DisposableEffect(googlePay) {
    val readyRequest = GooglePayHelper.createIsReadyToPayRequest(googlePay)
    val task = paymentsClient.isReadyToPay(readyRequest)
    task.addOnSuccessListener { result ->
      isReady = result
    }
    task.addOnFailureListener {
      isReady = false
    }
    onDispose { /* no-op */ }
  }

  val paymentDataRequest = remember(googlePay, businessName, paymentSessionId, amount, currency) {
    runCatching {
      GooglePayHelper.createPaymentDataRequest(
        googlePay = googlePay,
        businessName = businessName,
        paymentSessionId = paymentSessionId,
        amount = amount,
        currency = currency
      )
    }.getOrNull()
  }

  val activityResultLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartIntentSenderForResult()
  ) { result: ActivityResult ->
    when (result.resultCode) {
      Activity.RESULT_OK -> {
        val data: Intent? = result.data
        val paymentData = if (data != null) PaymentData.getFromIntent(data) else null
        handlePaymentData(paymentData, googlePay, onPaymentDataReceived)
      }

      Activity.RESULT_CANCELED -> {
        // User canceled the resolution flow (dismissed dialog, etc.) — same as web CANCELED: ignore
      }

      else -> {
        val statusExtra = runCatching {
          result.data
            ?.getIntExtra("com.google.android.gms.common.api.AutoResolveHelper.status", -1)
            ?.takeIf { it != -1 }
        }.getOrNull()
        mapGooglePayStatusToError(statusExtra)?.let(onPaymentFailed)
      }
    }
  }

  if (!isReady || paymentDataRequest == null) return

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    GooglePayButton(
      isLoading = isLoading,
      onClick = {
        val loadTask = paymentsClient.loadPaymentData(paymentDataRequest)
        loadTask.addOnSuccessListener { paymentData ->
          handlePaymentData(paymentData, googlePay, onPaymentDataReceived)
        }
        loadTask.addOnFailureListener { exception ->
          if (exception is ResolvableApiException) {
            val pending = exception.resolution
            val intentSenderRequest = IntentSenderRequest.Builder(pending).build()
            activityResultLauncher.launch(intentSenderRequest)
          } else {
            val exceptionAsStatus = extractStatusCode(exception as? Exception)
            val mappedError = mapGooglePayStatusToError(exceptionAsStatus)
              ?: mapGooglePayStatusToError(null)
            mappedError?.let(onPaymentFailed)
          }
        }
      }
    )
  }
}

@Composable
internal fun GooglePayButton(
  isLoading: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val appearance = xenditAppearance
  val interactionSource = remember { MutableInteractionSource() }
  val isLightBackground = remember(appearance.colorBackground) {
    appearance.colorBackground.luminance() > 0.65f
  }
  val buttonBgColor = if (isLightBackground) Color.Black else Color.White
  val textColor = if (isLightBackground) Color.White else Color.Black
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .background(
        color = buttonBgColor,
        shape = RoundedCornerShape(12.dp)
      )
      .then(
        if (isLoading) Modifier
        else Modifier.clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick
        )
      )
      .padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        color = Color.White,
        strokeWidth = 2.dp,
        modifier = Modifier.size(20.dp)
      )
    } else {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_google_pay),
          contentDescription = "Google Logo",
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Pay",
          color = textColor,
          style = MaterialTheme.typography.titleLarge,
        )
      }
    }
  }
}
