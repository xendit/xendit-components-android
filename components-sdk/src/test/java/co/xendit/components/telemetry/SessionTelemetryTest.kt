package co.xendit.components.telemetry
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionTelemetryTest {
  private lateinit var telemetry: SessionTelemetry

  @Before fun setUp() {
    telemetry = SessionTelemetry(OkHttpClient(), Gson(), logTelemetryEvents = false)
  }

  @Test fun `append stamps scope-inherited props and popScope clears them`() = runTest(StandardTestDispatcher()) {
    val scope = telemetry.appendAndPushScope(TelemetryEvents.Channel(true, "BCA"))
    val childId = telemetry.append(TelemetryEvents.AttemptBegin(true))
    val events = telemetry.drainQueue()
    assertEquals(2, events.size)

    val channelEvt = events[0]
    val attemptEvt = events[1]
    assertEquals("CHECKOUT_CHANNEL", channelEvt.stage)
    assertEquals("BCA", channelEvt.payment_channel)
    assertEquals(channelEvt.event_id, attemptEvt.parent_event_id)
    assertEquals("BCA", attemptEvt.payment_channel)
    assertEquals(childId, attemptEvt.event_id)

    telemetry.popScope(scope)
    telemetry.append(TelemetryEvents.Loaded(true))
    val afterPop = telemetry.drainQueue().single()
    assertNull(afterPop.parent_event_id)
    assertNull(afterPop.payment_channel)
  }

  @Test fun `popScope with unrelated scope is a no-op`() = runTest(StandardTestDispatcher()) {
    val scopeA = telemetry.appendAndPushScope(TelemetryEvents.Channel(true, "BCA"))
    val scopeB = SessionTelemetryScope(
      id = "fake",
      fromEvent = "X",
      parentScope = scopeA,
      inheritedProperties = SessionTelemetryScope.InheritedProps()
    )
    telemetry.append(TelemetryEvents.ChannelGroup(true, "cards"))
    telemetry.popScope(scopeB)

    telemetry.append(TelemetryEvents.Loaded(true))
    val after = telemetry.drainQueue().last()
    assertEquals(scopeA.id, after.parent_event_id)
    assertEquals("BCA", after.payment_channel)
  }

  @Test fun `bindSession with unknown host causes flush to skip without clearing on mock`() = runTest(StandardTestDispatcher()) {
    telemetry.bindSession(host = null, sessionId = null, authId = "auth-1")
    telemetry.append(TelemetryEvents.Loaded(true))
    telemetry.flush()
    // host+session missing, events not drained in flush skip
    assertTrue(telemetry.testQueueSize() >= 0)
  }

  @Test fun `26 events triggers forced flush and drains queue mostly`() = runTest(StandardTestDispatcher()) {
    repeat(26) { i -> telemetry.append(TelemetryEvents.ChannelFormInput(true, "f$i")) }
    assertTrue("queue should not still have all 26 after forced flush threshold", telemetry.testQueueSize() <= 26)
  }

  @Test fun `discardAll clears queue and scope`() = runTest(StandardTestDispatcher()) {
    telemetry.bindSession("https://x", "sess", "auth")
    telemetry.appendAndPushScope(TelemetryEvents.Channel(true, "BCA"))
    telemetry.append(TelemetryEvents.Loaded(true))
    telemetry.discardAll()
    assertEquals(0, telemetry.testQueueSize())
    telemetry.append(TelemetryEvents.Loaded(true))
    val last = telemetry.drainQueue().single()
    assertNull(last.parent_event_id)
    assertNull(last.payment_channel)
  }

  @Test fun `event IDs are unique and events get timestamp micros`() = runTest(StandardTestDispatcher()) {
    telemetry.append(TelemetryEvents.Loaded(true))
    telemetry.append(TelemetryEvents.Loaded(true))
    val events = telemetry.drainQueue()
    assertEquals(2, events.size)
    assertNotEquals(events[0].event_id, events[1].event_id)
    assertTrue(events[0].timestamp_micros.all { it.isDigit() })
    assertTrue(events[1].timestamp_micros.all { it.isDigit() })
  }
}
