package co.xendit.components.ui

import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.xendit.components.XenditComponentsPaymentType
import co.xendit.components.data.model.XenditPaymentResult
import co.xendit.components.internal_entry_point.CardViewModelFactory
import co.xendit.components.internal_entry_point.PaymentViewModelFactory
import co.xendit.components.ui.card.CardIntent
import co.xendit.components.ui.card.CardViewModel
import co.xendit.components.ui.style.XenditAppearance
import co.xendit.components.ui.style.xenditAppearance
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.milliseconds

internal enum class PaymentContainerPresentation {
  Dialog,
  BottomSheet
}

internal class PaymentContainerSessionController {
  private var onWipeRequested: (() -> Unit)? = null
  private var onDismissRequested: (() -> Unit)? = null

  fun bind(
    onWipeRequested: () -> Unit,
    onDismissRequested: () -> Unit
  ) {
    this.onWipeRequested = onWipeRequested
    this.onDismissRequested = onDismissRequested
  }

  fun unbind() {
    onWipeRequested = null
    onDismissRequested = null
  }

  fun requestWipe() {
    onWipeRequested?.invoke()
  }

  fun requestDismiss() {
    onDismissRequested?.invoke()
  }
}

@Composable
private fun ConfigureKeyboardAwareWindow() {
  val view = LocalView.current
  DisposableEffect(view) {
    val dialogWindow = (view.parent as? DialogWindowProvider)?.window
      ?: run {
        var ctx = view.context
        while (ctx is android.content.ContextWrapper) {
          if (ctx is android.app.Activity) return@run ctx.window
          ctx = ctx.baseContext
        }
        null
      }

    dialogWindow?.let { win ->
      win.setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            or WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
      )
      WindowCompat.setDecorFitsSystemWindows(win, false)
    }

    onDispose { }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentContainerHost(
  controller: PaymentContainerSessionController,
  presentation: PaymentContainerPresentation,
  sessionAuthKey: String,
  publicKey: String,
  merchantPreferredPaymentMethod: List<XenditComponentsPaymentType>?,
  style: XenditAppearance,
  onResult: (XenditPaymentResult) -> Unit,
  onCleanup: () -> Unit
) {
  val viewModel: PaymentViewModel =
    viewModel(factory = PaymentViewModelFactory(LocalContext.current))
  val mviState by viewModel.state.collectAsStateWithLifecycle()
  val cardViewModel: CardViewModel =
    viewModel(factory = CardViewModelFactory(LocalContext.current))
  val cardState by cardViewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val appearance = xenditAppearance
  var pendingSnackbarMessage by remember { mutableStateOf<String?>(null) }
  val autofillManager = LocalAutofillManager.current

  val sheetState =
    if (presentation == PaymentContainerPresentation.BottomSheet) {
      rememberModalBottomSheetState(skipPartiallyExpanded = true)
    } else {
      null
    }

  suspend fun performHardWipeAndThen(onWipeFlushed: suspend () -> Unit) {
    delay(15.milliseconds)
    yield()
    viewModel.runFormWipeNonce()
    yield()
    viewModel.wipeAllSensitiveData()
    cardViewModel.wipeAllSensitiveData()
    onWipeFlushed()
  }

  suspend fun finishWith(result: XenditPaymentResult) {
    performHardWipeAndThen {
      onResult(result)
      onCleanup()
    }
  }

  fun cancelAndDismiss() {
    scope.launch {
      if (presentation == PaymentContainerPresentation.BottomSheet && sheetState != null) {
        sheetState.hide()
      }
      finishWith(XenditPaymentResult.Canceled)
    }
  }

  DisposableEffect(viewModel, cardViewModel) {
    controller.bind(
      onWipeRequested = {
        scope.launch {
          performHardWipeAndThen { }
        }
      },
      onDismissRequested = ::cancelAndDismiss
    )
    onDispose {
      controller.unbind()
      viewModel.wipeAllSensitiveData()
      cardViewModel.wipeAllSensitiveData()
    }
  }

  LaunchedEffect(
    pendingSnackbarMessage,
    mviState.paymentActionRedirect,
    mviState.presentToCustomerPaymentAction
  ) {
    val message = pendingSnackbarMessage ?: return@LaunchedEffect
    if (mviState.paymentActionRedirect != null || mviState.presentToCustomerPaymentAction != null) {
      return@LaunchedEffect
    }
    snackbarHostState.showSnackbar(message)
    pendingSnackbarMessage = null
  }

  val dismiss: () -> Unit = ::cancelAndDismiss

  ObservePaymentContainerSideEffects(
    viewModel = viewModel,
    cardViewModel = cardViewModel,
    snackbarHostState = snackbarHostState,
    sessionAuthKey = sessionAuthKey,
    publicKey = publicKey,
    state = mviState,
    context = context,
    onFinish = ::finishWith,
    onEmitResult = onResult,
    onCloseWebPayment = { viewModel.dispatch(ActionIntent.CloseWebPayment) },
    onPendingSnackbar = { pendingSnackbarMessage = it }
  )

  val container: @Composable (@Composable () -> Unit) -> Unit = { content ->
    when (presentation) {
      PaymentContainerPresentation.Dialog -> {
        Dialog(
          onDismissRequest = dismiss,
          properties =
            DialogProperties(
              dismissOnBackPress = true,
              dismissOnClickOutside = false,
              usePlatformDefaultWidth = false,
              decorFitsSystemWindows = false
            )
        ) {
          ConfigureKeyboardAwareWindow()
          Box(
            modifier = Modifier
              .fillMaxSize()
              .semantics { testTagsAsResourceId = true }
              .testTag(XenditTestTags.PAYMENT_DIALOG)
          ) {
            content()
          }
        }
      }

      PaymentContainerPresentation.BottomSheet -> {
        ModalBottomSheet(
          onDismissRequest = dismiss,
          sheetState = sheetState!!,
          containerColor = style.colorBackground,
          contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) },
          modifier = Modifier.testTag(XenditTestTags.PAYMENT_SHEET)
        ) {
          ConfigureKeyboardAwareWindow()
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .fillMaxHeight(0.8f)
              .semantics { testTagsAsResourceId = true }
          ) {
            content()
          }
        }
      }
    }
  }

  container {
    Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      containerColor = style.colorBackground,
      modifier = Modifier
        .fillMaxSize()
        .imePadding()
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
      ) {
        PaymentContainerContent(
          state = mviState,
          cardState = cardState,
          style = style,
          appearance = appearance,
          merchantPreferredPaymentMethod = merchantPreferredPaymentMethod,
          snackbarHostState = snackbarHostState,
          dismiss = dismiss,
          onCleanup = onCleanup,
          viewModel = viewModel,
          cardViewModel = cardViewModel,
          onCommitAutofill = { autofillManager?.commit() },
          context = context
        )
      }
    }
  }
}
