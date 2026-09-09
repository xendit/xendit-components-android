package co.xendit.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.xendit.components.BuildConfig
import co.xendit.components.R
import co.xendit.components.XenditComponentsPaymentType
import co.xendit.components.ui.card.CardState
import co.xendit.components.ui.card.CardViewModel
import co.xendit.components.ui.style.XenditAppearance

@Composable
internal fun PaymentContainerContent(
  state: PaymentState,
  cardState: CardState,
  style: XenditAppearance,
  appearance: XenditAppearance,
  merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>?,
  snackbarHostState: SnackbarHostState,
  dismiss: () -> Unit,
  onCleanup: () -> Unit,
  viewModel: PaymentViewModel,
  cardViewModel: CardViewModel,
  onCommitAutofill: () -> Unit,
  context: android.content.Context
) {
  Column(modifier = Modifier.fillMaxSize()) {
    PaymentDebugBanner(
      appearance = appearance,
      referenceId = state.sessionResponse?.session?.referenceId
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      PaymentFlowContent(
        state = state,
        cardState = cardState,
        style = style,
        appearance = appearance,
        merchantPreferredPaymentMethod = merchantPreferredPaymentMethod,
        snackbarHostState = snackbarHostState,
        dismiss = dismiss,
        viewModel = viewModel,
        cardViewModel = cardViewModel,
        onCommitAutofill = onCommitAutofill,
        context = context
      )
    }
  }

  StartupErrorDialog(
    flowState = state.flowState,
    onCleanup = onCleanup
  )
  PaymentOverlay(
    state = state,
    style = style,
    onClose = { viewModel.dispatch(ActionIntent.CloseWebPayment) }
  )
}

@Composable
internal fun PaymentDebugBanner(
  appearance: XenditAppearance,
  referenceId: String?
) {
  if (!BuildConfig.DEBUG) return

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(appearance.borderRadius))
      .background(Color(0xFFF7F7F7))
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Column {
      Text(
        text = "Debug = ${BuildConfig.DEBUG}",
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray
      )
      Text(
        text = referenceId.orEmpty(),
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray
      )
    }
  }
}
