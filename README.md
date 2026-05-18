# Xendit Components Android

A drop-in payment UI SDK for Android that lets you accept payments through Xendit with minimal integration effort. Present a fully featured payment sheet in just a few lines of code.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/xendit/xendit-components-android)
[![Version](https://img.shields.io/badge/version-1.0.0-blue)](https://github.com/xendit/xendit-components-android/releases)
[![License](https://img.shields.io/badge/license-Apache%202.0-lightgrey)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%2026%2B-green)](https://developer.android.com/)

## Features

- **Pre-built payment sheet** — Full payment UI with channel selection, form validation, and error handling out of the box.
- **3DS & redirect handling** — Built-in WebView for 3D Secure challenges and redirect-based payment flows.
- **Customizable appearance** — Configure colors, fonts, and corner radius to match your brand.
- **Jetpack Compose native** — Built entirely with Material 3 and modern Compose components.
- **Secure by default** — Client-side encryption of sensitive card data using ECDH + AES-GCM.

## Requirements

| Requirement   | Minimum  |
|---------------|----------|
| Android        | API 26 (Android 8.0) |
| Compile SDK    | 37       |
| Kotlin         | 1.9+     |
| Jetpack Compose | 1.5+   |

## Getting Started

### Quick Start (Kotlin)

```kotlin
import co.xendit.components.XenditComponents
import co.xendit.components.data.model.XenditPaymentResult

// 1. Initialize once at app startup (Application class or main Activity)
XenditComponents.initialize(appearance = XenditAppearance())

// 2. Present the payment sheet from a ComponentActivity
XenditComponents.present(
    activity = this,
    componentsSdkKey = "<your_components_sdk_key>"
) { result ->
    when (result) {
        is XenditPaymentResult.Success ->
            println("Payment succeeded: ${result.paymentRequestId}")
        is XenditPaymentResult.Failed ->
            println("Payment failed: ${result.error.message}")
        is XenditPaymentResult.Canceled ->
            println("Session canceled")
        is XenditPaymentResult.Expired ->
            println("Session expired")
        is XenditPaymentResult.Dismissed ->
            println("User dismissed")
    }
}
```

### Quick Start (Java)

```java
import co.xendit.components.XenditComponents;
import co.xendit.components.data.model.XenditPaymentResult;
import co.xendit.components.ui.style.XenditAppearance;

// 1. Initialize once at app startup
XenditComponents.INSTANCE.initialize(new XenditAppearance(), null);

// 2. Present the payment sheet from a ComponentActivity
XenditComponents.INSTANCE.present(
    this,
    "<your_components_sdk_key>",
    null,
    result -> {
        if (result instanceof XenditPaymentResult.Success) {
            XenditPaymentResult.Success success = (XenditPaymentResult.Success) result;
            System.out.println("Paid: " + success.getPaymentRequestId());
        } else if (result instanceof XenditPaymentResult.Failed) {
            XenditPaymentResult.Failed failed = (XenditPaymentResult.Failed) result;
            System.out.println("Error: " + failed.getError().getMessage());
        }
    }
);
```

The `components_sdk_key` is obtained from the [Create Session](https://developers.xendit.co) API response on your backend.

## Appearance Customization

Use `XenditAppearance` to match the payment sheet to your app's brand. All color properties have built-in defaults — pass only the values you want to override.

### Property Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `fontFamily` | `FontFamily?` | `null` | Custom font family for all text. Falls back to the bundled Inter font. |
| `colorPrimary` | `Color` | `#0052FF` | Primary CTA color — pay button, selected states, checkmarks. |
| `colorText` | `Color` | `#1A1C1E` | Main body text color. |
| `colorTextSecondary` | `Color` | `#6B7280` | Subtitle and caption text color. |
| `colorTextPlaceholder` | `Color` | `#9CA3AF` | Placeholder text inside input fields. |
| `colorDisabled` | `Color` | `#E5E7EB` | Background of disabled buttons and controls. |
| `colorDanger` | `Color` | `#BA1A1A` | Error messages and invalid field borders. |
| `colorBorder` | `Color` | `#E6E6E6` | Input field outlines and dividers. |
| `colorBackground` | `Color` | `#FFFFFF` | Sheet and page background. |
| `qrForegroundColor` | `Color` | `#000000` | Tint of the container shown around the QR code image. |
| `qrBackgroundColor` | `Color` | `#FFFFFF` | Background of the container box behind the QR code image. |
| `borderRadius` | `Dp` | `8.dp` | Corner radius for buttons, fields, and cards. |

### Kotlin

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import co.xendit.components.XenditComponents
import co.xendit.components.ui.style.XenditAppearance

val customFont = FontFamily(
    Font(R.font.your_font_regular,   FontWeight.Normal,   FontStyle.Normal),
    Font(R.font.your_font_medium,    FontWeight.Medium,   FontStyle.Normal),
    Font(R.font.your_font_semibold,  FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.your_font_bold,      FontWeight.Bold,     FontStyle.Normal)
)

val appearance = XenditAppearance(
    fontFamily            = customFont,
    colorPrimary          = Color(0xFF0052FF),
    colorText             = Color(0xFF1A1C1E),
    colorTextSecondary    = Color(0xFF6B7280),
    colorTextPlaceholder  = Color(0xFF9CA3AF),
    colorDisabled         = Color(0xFFE5E7EB),
    colorDanger           = Color(0xFFBA1A1A),
    colorBorder           = Color(0xFFE6E6E6),
    colorBackground       = Color(0xFFFFFFFF),
    qrForegroundColor     = Color(0xFF000000),
    qrBackgroundColor     = Color(0xFFFFFFFF),
    borderRadius          = 12.dp
)

XenditComponents.initialize(appearance = appearance)

XenditComponents.present(
    activity = this,
    componentsSdkKey = "<your_components_sdk_key>"
) { result ->
    when (result) {
        is XenditPaymentResult.Success ->
            println("Payment succeeded: ${result.paymentRequestId}")
        is XenditPaymentResult.Failed ->
            println("Payment failed: ${result.error.message}")
        is XenditPaymentResult.Canceled ->
            println("Session canceled")
        is XenditPaymentResult.Expired ->
            println("Session expired")
        is XenditPaymentResult.Dismissed ->
            println("User dismissed")
    }
}
```

### Java

```java
import androidx.compose.ui.graphics.Color;
import androidx.compose.ui.text.font.Font;
import androidx.compose.ui.text.font.FontFamily;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.unit.DpKt;
import co.xendit.components.XenditComponents;
import co.xendit.components.data.model.XenditPaymentResult;
import co.xendit.components.ui.style.XenditAppearance;
import java.util.Arrays;

FontFamily customFont = new FontFamily(Arrays.asList(
    new Font(R.font.your_font_regular,  FontWeight.Companion.getNormal(),   FontStyle.Companion.getNormal()),
    new Font(R.font.your_font_semibold, FontWeight.Companion.getSemiBold(), FontStyle.Companion.getNormal()),
    new Font(R.font.your_font_bold,     FontWeight.Companion.getBold(),     FontStyle.Companion.getNormal())
));

XenditAppearance appearance = new XenditAppearance(
    customFont,
    new Color(0xFF0052FFL),  // colorPrimary
    new Color(0xFF1A1C1EL),  // colorText
    new Color(0xFF6B7280L),  // colorTextSecondary
    new Color(0xFF9CA3AFL),  // colorTextPlaceholder
    new Color(0xFFE5E7EBL),  // colorDisabled
    new Color(0xFFBA1A1AL),  // colorDanger
    new Color(0xFFE6E6E6L),  // colorBorder
    new Color(0xFFFFFFFFL),  // colorBackground
    new Color(0xFF000000L),  // qrForegroundColor
    new Color(0xFFFFFFFFL),  // qrBackgroundColor
    DpKt.getDp(12)           // borderRadius
);

XenditComponents.INSTANCE.initialize(appearance);

XenditComponents.INSTANCE.present(
    this,
    "<your_components_sdk_key>",
    null,
    result -> {
        if (result instanceof XenditPaymentResult.Success) {
            String id = ((XenditPaymentResult.Success) result).getPaymentRequestId();
            System.out.println("Paid: " + id);
        } else if (result instanceof XenditPaymentResult.Failed) {
            String msg = ((XenditPaymentResult.Failed) result).getError().getMessage();
            System.out.println("Error: " + msg);
        }
    }
);
```

> **Tip:** `XenditComponents.initialize()` can be called multiple times. Each call replaces the active appearance for all subsequent `present()` calls, allowing runtime theme switching without restarting the session.

## Dismissing Manually

```kotlin
XenditComponents.dismiss()
```

Calling `dismiss()` triggers the `onPaymentResult` callback with `XenditPaymentResult.Canceled` and removes the payment UI from the activity.

## Installation

### Gradle (Kotlin DSL)

Add Maven Central to your `settings.gradle.kts` if not already present:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Then add the dependency to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("co.xendit:components:1.0.0")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'co.xendit:components:1.0.0'
}
```

## Documentation & Examples

| Resource | Description |
|----------|-------------|
| [API Reference](https://developers.xendit.co) | Full API documentation for session creation and payment flows. |
| [Example App](example-app/) | Sample Jetpack Compose app demonstrating SDK integration. |

## Privacy

See [PRIVACY.md](PRIVACY.md) for a full breakdown of data collected, how it is used, whether it is linked to the end user's identity, and whether it is used for tracking — formatted for [Google Play Data Safety](https://support.google.com/googleplay/android-developer/answer/10787469).

## License

Xendit Components Android is available under the Apache License 2.0. See the [LICENSE](LICENSE) file for more info.
