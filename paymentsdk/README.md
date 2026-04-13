# Payment SDK

A standalone Android SDK module built with Jetpack Compose for easy payment card data collection.

## Features

- **Jetpack Compose**: Built with modern UI components and Material3.
- **Bottom Sheet UI**: Non-intrusive modal bottom sheet for card entry.
- **Card Formatting**: Automatic formatting for card numbers (16 digits) and expiry dates (MM/YY).
- **Validation**: Built-in Luhn algorithm check and date validation.
- **Dynamic Input**: Pass initial values (like order ID or amount) from your app to the SDK.

## Integration

## Integration

To allow anyone to use your library without needing your personal credentials, they can use **JitPack**.

### 1. Configure Repository

Add JitPack to your `settings.gradle.kts` (or root `build.gradle.kts`):
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add dependency

Add the following to your `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.github.argaasasta:XenComponentPrivate:v0.0.1-alpha")
}
```
*(Note: Replace `v0.0.1-alpha` with your latest tag)*

### 2. Basic Usage

Launch the SDK from any `ComponentActivity`:

```kotlin
PaymentSDK.show(
    context = this, 
    initialValue = "Order #12345", // Shown in the bottom sheet
    callback = { result ->
        when (result) {
            is PaymentResult.Success -> {
                val card = result.cardData
                // Handle payment with card.cardNumber, card.expiryDate, card.cvc
            }
            is PaymentResult.Cancelled -> {
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
