package co.xendit.components.ui

import co.xendit.components.telemetry.SessionTelemetry
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PaymentTelemetryCoordinatorTest {

  private val telemetry = mockk<SessionTelemetry>(relaxed = true)
  private val coordinator = PaymentTelemetryCoordinator(telemetry)

  @Test
  fun `digital wallet loaded emits only once`() {
    coordinator.onDigitalWalletLoaded()
    coordinator.onDigitalWalletLoaded()

    verify(exactly = 1) { telemetry.append(any()) }
  }

  @Test
  fun `form field completed emits once per field key`() {
    coordinator.onFormFieldCompleted("card_number")
    coordinator.onFormFieldCompleted("card_number")
    coordinator.onFormFieldCompleted("cvn")

    verify(exactly = 2) { telemetry.append(any()) }
  }
}
