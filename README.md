# Xendit Components Android SDK

A modern Android SDK built with Jetpack Compose to integrate Xendit's payment components into your Android application.

## Features

- Jetpack Compose UI (Material3)
- Cards payment flow (including 3DS / redirect handling)
- Smart input formatting (card number, expiry, CVV)
- Built-in validation (Luhn, expiry date checks)
- Customizable appearance (colors, typography, corner radius)

## Requirements

- Min SDK: 26
- Compile SDK: 37
- Kotlin: 1.9+
- Jetpack Compose: 1.5+
- Material3 enabled

## Integration

### 1. Configure repository

Add Maven Central in your `settings.gradle(.kts)` (or project repositories):

```kotlin
dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
}
```

### 2. Add dependency

Add the SDK dependency to your `app/build.gradle.kts`:

```kotlin
dependencies {
  implementation("co.xendit:components:1.0.0")
}
```

## Usage

### 1. (Optional) Global initialization

You can set appearance and default payment-method ordering globally (e.g., in `Application` or your main `Activity`):

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.xendit.components.XenditComponents
import co.xendit.components.ui.style.XenditAppearance

val appearance = XenditAppearance(
  colorPrimary = Color(0xFF0052FF),
  colorText = Color(0xFF1A1C1E),
  borderRadius = 12.dp,
)

XenditComponents.initialize(
  appearance = appearance,
  merchantPreferredPaymentMethod = listOf("cards")
)
```

### 2. Present the payment UI

Launch the payment UI from a `ComponentActivity`:

```kotlin
import co.xendit.components.XenditComponents
import co.xendit.components.data.model.XenditPaymentResult

XenditComponents.present(
  activity = this,
  componentsSdkKey = "session-<session_id>-<host_id>-<public_key>-<signature>",
  merchantPreferredPaymentMethod = listOf("cards"),
  onPaymentResult = { result ->
    when (result) {
      is XenditPaymentResult.Success -> {
        val paymentRequestId = result.paymentRequestId
        val channelCode = result.channelCode
      }
      is XenditPaymentResult.Failed -> {
        val error = result.error
      }
      XenditPaymentResult.Canceled -> {
      }
      XenditPaymentResult.Expired -> {
      }
      XenditPaymentResult.Dismissed -> {
      }
    }
  }
)
```

## Components SDK Key format

This SDK expects a single `componentsSdkKey` string with 5 dash-separated segments:

`session-<session_id>-<host_id>-<public_key>-<signature>`

- `session-<session_id>` is treated as the Session Auth Key
- `<host_id>` selects the environment base URL (for example: `pl`, `pd`, `sl`, `sd`)
- `<public_key>` is used for client-side encryption of sensitive fields
- `<signature>` is included in the key format

Generate the Components SDK Key from your backend. Do not embed secret keys in the app.

## Payment methods

Current UI support:

- `cards`

## Customization

`XenditAppearance` supports:

| Property | Default | Description |
|---|---|---|
| `colorPrimary` | `#0052FF` | Primary action color (buttons, icons) |
| `colorText` | `#1A1C1E` | Main text color |
| `colorTextSecondary` | `#6B7280` | Secondary/hint text color |
| `colorBorder` | `#E6E6E6` | Input field border color |
| `borderRadius` | `8.dp` | Corner radius for inputs and buttons |
| `fontFamily` | `null` | Custom font family for all text |

## Example app

This repository includes an example app module:

- `example-app/`

Open the project in Android Studio and run the `example-app` configuration to try the SDK UI.

## Release workflow

The repository includes automated GitHub Actions for releasing:

- Build Demo: triggers on `main` push, generates debug/release APKs
- Versioned Release: create a branch `release/vX.Y.Z` to build version-named APKs and create a GitHub Release
- Publish: publishes the library for consumption

