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
    XenditComponents.initialize(appearance = XenditAppearance(fontFamily = openSansFontFamily))

    enableEdgeToEdge()
    setContent {
      XenComponentPrivateTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          PaymentDemo(
            fontFamily = openSansFontFamily,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

@Composable
fun PaymentDemo(fontFamily: FontFamily, modifier: Modifier = Modifier) {
  var sessionId by remember { mutableStateOf("") }
  val context = LocalContext.current
  var paymentResultText by remember { mutableStateOf("") }
  var colorPrimaryHex by remember { mutableStateOf("") }
  var colorTextHex by remember { mutableStateOf("") }
  var colorTextSecondaryHex by remember { mutableStateOf("") }
  var colorTextPlaceholderHex by remember { mutableStateOf("") }
  var colorDisabledHex by remember { mutableStateOf("") }
  var colorDangerHex by remember { mutableStateOf("") }
  var colorBorderHex by remember { mutableStateOf("") }
  var colorBackgroundHex by remember { mutableStateOf("") }
  var qrForegroundHex by remember { mutableStateOf("") }
  var qrBackgroundHex by remember { mutableStateOf("") }
  var borderRadiusDp by remember { mutableStateOf("") }
  var styleError by remember { mutableStateOf("") }

  val onLaunch: () -> Unit = {
    context.findActivity()?.let { act ->
      styleError = ""
      var appearance = XenditAppearance(fontFamily = fontFamily)

      parseColorOrNull(colorPrimaryHex)?.let { appearance = appearance.copy(colorPrimary = it) }
      parseColorOrNull(colorTextHex)?.let { appearance = appearance.copy(colorText = it) }
      parseColorOrNull(colorTextSecondaryHex)?.let {
        appearance = appearance.copy(colorTextSecondary = it)
      }
      parseColorOrNull(colorTextPlaceholderHex)?.let {
        appearance = appearance.copy(colorTextPlaceholder = it)
      }
      parseColorOrNull(colorDisabledHex)?.let { appearance = appearance.copy(colorDisabled = it) }
      parseColorOrNull(colorDangerHex)?.let { appearance = appearance.copy(colorDanger = it) }
      parseColorOrNull(colorBorderHex)?.let { appearance = appearance.copy(colorBorder = it) }
      parseColorOrNull(colorBackgroundHex)?.let {
        appearance = appearance.copy(colorBackground = it)
      }
      parseColorOrNull(qrForegroundHex)?.let {
        appearance = appearance.copy(qrForegroundColor = it)
      }
      parseColorOrNull(qrBackgroundHex)?.let {
        appearance = appearance.copy(qrBackgroundColor = it)
      }

      borderRadiusDp.trim().takeIf { it.isNotBlank() }?.toFloatOrNull()?.let {
        appearance = appearance.copy(borderRadius = it.dp)
      }

      val anyInvalidColor =
        listOf(
          colorPrimaryHex to "colorPrimary",
          colorTextHex to "colorText",
          colorTextSecondaryHex to "colorTextSecondary",
          colorTextPlaceholderHex to "colorTextPlaceholder",
          colorDisabledHex to "colorDisabled",
          colorDangerHex to "colorDanger",
          colorBorderHex to "colorBorder",
          colorBackgroundHex to "colorBackground",
          qrForegroundHex to "qrForegroundColor",
          qrBackgroundHex to "qrBackgroundColor"
        ).firstOrNull { (raw, _) -> raw.isNotBlank() && parseColorOrNull(raw) == null }

      if (anyInvalidColor != null) {
        styleError = "Invalid hex for ${anyInvalidColor.second}"
        return@let
      }
      if (borderRadiusDp.isNotBlank() && borderRadiusDp.trim().toFloatOrNull() == null) {
        styleError = "Invalid borderRadius dp"
        return@let
      }

      XenditComponents.initialize(appearance = appearance)
      XenditComponents.present(
        act,
        sessionId,
        merchantPreferredPaymentMethod =
          listOf(
            XenditComponents.UiGroup.BANK_TRANSFER,
            XenditComponents.UiGroup.CARDS,
            XenditComponents.UiGroup.EWALLET,
            XenditComponents.UiGroup.QR_CODE
          )
      ) {
        paymentResultText = it.toString()
      }
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize(),
    bottomBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .imePadding()
          .background(MaterialTheme.colorScheme.surface)
          .padding(16.dp)
      ) {
        Button(
          onClick = onLaunch,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
        ) {
          Text("Launch Xendit Payment SDK")
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(paddingValues)
        .padding(horizontal = 24.dp)
        .padding(vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Xendit Components Demo",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 12.dp),
      )
      Text(
        text = "Payment",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 24.dp, bottom = 12.dp)
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

      Text(
        text = "Customize Appearance (hex like 21DCCB, #21DCCB or #FF21DCCB)",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp)
      )

      OutlinedTextField(
        value = colorPrimaryHex,
        onValueChange = { colorPrimaryHex = it },
        label = { Text("colorPrimary") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = colorTextHex,
        onValueChange = { colorTextHex = it },
        label = { Text("colorText") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = colorTextSecondaryHex,
        onValueChange = { colorTextSecondaryHex = it },
        label = { Text("colorTextSecondary") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = colorTextPlaceholderHex,
        onValueChange = { colorTextPlaceholderHex = it },
        label = { Text("colorTextPlaceholder") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = colorDisabledHex,
        onValueChange = { colorDisabledHex = it },
        label = { Text("colorDisabled") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = colorDangerHex,
        onValueChange = { colorDangerHex = it },
        label = { Text("colorDanger") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = colorBorderHex,
        onValueChange = { colorBorderHex = it },
        label = { Text("colorBorder") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = colorBackgroundHex,
        onValueChange = { colorBackgroundHex = it },
        label = { Text("colorBackground") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = qrForegroundHex,
        onValueChange = { qrForegroundHex = it },
        label = { Text("qrForegroundColor") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = qrBackgroundHex,
        onValueChange = { qrBackgroundHex = it },
        label = { Text("qrBackgroundColor") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = borderRadiusDp,
        onValueChange = { borderRadiusDp = it },
        label = { Text("borderRadius (dp)") },
        modifier = Modifier.fillMaxWidth()
      )

      if (styleError.isNotBlank()) {
        Text(
          text = styleError,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.Red,
          modifier = Modifier.padding(top = 8.dp)
        )
      }
    }
  }
}

fun Context.findActivity(): ComponentActivity? = when (this) {
  is ComponentActivity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

private fun parseColorOrNull(raw: String): Color? {
  val cleaned =
    raw.trim()
      .removePrefix("Color(")
      .removeSuffix(")")
      .removePrefix("0x")
      .removePrefix("0X")
      .removePrefix("#")
      .replace("_", "")
      .replace(" ", "")

  val hex = cleaned.uppercase()
  val argb =
    when (hex.length) {
      6 -> "FF$hex"
      8 -> hex
      else -> return null
    }

  return runCatching {
    Color(argb.toLong(16))
  }.getOrNull()
}
