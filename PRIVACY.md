# Xendit Components Android — Privacy Details

This document describes all data the SDK handles. Use it to complete your [Google Play Data Safety](https://support.google.com/googleplay/android-developer/answer/10787469) section.

| Data Type | Is this data used? | How is this data used? | Is this data linked to the end user's identity? | Is this data used for tracking purposes? |
|-----------|-------------------|------------------------|------------------------------------------------|------------------------------------------|
| Payment Info (card number, expiry, CVV) | **Yes.** Collected when the user enters card details in the payment sheet. All data is encrypted on-device before transmission. | App Functionality | **Conditional.** If Xendit associates the payment method with a customer record on your behalf, you may need to disclose this. | **No.** Xendit does not use this data for tracking purposes. |
| Contact Info (billing name, email, phone, address) | **Conditional.** Collected only when your session configuration includes billing address fields. | App Functionality | **Conditional.** If this data is associated with a payment session or customer record, it may be linked to the end user's identity. | **No.** Xendit does not use this data for tracking purposes. |
| App Info (package name) | **Yes.** The host app's package name is sent with each request for host identification and fraud prevention. | App Functionality, Fraud Prevention | **No.** The package name identifies the host application, not the individual end user. | **No.** Xendit does not use this data for tracking purposes. |
| Network Info (IP address) | **Yes.** Transmitted with every HTTP request as an inherent part of network communication. | Fraud Prevention | **No.** Xendit does not link IP addresses to the end user's identity. | **No.** Xendit does not use this data for tracking purposes. |
| Performance Telemetry (session-scoped performance events) | **Yes.** The SDK emits in-session performance and UI lifecycle events (checkout load, channel selection, form inputs, payment attempt begin/end, abandon) to help Xendit detect regressions, measure reliability, and prioritize product work. | Analytics, App Performance & Reliability, Fraud Prevention | **Conditional.** Events are scoped to the `payment_session_id` and `session_auth_id` issued for the checkout session. These IDs may be linked on the Xendit backend to the payment record and — if you attach a customer object to the payment request — to an end-user identity you provided. | **No.** This data is used for reliability, performance monitoring, and fraud prevention. It is not sold or used for third-party advertising, behavioural advertising profiles, or cross-app tracking. |

## Data Handling

**Encryption.** Card number, expiry, and CVV are encrypted on-device using ECDH + AES-256-GCM before any data leaves the device. Xendit servers receive only the ciphertext; the plaintext never travels over the wire.

**No on-device persistence of payment credentials.** Card numbers, CVV, and token values are held only in process memory during the payment session and discarded when the sheet is dismissed.

**No third-party analytics.** The SDK does not embed any advertising or behavioural analytics SDKs. All data is transmitted exclusively to Xendit endpoints.

## Performance Telemetry — What Exactly Is Sent

The SDK sends a small, best-effort payload of in-session performance events to `https://checkout-ui-gateway.xendit.co/v1/sessions/performance` (region-dependent host). This telemetry helps Xendit measure SDK reliability, catch UI regressions quickly, and detect anomalous behaviour that may indicate fraud.

**Top-level envelope fields:**

| Field | Source file | Description | Contains user data? |
|-------|-------------|-------------|---------------------|
| `payment_session_id` | [SessionTelemetry.kt#L53](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetry.kt#L53) | The session identifier returned by the Create Session API. Ties events to the checkout session. | No — this is a technical session ID issued to you by Xendit; not a user email/phone/name. |
| `session_auth_id` | [SessionTelemetry.kt#L54](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetry.kt#L54) | Authorization ID extracted from the `components_sdk_key` used to call `present()`. | No. |
| `events` | [SessionTelemetry.kt#L55](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetry.kt#L55) | Batched list of lifecycle events (up to 25 per batch). | See table below. |

**Per-event fields (inside `events[]`):**

| Field | Source file | Description | Contains PAN / CVV / user PII? |
|-------|-------------|-------------|---------------------------------|
| `stage` | [SessionTelemetry.kt#L41](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetry.kt#L41) | One of: `CHECKOUT_LOADED`, `CHECKOUT_CHANNEL_GROUP`, `CHECKOUT_CHANNEL`, `CHECKOUT_CHANNEL_FORM_INPUT`, `CHECKOUT_ATTEMPT_BEGIN`, `CHECKOUT_ATTEMPT`, `CHECKOUT_ATTEMPT_DISCARD`, `CHECKOUT_ACTION_BEGIN`, `CHECKOUT_ACTION_CLOSE`, `CHECKOUT_DIGITAL_WALLET_BEGIN`, `CHECKOUT_DIGITAL_WALLET_CLOSE`, `CHECKOUT_ACTION_COPY_TEXT`, `CHECKOUT_END`, `CHECKOUT_PENDING`, `CHECKOUT_ABANDON`. See [SessionTelemetryEvents.kt#L5-L21](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetryEvents.kt#L5-L21). | **No.** |
| `event_id` | [SessionTelemetry.kt#L42](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetry.kt#L42) | UUID generated on-device to order and deduplicate the event stream. | **No.** |
| `success` | [SessionTelemetry.kt#L43](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetry.kt#L43) | Boolean indicating whether the step succeeded or encountered an error. | **No.** |
| `timestamp_micros` | [SessionTelemetry.kt#L44](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetry.kt#L44) | Monotonic wall-clock timestamp of the event (µs). | **No.** |
| `parent_event_id` | [SessionTelemetry.kt#L45](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetry.kt#L45) | For tree-relationship between events (e.g., `Channel` → child form-input events). | **No.** |
| `payment_channel` | [SessionTelemetry.kt#L46](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetry.kt#L46) | Xendit channel code (e.g. `BCA`, `BCA_KLIKPAY`, `SHOPEEPAY`, `CREDIT_CARD`). | **No.** |
| `payment_request_id` | [SessionTelemetry.kt#L47](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetry.kt#L47) | Xendit Payment Request ID (set after a submit attempt succeeds). | **No.** |
| `payment_token_id` | [SessionTelemetry.kt#L48](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetry.kt#L48) | Xendit Payment Token ID (set after a card tokenize step). | **No.** |
| `metadata` | [SessionTelemetry.kt#L49](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetry.kt#L49) | Structured, non-PII diagnostic context. Keys used today: `group_name` (payment-method group: `cards`/`ewallet`/`qr_code`/etc.), `field_name` (UI field label such as `card_number`, `expiry`, `cvv`, `phone`, `name` — **the value the user typed is never included**), `validation_error` (validation key), `error_code`, `failure_code`, `status`, `digital_wallet` (`GOOGLE_PAY`). Defined in [SessionTelemetryEvents.kt#L38-L118](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/telemetry/SessionTelemetryEvents.kt#L38-L118). | **No.** `field_name` is a static label, not the input value. Card PAN, CVV, expiry digits, and any user-typed strings are **never** transmitted in telemetry. |

### Data NOT sent in telemetry

**Never included in any telemetry event:**
- Card PAN (full number or masked)
- CVV / CVC
- Card expiry month/year as entered by the user
- Billing name, email, phone, or address text entered by the user
- Any `value` of a form field — only the static `field_name` label is sent when present
- Android Advertising ID / AAID / GAID
- Device location
- User contact data from the merchant app

### Lifecycle & delivery guarantees

- **Best-effort, in-memory only.** Events are buffered in a `ConcurrentLinkedQueue` and flushed on a 5-second timer, when the queue reaches 25 events, or when the app is sent to the background. If the process is killed by the OS or the user before a flush completes, those events are dropped — performance telemetry is explicitly non-transactional.
- **Synchronous flush on lifecycle transitions.** The SDK calls `flush()` from both the Activity `onStop`/`onDestroy` and the `ProcessLifecycleOwner` `onStop` callback in [XenditComponents.kt#L240-L258](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/XenditComponents.kt#L240-L258), mirroring the Web SDK's `visibilitychange → hidden` flush behaviour.
- **Release vs debug.** In release builds, telemetry still fires (it must — we need reliability data from production), but **logcat printing is suppressed** via `BuildConfig.DEBUG` gates in [XLogger.kt](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/util/XLogger.kt#L13-L37). No payload contents are ever logged to logcat in release builds.
- **Memory pressure discard.** On Android `ComponentCallbacks2.TRIM_MEMORY_BACKGROUND` (OS signals the process entered cached state), the queue is fully discarded via `discardAll()` — no data is persisted to disk. See [XenditComponents.kt#L216-L235](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/XenditComponents.kt#L216-L235).

### Debug-only visibility (never ship to Play Store)

In `BuildConfig.DEBUG` builds, the telemetry OkHttp client attaches an `OkHttpProfilerInterceptor` that exposes requests to Android Studio's Profiler (and to `setTelemetryLoggingEnabled(true)` logcat prints). This interceptor is **excluded from release builds** by the `if (BuildConfig.DEBUG)` gate in [CoreSdkComponent.kt#L109-L112](file:///Users/arga/Documents/Xendit/XenditComponents/XenditComponentsAndroid/components-sdk/src/main/java/co/xendit/components/core/CoreSdkComponent.kt#L109-L112). Your merchant's production users will never see the profiler or log payloads.

### Public APIs for merchant debugging

The following public APIs are available on the `XenditComponents` object. They have **no effect on whether data is sent** — they only control log visibility for the merchant's debugging session:

| API | Purpose |
|-----|---------|
| `setTelemetryLoggingEnabled(Boolean)` | Toggles logcat printing of telemetry events and payloads. Defaults to `BuildConfig.DEBUG`. Has no effect on network transmission. |
| `logTelemetryQueueSnapshot(label: String)` | Prints a snapshot of the currently buffered (not-yet-flushed) events to logcat. No-op when `setTelemetryLoggingEnabled(false)`. |

For full details, see the [Xendit Privacy Policy](https://www.xendit.co/en/privacy-policy/).
