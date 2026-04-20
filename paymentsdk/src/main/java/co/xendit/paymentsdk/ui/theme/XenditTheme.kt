package co.xendit.paymentsdk.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import co.xendit.paymentsdk.ui.components.molecule.XenditTextField
import co.xendit.paymentsdk.ui.style.LocalXenditAppearance
import co.xendit.paymentsdk.ui.style.XenditAppearance

/** A custom MaterialTheme that applies the partner's [XenditAppearance]. */
@Composable
internal fun XenditTheme(style: XenditAppearance, content: @Composable () -> Unit) {
  val colorScheme =
    lightColorScheme(
      primary = style.colorPrimary,
      onPrimary = style.colorBackground,
      surface = style.colorBackground,
      onSurface = style.colorText,
      onSurfaceVariant = style.colorTextSecondary,
      error = style.colorDanger,
      outline = style.colorBorder,
      surfaceTint = Color.Transparent,
      surfaceVariant = style.colorBackground,
      surfaceContainer = style.colorBackground,
      surfaceContainerLow = style.colorBackground,
      surfaceContainerHigh = style.colorBackground,
      surfaceContainerLowest = style.colorBackground
    )

  val baseTypography = Typography()
  val typography =
    baseTypography.copy(
      displayLarge = baseTypography.displayLarge.copy(fontFamily = style.fontFamily),
      displayMedium = baseTypography.displayMedium.copy(fontFamily = style.fontFamily),
      displaySmall = baseTypography.displaySmall.copy(fontFamily = style.fontFamily),
      headlineLarge = baseTypography.headlineLarge.copy(fontFamily = style.fontFamily),
      headlineMedium = baseTypography.headlineMedium.copy(fontFamily = style.fontFamily),
      headlineSmall = baseTypography.headlineSmall.copy(fontFamily = style.fontFamily),
      titleLarge = baseTypography.titleLarge.copy(fontFamily = style.fontFamily),
      titleMedium = baseTypography.titleMedium.copy(fontFamily = style.fontFamily),
      titleSmall = baseTypography.titleSmall.copy(fontFamily = style.fontFamily),
      bodyLarge = baseTypography.bodyLarge.copy(fontFamily = style.fontFamily),
      bodyMedium = baseTypography.bodyMedium.copy(fontFamily = style.fontFamily),
      bodySmall = baseTypography.bodySmall.copy(fontFamily = style.fontFamily),
      labelLarge = baseTypography.labelLarge.copy(fontFamily = style.fontFamily),
      labelMedium = baseTypography.labelMedium.copy(fontFamily = style.fontFamily),
      labelSmall = baseTypography.labelSmall.copy(fontFamily = style.fontFamily)
    )

  val shapes =
    Shapes(
      extraSmall = RoundedCornerShape(style.borderRadius ?: 8.dp),
      small = RoundedCornerShape(style.borderRadius ?: 8.dp),
      medium = RoundedCornerShape(style.borderRadius ?: 8.dp),
      large = RoundedCornerShape(style.borderRadius ?: 8.dp),
      extraLarge = RoundedCornerShape(style.borderRadius ?: 8.dp)
    )

  val customColors =
    XenditCustomColors(
      colorTextPlaceholder = style.colorTextPlaceholder ?: Color(0xFF9CA3AF),
      colorDisabled = style.colorDisabled ?: Color(0xFFE5E7EB),
      colorBorder = style.colorBorder ?: Color(0xFFE5E7EB),
      qrForegroundColor = style.qrForegroundColor ?: Color(0xFF000000),
      qrBackgroundColor = style.qrBackgroundColor ?: Color(0xFFFFFFFF)
    )

  CompositionLocalProvider(
    LocalXenditAppearance provides style,
    LocalXenditCustomColors provides customColors
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = typography,
      shapes = shapes,
      content = content
    )
  }
}

@Preview(showBackground = true, name = "XenditTheme - Default")
@Composable
private fun XenditThemePreviewDefault() {
  XenditTheme(style = XenditAppearance()) { XenditThemePreviewContent() }
}

@Preview(showBackground = true, name = "XenditTheme - Custom")
@Composable
private fun XenditThemePreviewCustom() {
  XenditTheme(
    style =
      XenditAppearance(
        colorPrimary = Color(0xFF0B57D0),
        colorText = Color(0xFF0F172A),
        colorTextSecondary = Color(0xFF475569),
        colorTextPlaceholder = Color(0xFF94A3B8),
        colorBorder = Color(0xFFE2E8F0),
        colorBackground = Color(0xFFFFFFFF)
      )
  ) {
    XenditThemePreviewContent()
  }
}

@Composable
private fun XenditThemePreviewContent() {
  val typographySamples =
    listOf(
      "displayLarge" to MaterialTheme.typography.displayLarge,
      "displayMedium" to MaterialTheme.typography.displayMedium,
      "displaySmall" to MaterialTheme.typography.displaySmall,
      "headlineLarge" to MaterialTheme.typography.headlineLarge,
      "headlineMedium" to MaterialTheme.typography.headlineMedium,
      "headlineSmall" to MaterialTheme.typography.headlineSmall,
      "titleLarge" to MaterialTheme.typography.titleLarge,
      "titleMedium" to MaterialTheme.typography.titleMedium,
      "titleSmall" to MaterialTheme.typography.titleSmall,
      "bodyLarge" to MaterialTheme.typography.bodyLarge,
      "bodyMedium" to MaterialTheme.typography.bodyMedium,
      "bodySmall" to MaterialTheme.typography.bodySmall,
      "labelLarge" to MaterialTheme.typography.labelLarge,
      "labelMedium" to MaterialTheme.typography.labelMedium,
      "labelSmall" to MaterialTheme.typography.labelSmall
    )

  Column(
    modifier = Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    SectionHeader("Typography")
    typographySamples.forEach {
      TypographySample(it.first, it.second)
    }


    SectionHeader("Colors")
    ColorSamples()

    SectionHeader("Components")

    XenditTextField(
      value = "",
      onValueChange = {},
      label = "Sample Field",
      placeholder = "Placeholder",
      modifier = Modifier.fillMaxWidth()
    )

  }
}

@Composable
private fun SectionHeader(text: String) {
  androidx.compose.material3.Text(
    text = text,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurface
  )
}

@Composable
private fun TypographySample(name: String, style: TextStyle) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    androidx.compose.material3.Text(
      text = name,
      style = style,
      color = MaterialTheme.colorScheme.onSurface
    )
    androidx.compose.material3.Text(
      text =
        "fontSize=${style.fontSize.toSpLabel()}, lineHeight=${style.lineHeight.toSpLabel()}, weight=${style.fontWeight}",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

private fun TextUnit.toSpLabel(): String {
  return if (this == TextUnit.Unspecified) "unspecified" else "${this.value}sp"
}

@Composable
private fun ColorSamples() {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      ColorSwatch("primary", MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
      ColorSwatch("surface", MaterialTheme.colorScheme.surface, modifier = Modifier.weight(1f))
      ColorSwatch("outline", MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      ColorSwatch("onSurface", MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
      ColorSwatch(
        "onSurfaceVar",
        MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f)
      )
      ColorSwatch("error", MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
    }
  }
}

@Composable
private fun ColorSwatch(name: String, color: Color, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(36.dp)
        .background(color, shape = RoundedCornerShape(8.dp))
    )
    Spacer(modifier = Modifier.size(4.dp))
    androidx.compose.material3.Text(
      name,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
