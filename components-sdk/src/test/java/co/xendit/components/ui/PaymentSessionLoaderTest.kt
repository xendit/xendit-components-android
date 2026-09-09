package co.xendit.components.ui

import co.xendit.components.XenditComponentsPaymentType
import co.xendit.components.data.model.BffBusiness
import co.xendit.components.data.model.BffChannel
import co.xendit.components.data.model.BffSession
import co.xendit.components.data.model.BffSessionAllowSavePaymentMethod
import co.xendit.components.data.model.BffSessionType
import co.xendit.components.data.model.PaymentSessionStatus
import co.xendit.components.data.model.SessionResponse
import co.xendit.components.data.network.repo.session.XenditRepository
import co.xendit.components.data.network.repo.session.XenditRepositoryResult
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PaymentSessionLoaderTest {

  private val repository = mockk<XenditRepository>()
  private val loader = PaymentSessionLoader(repository)

  @Test
  fun `load maps repository success into checkout session data`() = kotlinx.coroutines.test.runTest {
    val sessionResponse =
      SessionResponse(
        session = BffSession(
          id = "session-id",
          paymentSessionId = "pay-session-id",
          status = PaymentSessionStatus.ACTIVE,
          sessionType = BffSessionType.PAY,
          allowSavePaymentMethod = BffSessionAllowSavePaymentMethod.OPTIONAL,
          referenceId = "ref-1",
          currency = "IDR",
          country = "ID",
          amount = BigDecimal.TEN,
          items = emptyList()
        ),
        business = BffBusiness(name = "Demo Merchant"),
        paymentChannels = listOf(
          BffChannel(
            brandName = "BCA VA",
            brandLogoUrl = null,
            brandColor = "#0000FF",
            pmType = XenditComponentsPaymentType.VIRTUAL_ACCOUNT,
            uiGroup = "bank_transfer",
            channelCode = "BCA_VA",
            allowPayWithoutSave = true,
            allowSave = false,
            minAmount = null,
            maxAmount = null,
            requiresCustomerDetails = false,
            card = null,
            form = emptyList(),
            instructions = emptyList()
          )
        ),
        channelUiGroups = emptyList(),
        succeededChannel = null,
        digitalWallets = null
      )
    coEvery { repository.getSession("auth-key") } returns XenditRepositoryResult.Success(sessionResponse)

    val result = loader.load("auth-key")

    assertTrue(result is PaymentSessionLoadResult.Success)
    result as PaymentSessionLoadResult.Success
    assertEquals("pay-session-id", result.data.paymentSessionId)
    assertEquals(BffSessionType.PAY, result.data.sessionType)
    assertEquals(1, result.data.channels.size)
    assertEquals(listOf("BCA_VA"), result.data.selectableChannelCodes)
  }
}
