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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.xendit.components.XenditComponents
import co.xendit.components.XenditComponentsPaymentType
import co.xendit.components.data.model.XenditPaymentResult
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
        PaymentDemo(
          fontFamily = openSansFontFamily
        )
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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
  var selectedPreset by remember { mutableStateOf(AppearancePreset.Custom) }
  var presetExpanded by remember { mutableStateOf(false) }
  val appearanceInputsEnabled = selectedPreset == AppearancePreset.Custom

  val onLaunch: () -> Unit = {
    context.findActivity()?.let { act ->
      styleError = ""
      val appearance =
        if (selectedPreset == AppearancePreset.Custom) {
          var customAppearance = XenditAppearance(fontFamily = fontFamily)

          parseColorOrNull(colorPrimaryHex)?.let {
            customAppearance = customAppearance.copy(colorPrimary = it)
          }
          parseColorOrNull(colorTextHex)?.let {
            customAppearance = customAppearance.copy(colorText = it)
          }
          parseColorOrNull(colorTextSecondaryHex)?.let {
            customAppearance = customAppearance.copy(colorTextSecondary = it)
          }
          parseColorOrNull(colorTextPlaceholderHex)?.let {
            customAppearance = customAppearance.copy(colorTextPlaceholder = it)
          }
          parseColorOrNull(colorDisabledHex)?.let {
            customAppearance = customAppearance.copy(colorDisabled = it)
          }
          parseColorOrNull(colorDangerHex)?.let {
            customAppearance = customAppearance.copy(colorDanger = it)
          }
          parseColorOrNull(colorBorderHex)?.let {
            customAppearance = customAppearance.copy(colorBorder = it)
          }
          parseColorOrNull(colorBackgroundHex)?.let {
            customAppearance = customAppearance.copy(colorBackground = it)
          }
          parseColorOrNull(qrForegroundHex)?.let {
            customAppearance = customAppearance.copy(qrForegroundColor = it)
          }
          parseColorOrNull(qrBackgroundHex)?.let {
            customAppearance = customAppearance.copy(qrBackgroundColor = it)
          }

          borderRadiusDp.trim().takeIf { it.isNotBlank() }?.toFloatOrNull()?.let {
            customAppearance = customAppearance.copy(borderRadius = it.dp)
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

          customAppearance
        } else {
          presetAppearance(preset = selectedPreset, openSansFontFamily = fontFamily)
        }

      XenditComponents.initialize(appearance = appearance)
      XenditComponents.present(
        act,
        sessionId,
        merchantPreferredPaymentMethod =
          listOf(
            XenditComponentsPaymentType.QR_CODE,
            XenditComponentsPaymentType.BANK_TRANSFER,
            XenditComponentsPaymentType.DIRECT_DEBIT,
            XenditComponentsPaymentType.VIRTUAL_ACCOUNT,
            XenditComponentsPaymentType.CARDS,
            XenditComponentsPaymentType.EWALLET,
            XenditComponentsPaymentType.OVER_THE_COUNTER,
            XenditComponentsPaymentType.GOOGLE_PAY
          )
      ) { result ->
        paymentResultText = result.toString()
        if (result !is XenditPaymentResult.Failed) {
          XenditComponents.wipeAllSensitiveData()
          XenditComponents.performSensitiveDataGcPass()
        }
      }
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .imePadding()
      .navigationBarsPadding()
      .semantics { testTagsAsResourceId = true },
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
            .testTag("launch_payment_button")
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
        modifier = Modifier
          .fillMaxWidth()
          .testTag("session_id_input")
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
        text = "Preset Style",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 24.dp, bottom = 12.dp)
      )

      ExposedDropdownMenuBox(
        expanded = presetExpanded,
        onExpandedChange = { presetExpanded = !presetExpanded },
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = selectedPreset.displayName(),
          onValueChange = {},
          readOnly = true,
          label = { Text("Choose a preset") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
          modifier = Modifier
            .menuAnchor(
              type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
              enabled = true
            )
            .fillMaxWidth()
        )
        ExposedDropdownMenu(
          expanded = presetExpanded,
          onDismissRequest = { presetExpanded = false }
        ) {
          AppearancePreset.values().forEach { preset ->
            DropdownMenuItem(
              text = { Text(preset.displayName()) },
              onClick = {
                selectedPreset = preset
                presetExpanded = false
                styleError = ""
              }
            )
          }
        }
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
        modifier = Modifier.fillMaxWidth(),
        enabled = appearanceInputsEnabled
      )
      OutlinedTextField(
        value = colorTextHex,
        onValueChange = { colorTextHex = it },
        label = { Text("colorText") },
        modifier = Modifier.fillMaxWidth(),
        enabled = appearanceInputsEnabled
      )
      OutlinedTextField(
        value = colorTextSecondaryHex,
        onValueChange = { colorTextSecondaryHex = it },
        label = { Text("colorTextSecondary") },
        modifier = Modifier.fillMaxWidth(),
        enabled = appearanceInputsEnabled
      )
      OutlinedTextField(
        value = colorTextPlaceholderHex,
        onValueChange = { colorTextPlaceholderHex = it },
        label = { Text("colorTextPlaceholder") },
        modifier = Modifier.fillMaxWidth(),
        enabled = appearanceInputsEnabled
      )
      OutlinedTextField(
        value = colorDisabledHex,
        onValueChange = { colorDisabledHex = it },
        label = { Text("colorDisabled") },
        modifier = Modifier.fillMaxWidth(),
        enabled = appearanceInputsEnabled
      )
      OutlinedTextField(
        value = colorDangerHex,
        onValueChange = { colorDangerHex = it },
        label = { Text("colorDanger") },
        modifier = Modifier.fillMaxWidth(),
        enabled = appearanceInputsEnabled
      )
      OutlinedTextField(
        value = colorBorderHex,
        onValueChange = { colorBorderHex = it },
        label = { Text("colorBorder") },
        modifier = Modifier.fillMaxWidth(),
        enabled = appearanceInputsEnabled
      )
      OutlinedTextField(
        value = colorBackgroundHex,
        onValueChange = { colorBackgroundHex = it },
        label = { Text("colorBackground") },
        modifier = Modifier.fillMaxWidth(),
        enabled = appearanceInputsEnabled
      )
      OutlinedTextField(
        value = qrForegroundHex,
        onValueChange = { qrForegroundHex = it },
        label = { Text("qrForegroundColor") },
        modifier = Modifier.fillMaxWidth(),
        enabled = appearanceInputsEnabled
      )
      OutlinedTextField(
        value = qrBackgroundHex,
        onValueChange = { qrBackgroundHex = it },
        label = { Text("qrBackgroundColor") },
        modifier = Modifier.fillMaxWidth(),
        enabled = appearanceInputsEnabled
      )
      OutlinedTextField(
        value = borderRadiusDp,
        onValueChange = { borderRadiusDp = it },
        label = { Text("borderRadius (dp)") },
        modifier = Modifier.fillMaxWidth(),
        enabled = appearanceInputsEnabled
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

private enum class AppearancePreset {
  Custom,
  DailyBrew,
  FintechBlue,
  Arcade,
  Boutique,
  MidnightCyber,
  LavenderFields,
  SunsetGlow,
  MintFresh,
}

private fun AppearancePreset.displayName(): String =
  when (this) {
    AppearancePreset.Custom -> "Custom"
    AppearancePreset.DailyBrew -> "Daily Brew"
    AppearancePreset.FintechBlue -> "Fintech Blue"
    AppearancePreset.Arcade -> "Arcade"
    AppearancePreset.Boutique -> "Boutique"
    AppearancePreset.MidnightCyber -> "Midnight Cyber"
    AppearancePreset.LavenderFields -> "Lavender Fields"
    AppearancePreset.SunsetGlow -> "Sunset Glow"
    AppearancePreset.MintFresh -> "Mint Fresh"
  }

private fun presetAppearance(
  preset: AppearancePreset,
  openSansFontFamily: FontFamily,
): XenditAppearance =
  when (preset) {
    AppearancePreset.Custom ->
      XenditAppearance(fontFamily = openSansFontFamily)

    AppearancePreset.DailyBrew ->
      XenditAppearance(
        fontFamily = openSansFontFamily,
        colorPrimary = Color(0xFF8D6E63),
        borderRadius = 12.dp,
        colorBackground = Color(0xFFFFFBF0),
        colorText = Color(0xFF3E2723),
        colorBorder = Color(0xFFD7CCC8),
        colorTextSecondary = Color(0xFF795548),
        colorTextPlaceholder = Color(0xFFA1887F),
        colorDanger = Color(0xFFD32F2F),
        qrForegroundColor = Color(0xFF3E2723),
        qrBackgroundColor = Color(0xFFFFFBF0),
      )

    AppearancePreset.FintechBlue ->
      XenditAppearance(
        fontFamily = openSansFontFamily,
        colorPrimary = Color(0xFF0052FF),
        borderRadius = 6.dp,
        colorBackground = Color(0xFFFFFFFF),
        colorText = Color(0xFF111827),
        colorBorder = Color(0xFFE5E7EB),
        colorTextSecondary = Color(0xFF6B7280),
        colorTextPlaceholder = Color(0xFF9CA3AF),
        colorDanger = Color(0xFFDC2626),
        qrForegroundColor = Color(0xFF0052FF),
        qrBackgroundColor = Color(0xFFFFFFFF),
      )

    AppearancePreset.Arcade ->
      XenditAppearance(
        fontFamily = FontFamily.Monospace,
        colorPrimary = Color(0xFF00FFD1),
        borderRadius = 4.dp,
        colorBackground = Color(0xFF000000),
        colorText = Color(0xFFFFFFFF),
        colorBorder = Color(0xFF00FFD1),
        colorTextSecondary = Color(0xFF888888),
        colorTextPlaceholder = Color(0xFF333333),
        colorDisabled = Color(0xFF1A1A1A),
        colorDanger = Color(0xFFFF0055),
        qrForegroundColor = Color(0xFF000000),
        qrBackgroundColor = Color(0xFF00FFD1),
      )

    AppearancePreset.Boutique ->
      XenditAppearance(
        fontFamily = FontFamily.Serif,
        colorPrimary = Color(0xFF2C2C2C),
        borderRadius = 0.dp,
        colorBackground = Color(0xFFF4F1EA),
        colorText = Color(0xFF2C2C2C),
        colorBorder = Color(0xFF2C2C2C),
        colorTextSecondary = Color(0xFF5A5A5A),
        colorTextPlaceholder = Color(0xFFAAAAAA),
        colorDanger = Color(0xFF941B1B),
        qrForegroundColor = Color(0xFF2C2C2C),
        qrBackgroundColor = Color(0xFFF4F1EA),
      )

    AppearancePreset.MidnightCyber ->
      XenditAppearance(
        fontFamily = FontFamily.Monospace,
        colorPrimary = Color(0xFF00E5FF),
        borderRadius = 8.dp,
        colorBackground = Color(0xFF0B0E14),
        colorText = Color(0xFFF5F7FA),
        colorBorder = Color(0xFF1F2633),
        colorTextSecondary = Color(0xFF94A3B8),
        colorTextPlaceholder = Color(0xFF475569),
        colorDanger = Color(0xFFFF4655),
        qrForegroundColor = Color(0xFFF5F7FA),
        qrBackgroundColor = Color(0xFF0B0E14),
      )

    AppearancePreset.LavenderFields ->
      XenditAppearance(
        fontFamily = FontFamily.Serif,
        colorPrimary = Color(0xFF6D28D9),
        borderRadius = 6.dp,
        colorBackground = Color(0xFFFAF5FF),
        colorText = Color(0xFF2E1065),
        colorBorder = Color(0xFFE9D5FF),
        colorTextSecondary = Color(0xFF6B21A8),
        colorTextPlaceholder = Color(0xFFC084FC),
        colorDanger = Color(0xFFE11D48),
        qrForegroundColor = Color(0xFF2E1065),
        qrBackgroundColor = Color(0xFFFAF5FF),
      )

    AppearancePreset.SunsetGlow ->
      XenditAppearance(
        fontFamily = FontFamily.Default,
        colorPrimary = Color(0xFFF97316),
        borderRadius = 24.dp,
        colorBackground = Color(0xFFFFF7ED),
        colorText = Color(0xFF431407),
        colorBorder = Color(0xFFFFEDD5),
        colorTextSecondary = Color(0xFF9A3412),
        colorTextPlaceholder = Color(0xFFC2410C),
        colorDanger = Color(0xFFDC2626),
        qrForegroundColor = Color(0xFF431407),
        qrBackgroundColor = Color(0xFFFFF7ED),
      )

    AppearancePreset.MintFresh ->
      XenditAppearance(
        fontFamily = FontFamily.SansSerif,
        colorPrimary = Color(0xFF10B981),
        borderRadius = 16.dp,
        colorBackground = Color(0xFFF0FDF4),
        colorText = Color(0xFF064E3B),
        colorBorder = Color(0xFFDCFCE7),
        colorTextSecondary = Color(0xFF374151),
        colorTextPlaceholder = Color(0xFF9CA3AF),
        colorDanger = Color(0xFFF43F5E),
        qrForegroundColor = Color(0xFF064E3B),
        qrBackgroundColor = Color(0xFFF0FDF4),
      )
  }
