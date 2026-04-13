package co.xendit.paymentsdk.data

import co.xendit.paymentsdk.core.CoreSdkComponent.safeApiCall
import co.xendit.paymentsdk.core.CoreSdkComponent.xenditApi
import co.xendit.paymentsdk.data.network.repo.session.XenditRepositoryImpl

object DataSdkComponent {
  val xenditRepository: XenditRepositoryImpl by lazy {
    XenditRepositoryImpl(safeApiCall, xenditApi)
  }
}