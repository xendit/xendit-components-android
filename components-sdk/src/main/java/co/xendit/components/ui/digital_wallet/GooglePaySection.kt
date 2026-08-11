package co.xendit.components.ui.digital_wallet

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.wallet.PaymentData
import co.xendit.components.data.model.BffGooglePay
import co.xendit.components.ui.helper.GooglePayHelper
import java.math.BigDecimal

private tailrec fun Context.findActivity(): Activity? {
  return when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
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
  onPaymentDataReceived: (paymentDataJson: String, channelCode: String) -> Unit,
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
    val data: android.content.Intent? = result.data
    when (result.resultCode) {
      Activity.RESULT_OK -> {
        val paymentData = if (data != null) PaymentData.getFromIntent(data) else null
        val json = paymentData?.toJson()
        if (json != null) {
          val channelCode =
            googlePay.allowedPaymentMethods.firstOrNull()?.channelCode ?: "CARDS"
          onPaymentDataReceived(json, channelCode)
        }
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
          val json = paymentData.toJson()
          if (json != null) {
            val channelCode =
              googlePay.allowedPaymentMethods.firstOrNull()?.channelCode ?: "CARDS"
            onPaymentDataReceived(json, channelCode)
          }
        }
        loadTask.addOnFailureListener { exception ->
          if (exception is ResolvableApiException) {
            val pending = exception.resolution
            val intentSenderRequest = IntentSenderRequest.Builder(pending).build()
            activityResultLauncher.launch(intentSenderRequest)
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
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .background(
        color = Color.Black,
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
      Text(
        text = "Pay with Google Pay",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}
