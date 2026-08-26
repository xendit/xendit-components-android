package co.xendit.components.telemetry

import com.google.gson.annotations.SerializedName

internal enum class TelemetryStage(val value: String) {
  @SerializedName("CHECKOUT_LOADED") CHECKOUT_LOADED("CHECKOUT_LOADED"),
  @SerializedName("CHECKOUT_CHANNEL_GROUP") CHECKOUT_CHANNEL_GROUP("CHECKOUT_CHANNEL_GROUP"),
  @SerializedName("CHECKOUT_CHANNEL") CHECKOUT_CHANNEL("CHECKOUT_CHANNEL"),
  @SerializedName("CHECKOUT_CHANNEL_FORM_INPUT") CHECKOUT_CHANNEL_FORM_INPUT("CHECKOUT_CHANNEL_FORM_INPUT"),
  @SerializedName("CHECKOUT_ATTEMPT_BEGIN") CHECKOUT_ATTEMPT_BEGIN("CHECKOUT_ATTEMPT_BEGIN"),
  @SerializedName("CHECKOUT_ATTEMPT") CHECKOUT_ATTEMPT("CHECKOUT_ATTEMPT"),
  @SerializedName("CHECKOUT_ATTEMPT_DISCARD") CHECKOUT_ATTEMPT_DISCARD("CHECKOUT_ATTEMPT_DISCARD"),
  @SerializedName("CHECKOUT_ACTION_BEGIN") CHECKOUT_ACTION_BEGIN("CHECKOUT_ACTION_BEGIN"),
  @SerializedName("CHECKOUT_ACTION_CLOSE") CHECKOUT_ACTION_CLOSE("CHECKOUT_ACTION_CLOSE"),
  @SerializedName("CHECKOUT_DIGITAL_WALLET_BEGIN") CHECKOUT_DIGITAL_WALLET_BEGIN("CHECKOUT_DIGITAL_WALLET_BEGIN"),
  @SerializedName("CHECKOUT_DIGITAL_WALLET_CLOSE") CHECKOUT_DIGITAL_WALLET_CLOSE("CHECKOUT_DIGITAL_WALLET_CLOSE"),
  @SerializedName("CHECKOUT_ACTION_COPY_TEXT") CHECKOUT_ACTION_COPY_TEXT("CHECKOUT_ACTION_COPY_TEXT"),
  @SerializedName("CHECKOUT_END") CHECKOUT_END("CHECKOUT_END"),
  @SerializedName("CHECKOUT_PENDING") CHECKOUT_PENDING("CHECKOUT_PENDING"),
  @SerializedName("CHECKOUT_ABANDON") CHECKOUT_ABANDON("CHECKOUT_ABANDON"),
}

internal interface SessionTelemetryEvent {
  val stage: TelemetryStage
  val success: Boolean
  val paymentChannel: String? get() = null
  val paymentRequestId: String? get() = null
  val paymentTokenId: String? get() = null
  val metadata: Map<String, Any>? get() = null
}

internal object TelemetryEvents {
  fun Loaded(success: Boolean) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_LOADED
    override val success = success
  }

  fun ChannelGroup(success: Boolean, groupName: String) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_CHANNEL_GROUP
    override val success = success
    override val metadata = mapOf("group_name" to groupName)
  }

  fun Channel(success: Boolean, paymentChannel: String) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_CHANNEL
    override val success = success
    override val paymentChannel = paymentChannel
  }

  fun ChannelFormInput(success: Boolean, fieldName: String) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_CHANNEL_FORM_INPUT
    override val success = success
    override val metadata = mapOf("field_name" to fieldName)
  }

  fun AttemptBegin(success: Boolean, validationError: String? = null) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_ATTEMPT_BEGIN
    override val success = success
    override val metadata = validationError?.let { mapOf("validation_error" to it) }
  }

  fun Attempt_PR(success: Boolean, paymentRequestId: String) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_ATTEMPT
    override val success = success
    override val paymentRequestId = paymentRequestId
  }

  fun Attempt_PT(success: Boolean, paymentTokenId: String) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_ATTEMPT
    override val success = success
    override val paymentTokenId = paymentTokenId
  }

  fun Attempt_Error(success: Boolean, errorCode: String? = null) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_ATTEMPT
    override val success = success
    override val metadata = errorCode?.let { mapOf("error_code" to it) }
  }

  fun AttemptDiscard(success: Boolean, failureCode: String? = null) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_ATTEMPT_DISCARD
    override val success = success
    override val metadata = failureCode?.let { mapOf("failure_code" to it) }
  }

  fun ActionBegin(success: Boolean) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_ACTION_BEGIN
    override val success = success
  }

  fun ActionClose(success: Boolean) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_ACTION_CLOSE
    override val success = success
  }

  fun DigitalWalletBegin(success: Boolean, digitalWallet: String) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_DIGITAL_WALLET_BEGIN
    override val success = success
    override val metadata = mapOf("digital_wallet" to digitalWallet)
  }

  fun DigitalWalletClose(success: Boolean, errorCode: String? = null) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_DIGITAL_WALLET_CLOSE
    override val success = success
    override val metadata = errorCode?.let { mapOf("error_code" to it) }
  }

  fun ActionCopyText(success: Boolean, fieldName: String) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_ACTION_COPY_TEXT
    override val success = success
    override val metadata = mapOf("field_name" to fieldName)
  }

  fun End(success: Boolean, status: String) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_END
    override val success = success
    override val metadata = mapOf("status" to status)
  }

  fun Pending(success: Boolean) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_PENDING
    override val success = success
  }

  fun Abandon(success: Boolean) = object : SessionTelemetryEvent {
    override val stage = TelemetryStage.CHECKOUT_ABANDON
    override val success = success
  }
}
