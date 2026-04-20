# Payment SDK

A standalone Android SDK module built with Jetpack Compose for easy payment processing using Xendit Components.

## Features

- **Jetpack Compose**: Built with modern UI components and Material3.
- **Dynamic Payment Methods**: Seamlessly render Cards, QR Codes, and other payment channels dynamically.
- **Card Formatting**: Automatic formatting for card numbers and expiry dates.
- **Validation**: Built-in Luhn algorithm check and date validation.
- **Customizable UI**: Configure styling to match your brand's look and feel.
- **Action Handling**: Handles web-based authentications (like 3DS) via WebView or native components seamlessly.

## Integration

To integrate the SDK into your project, you can use **Maven Central** or **JitPack**.

### 1. Configure Repository

Add Maven Central (or JitPack) to your `settings.gradle.kts` (or root `build.gradle.kts`):
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // If using JitPack:
        // maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add dependency

Add the following to your `app/build.gradle.kts`:
```kotlin
dependencies {
    // Replace with the latest published version
    implementation("com.xendit:paymentsdk:0.0.1") 
    // Or if using JitPack:
    // implementation("com.github.argaasasta:XenComponentPrivate:v0.0.1-alpha")
}
```

## Basic Usage

### 1. Initialization (Optional)

You can optionally initialize the SDK with custom appearance settings before presenting the payment UI:

```kotlin
val customStyle = XenditAppearance(
    // Customize your font, colors, border radius, etc.
    // fontFamily = yourCustomFontFamily,
    // colorPrimary = Color(0xFF21DCCB),
)

XenditComponents.initialize(
    appearance = customStyle,
    merchantPreferredPaymentMethod = listOf("cards", "qr_code") // Optional ordering
)
```

### 2. Presenting the Payment UI

Launch the SDK from any `ComponentActivity` using a Session SDK Key:

```kotlin
XenditComponents.present(
    activity = this, 
    componentsSdkKey = "session-123-prod-PK123-SIG123", // Your Components SDK Key
    merchantPreferredPaymentMethod = listOf("cards", "qr_code"), // Optional: Override preferences for this session
    onPaymentResult = { result ->
        when (result) {
            is XenditPaymentResult.Success -> {
                // Payment was successfully completed
                val paymentMethodId = result.paymentMethodId
            }
            is XenditPaymentResult.Failed -> {
                // Payment failed or encountered an error
                val error = result.error
            }
            is XenditPaymentResult.Canceled -> {
                // User dismissed the payment form
            }
        }
    }
)
```

## Requirements

- Android SDK 26+
- Jetpack Compose support
- Material3

## Building AAR

To build the SDK as a standalone AAR for distribution:

```bash
./gradlew :paymentsdk:assembleRelease
```

The AAR will be generated at:
`paymentsdk/build/outputs/aar/paymentsdk-release.aar`

