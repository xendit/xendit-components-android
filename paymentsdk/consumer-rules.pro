# Keep the public SDK interface
-keep class co.xendit.paymentsdk.XenditComponents {
    public *;
}
-keep class co.xendit.paymentsdk.data.model.XenditPaymentResult { *; }
-keep class co.xendit.paymentsdk.data.model.XenditPaymentResult$* { *; }
-keep class co.xendit.paymentsdk.data.model.XenditError { *; }
-keep class co.xendit.paymentsdk.ui.style.XenditAppearance { *; }

# Keep all data models used for JSON serialization/deserialization
-keep @androidx.annotation.Keep class co.xendit.paymentsdk.data.model.** { *; }

# Keep GSON related classes and annotations
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# Retrofit and OkHttp
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Coil
-keep class coil.** { *; }

# Jetpack Compose
-keep class androidx.compose.** { *; }
