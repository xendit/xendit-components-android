# Xendit Components Android SDK

A modern Android SDK built with Jetpack Compose to integrate Xendit's payment components seamlessly into your Android application.

## Features

- **Jetpack Compose Native**: Built entirely with modern UI components and Material3.
- **Dynamic Payment Methods**: Supports Cards and other payment channels dynamically.
- **Smart Formatting**: Automatic formatting for card numbers, expiry dates, and CVV.
- **Robust Validation**: Built-in Luhn algorithm and date validation.
- **Highly Customizable**: Easily configure colors, fonts, and border radius to match your brand identity.
- **Seamless 3DS Handling**: Built-in WebView and iframe support for handling 3D Secure and other redirects.
- **Production Ready**: Optimized logging (XLogger) and internal-only testing utilities.

## Integration

### 1. Configure Repository

Add Maven Central to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

### 2. Add Dependency

Add the SDK dependency to your `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("co.xendit:components:0.0.1")
}
```

## Usage

### 1. Global Initialization (Optional)

You can customize the SDK's appearance globally before launching the UI. This is usually done in your `Application` class or main `Activity`.

```kotlin
val appearance = XenditAppearance(
    colorPrimary = Color(0xFF0052FF),
    colorText = Color(0xFF1A1C1E),
    borderRadius = 12.dp,
    // fontFamily = yourCustomFontFamily
)

XenditComponents.initialize(
    appearance = appearance,
    merchantPreferredPaymentMethod = listOf("cards", "qr_code") // Default ordering
)
```

### 2. Presenting the Payment UI

Launch the payment interface from any `ComponentActivity`. The SDK handles the entire flow, including 3DS redirects and status polling.

```kotlin
XenditComponents.present(
    activity = this, 
    componentsSdkKey = "session-hostid-publickey-signature", // Your Components SDK Key
    merchantPreferredPaymentMethod = listOf("cards"), // Optional: Override global preferences
    onPaymentResult = { result ->
        when (result) {
            is XenditPaymentResult.Success -> {
                // Payment completed successfully
                val paymentRequestId = result.paymentRequestId
            }
            is XenditPaymentResult.Failed -> {
                // Payment failed
                val error = result.error
                println("Error: ${error.message}")
            }
            is XenditPaymentResult.Canceled -> {
                // User closed the payment UI
            }
        }
    }
)
```

## Customization Options

The `XenditAppearance` class allows you to customize:

| Property | Default | Description |
|---|---|---|
| `colorPrimary` | `#0052FF` | Primary action color (buttons, icons) |
| `colorText` | `#1A1C1E` | Main text color |
| `colorTextSecondary`| `#6B7280` | Secondary/hint text color |
| `colorBorder` | `#E6E6E6` | Input field border color |
| `borderRadius` | `8.dp` | Corner radius for inputs and buttons |
| `fontFamily` | `null` | Custom font family for all text |

## Release Workflow

The SDK includes automated GitHub Actions for releasing:
- **Build Demo**: Triggers on `main` push, generates debug/release APKs.
- **Versioned Release**: Create a branch `release/v1.0.0` to automatically build version-named APKs (e.g., `v1.0.0-release-demo.apk`) and create a GitHub Release.
- **Publish**: Automatically publishes to Maven Central on `release/**` branch push.

## Requirements

- **Min SDK**: 26
- **Compile SDK**: 37
- **Kotlin**: 1.9+
- **Compose**: 1.5+
- **Material3**: Enabled
