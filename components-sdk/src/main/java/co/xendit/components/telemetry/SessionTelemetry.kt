package co.xendit.components.telemetry

import androidx.annotation.VisibleForTesting
import co.xendit.components.BuildConfig
import co.xendit.components.util.XLogger
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

private const val TELEMETRY_FLUSH_INTERVAL = 5000
private const val TELEMETRY_BATCH_SIZE_LIMIT = 25
private const val TELEMETRY_PATH = "/v1/sessions/performance"

internal data class SessionTelemetryScope(
  val id: String?,
  val fromEvent: String,
  val parentScope: SessionTelemetryScope?,
  val inheritedProperties: InheritedProps
) {
  data class InheritedProps(
    val parentEventId: String? = null,
    val paymentChannel: String? = null,
    val paymentRequestId: String? = null,
    val paymentTokenId: String? = null,
  )
}

internal data class SessionTelemetryEventWithExtras(
  val stage: String,
  val event_id: String,
  val success: Boolean,
  val timestamp_micros: String,
  val parent_event_id: String? = null,
  val payment_channel: String? = null,
  val payment_request_id: String? = null,
  val payment_token_id: String? = null,
  val metadata: Map<String, Any>? = null,
)

private data class PerformancePayload(
  val payment_session_id: String,
  val session_auth_id: String,
  val events: List<SessionTelemetryEventWithExtras>,
)

internal class SessionTelemetry(
  private val okHttpClient: OkHttpClient,
  private val gson: Gson,
  logTelemetryEvents: Boolean = BuildConfig.DEBUG,
) {
  @Volatile
  var expectingRedirectAway: Boolean = false

  @Volatile
  var logTelemetryEvents: Boolean = logTelemetryEvents
    set(value) {
      field = value
      if (value) XLogger.d("[telemetry] logging enabled")
    }

  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val queue = ConcurrentLinkedQueue<SessionTelemetryEventWithExtras>()

  private val rootScope = SessionTelemetryScope(
    id = null,
    fromEvent = "ROOT",
    parentScope = null,
    inheritedProperties = SessionTelemetryScope.InheritedProps()
  )

  private val currentScope = AtomicReference(rootScope)
  private val flushTimerJob = AtomicReference<Job?>(null)

  @Volatile private var telemetryHost: String? = null
  @Volatile private var paymentSessionId: String? = null
  @Volatile private var sessionAuthId: String? = null

  fun bindSession(host: String?, sessionId: String?, authId: String?) {
    if (!host.isNullOrBlank()) {
      this.telemetryHost = host
    }
    if (!sessionId.isNullOrBlank()) {
      this.paymentSessionId = sessionId
    }
    if (!authId.isNullOrBlank()) {
      this.sessionAuthId = authId
    }
    if (logTelemetryEvents) {
      XLogger.d(
        "[telemetry] bindSession",
        """
          host(stored):      ${this.telemetryHost}
          host(passed):      $host
          sessionId(stored): ${this.paymentSessionId}
          sessionId(passed): ${if (sessionId.isNullOrBlank()) "unchanged" else sessionId}
          authId(stored):    ${this.sessionAuthId}
          authId(passed):    ${if (authId.isNullOrBlank()) "unchanged" else "•" + authId.takeLast(4)}
        """.trimIndent()
      )
    }
  }

  fun appendAndPushScope(event: SessionTelemetryEvent): SessionTelemetryScope {
    val id = append(event)
    val prev = currentScope.get()
    val newScope = SessionTelemetryScope(
      id = id,
      fromEvent = event.stage.value,
      parentScope = prev,
      inheritedProperties = prev.inheritedProperties.copy(
        parentEventId = id,
        paymentChannel = event.paymentChannel ?: prev.inheritedProperties.paymentChannel,
        paymentRequestId = event.paymentRequestId ?: prev.inheritedProperties.paymentRequestId,
        paymentTokenId = event.paymentTokenId ?: prev.inheritedProperties.paymentTokenId,
      )
    )
    currentScope.set(newScope)
    if (logTelemetryEvents) {
      XLogger.d(
        "[telemetry] PUSH SCOPE",
        """
          fromEvent:  ${newScope.fromEvent}
          scopeId:    ${newScope.id}
          inherit →
            parent_event_id:  ${newScope.inheritedProperties.parentEventId}
            payment_channel:  ${newScope.inheritedProperties.paymentChannel}
            payment_request:  ${newScope.inheritedProperties.paymentRequestId}
            payment_token:    ${newScope.inheritedProperties.paymentTokenId}
        """.trimIndent()
      )
    }
    return newScope
  }

  fun append(event: SessionTelemetryEvent): String {
    val scope = currentScope.get()
    val eventId = UUID.randomUUID().toString()
    val stamped = SessionTelemetryEventWithExtras(
      stage = event.stage.value,
      success = event.success,
      event_id = eventId,
      timestamp_micros = "${System.currentTimeMillis() * 1000}",
      parent_event_id = scope.inheritedProperties.parentEventId,
      payment_channel = event.paymentChannel ?: scope.inheritedProperties.paymentChannel,
      payment_request_id = event.paymentRequestId ?: scope.inheritedProperties.paymentRequestId,
      payment_token_id = event.paymentTokenId ?: scope.inheritedProperties.paymentTokenId,
      metadata = event.metadata,
    )
    if (logTelemetryEvents) {
      XLogger.d(
        "[telemetry] APPEND stage=${stamped.stage}",
        """
          event_id:          ${stamped.event_id}
          parent_event_id:   ${stamped.parent_event_id}
          payment_channel:   ${stamped.payment_channel}
          payment_request:   ${stamped.payment_request_id}
          payment_token:     ${stamped.payment_token_id}
          metadata:          ${stamped.metadata}
          scope.fromEvent:   ${scope.fromEvent}
          scope.id:          ${scope.id}
          pretty-json:       ${gson.prettyPrint(stamped)}
        """.trimIndent()
      )
    }
    queue.offer(stamped)
    scheduleFlush()
    if (queue.size >= TELEMETRY_BATCH_SIZE_LIMIT) flush()
    return eventId
  }

  private fun scheduleFlush() {
    if (flushTimerJob.get()?.isActive == true) return
    flushTimerJob.set(applicationScope.launch {
      delay(TELEMETRY_FLUSH_INTERVAL.milliseconds)
      flush()
    })
  }

  fun flush() {
    val host = telemetryHost
    val sessionId = paymentSessionId
    val authId = sessionAuthId
    val current = drainQueue()
    flushTimerJob.getAndSet(null)?.cancel()

    if (!host.isNullOrBlank() && !sessionId.isNullOrBlank() && !authId.isNullOrBlank() && current.isNotEmpty()) {
      applicationScope.launch {
        runCatching {
          val payload = PerformancePayload(
            payment_session_id = sessionId,
            session_auth_id = authId,
            events = current,
          )
          val jsonBody = gson.toJson(payload)

          if (logTelemetryEvents) {
            XLogger.d(
              "[telemetry] FLUSH POST → ${host}$TELEMETRY_PATH",
              """
                events count:        ${current.size}
                payment_session_id:  $sessionId
                session_auth_id:     $authId
                body bytes:          ${jsonBody.toByteArray(Charsets.UTF_8).size}
                BODY (pretty):
                ${gson.prettyPrint(payload)}
              """.trimIndent()
            )
          }

          val body = jsonBody.toRequestBody("text/plain;charset=UTF-8".toMediaType())
          val request = Request.Builder()
            .url("$host$TELEMETRY_PATH")
            .post(body)
            .build()

          okHttpClient.newCall(request).execute().use { resp ->
            if (logTelemetryEvents) {
              val respBody = runCatching { resp.body?.string() ?: "<empty>" }.getOrDefault("<unreadable>")
              XLogger.d(
                "[telemetry] FLUSH RESPONSE ${resp.code}",
                """
                  success:         ${resp.isSuccessful}
                  headers:         ${resp.headers}
                  response body:   $respBody
                """.trimIndent()
              )
            }
          }
        }.onFailure { t ->
          XLogger.e("[telemetry] FLUSH FAILED", t)
        }
      }
    } else {
      if (logTelemetryEvents) {
        XLogger.d(
          "[telemetry] FLUSH SKIPPED",
          "host=$host sessionId=$sessionId authIdSet=${!authId.isNullOrBlank()} events=${current.size}"
        )
      }
    }
  }

  fun discardAll() {
    queue.clear()
    flushTimerJob.getAndSet(null)?.cancel()
    currentScope.set(rootScope)
    telemetryHost = null
    paymentSessionId = null
    sessionAuthId = null
    expectingRedirectAway = false
  }

  fun logQueueSnapshot(label: String = "SNAPSHOT") {
    if (!logTelemetryEvents) return
    val eventsSnapshot = queue.toList()
    XLogger.d(
      "[telemetry] QUEUE $label (count=${eventsSnapshot.size})",
      buildString {
        eventsSnapshot.forEachIndexed { i, e ->
          appendLine("#$i: ${gson.prettyPrint(e)}")
        }
      }
    )
  }

  fun popScope(scope: SessionTelemetryScope?) {
    scope ?: return
    val parent = scope.parentScope ?: return
    var walker = currentScope.get()
    while (true) {
      if (walker === scope) break
      walker = walker.parentScope ?: return
    }
    if (logTelemetryEvents) XLogger.d("[telemetry] POP SCOPE fromEvent=${scope.fromEvent}")
    currentScope.set(parent)
  }

  @VisibleForTesting
  internal fun drainQueue(): List<SessionTelemetryEventWithExtras> {
    val out = mutableListOf<SessionTelemetryEventWithExtras>()
    while (queue.isNotEmpty()) {
      queue.poll()?.let { out.add(it) }
    }
    return out
  }

  @VisibleForTesting
  internal fun peekNextEventOrNull(): SessionTelemetryEventWithExtras? = queue.peek()

  @VisibleForTesting
  internal fun testQueueSize(): Int = queue.size

}

private fun Gson.prettyPrint(any: Any): String =
  runCatching { this.newBuilder().setPrettyPrinting().create().toJson(any) }
    .getOrDefault(any.toString())

private fun XLogger.d(tag: String, extra: String) {
  if (BuildConfig.DEBUG) {
    d("$tag\n$extra")
  }
}
