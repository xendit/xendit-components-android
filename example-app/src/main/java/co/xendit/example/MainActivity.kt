package co.xendit.example

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.xendit.components.XenditComponents
import co.xendit.components.ui.style.XenditAppearance
import co.xendit.example.ui.theme.XenComponentPrivateTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val openSansFontFamily = FontFamily(
      // Light
      Font(R.font.open_sans_light, FontWeight.Light, FontStyle.Normal),
      Font(R.font.open_sans_light_italic, FontWeight.Light, FontStyle.Italic),

      // Regular
      Font(R.font.open_sans_regular, FontWeight.Normal, FontStyle.Normal),
      Font(R.font.open_sans_italic, FontWeight.Normal, FontStyle.Italic),

      // SemiBold
      Font(R.font.open_sans_semi_bold, FontWeight.SemiBold, FontStyle.Normal),
      Font(R.font.open_sans_semi_bold_italic, FontWeight.SemiBold, FontStyle.Italic),

      // Bold
      Font(R.font.open_sans_bold, FontWeight.Bold, FontStyle.Normal),
      Font(R.font.open_sans_bold_italic, FontWeight.Bold, FontStyle.Italic),

      // ExtraBold
      Font(R.font.open_sans_extra_bold, FontWeight.ExtraBold, FontStyle.Normal),
      Font(R.font.open_sans_extra_bold_italic, FontWeight.ExtraBold, FontStyle.Italic)
    )
    val customStyle = XenditAppearance(
      fontFamily = openSansFontFamily,
//      colorPrimary = Color(0xFF21DCCB),
//      borderRadius = 2.dp,
//      colorText = Color(0xFF035E6C),
//      colorTextSecondary = Color(0xFF44474E),
//      colorTextPlaceholder = Color(0xFFAAAAAA),
//      colorDisabled = Color(0xFFCCCCCC),
//      colorDanger = Color(0xFFFF0000),
//      colorBorder = Color(0xFF0A7F8D),
//      colorBackground = Color(0xFFCCE3E5),
//      qrForegroundColor = Color(0xFF000000),
//      qrBackgroundColor = Color(0xFFFFFFFF),
    )
    findActivity()?.let { act ->
      XenditComponents.initialize(
        appearance = customStyle,
//      merchantPreferredPaymentMethod = listOf("cards")
      )
    }

    enableEdgeToEdge()
    setContent {
      XenComponentPrivateTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          PaymentDemo(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

@Composable
fun PaymentDemo(modifier: Modifier = Modifier) {
  var sessionId by remember { mutableStateOf("") }
  val context = LocalContext.current
  var paymentResultText by remember { mutableStateOf("") }


  Box(modifier = modifier.fillMaxSize()) {
    // 1. SCROLLABLE CONTENT (Top & Middle)
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(
          top = 60.dp,
          bottom = 120.dp
        ), // Large bottom padding so text doesn't hide behind button
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Xendit Components Demo",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 32.dp),
      )

      OutlinedTextField(
        value = sessionId,
        onValueChange = { sessionId = it },
        label = { Text("Enter Session ID") },
        modifier = Modifier.fillMaxWidth()
      )

      if (paymentResultText.isNotEmpty()) {
        Text(
          text = paymentResultText,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.padding(top = 24.dp),
          color = if (paymentResultText.startsWith("Success")) Color.Green else Color.Red
        )
      }
    }

    // 2. FIXED BUTTON (Bottom)
    // This Box is NOT inside the scrollable Column
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .imePadding()
        .background(MaterialTheme.colorScheme.surface) // Prevents text from bleeding through
        .padding(16.dp)
    ) {
      Button(
        onClick = {

          context.findActivity()?.let { act ->
            XenditComponents.present(
              act,
              sessionId,
              merchantPreferredPaymentMethod = listOf("cards", "qr_code")
            ) {
              paymentResultText = "Success: ${it.toString()}"
            }
          }

        },
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
      ) {
        Text("Launch Xendit Payment SDK")
      }
    }
  }
}

fun Context.findActivity(): ComponentActivity? = when (this) {
  is ComponentActivity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}
