package co.xendit.components.data

import co.xendit.components.core.CoreSdkComponent.safeApiCall
import co.xendit.components.core.CoreSdkComponent.xenditApi
import co.xendit.components.data.network.repo.session.XenditRepositoryImpl

internal object DataSdkComponent {
  val xenditRepository: XenditRepositoryImpl by lazy {
    XenditRepositoryImpl(safeApiCall, xenditApi)
  }
}