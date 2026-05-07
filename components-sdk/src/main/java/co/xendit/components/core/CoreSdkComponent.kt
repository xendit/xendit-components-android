package co.xendit.components.core

import android.content.Context
import co.xendit.components.BuildConfig
import co.xendit.components.util.XLogger
import co.xendit.components.core.model.GlobalErrorHandler
import co.xendit.components.core.model.GlobalLoadingHandler
import co.xendit.components.core.model.SafeApiCall
import co.xendit.components.core.network.interceptor.BaseUrlInterceptor
import co.xendit.components.core.network.interceptor.ErrorInterceptor
import co.xendit.components.core.network.interceptor.HeaderInterceptor
import co.xendit.components.core.network.provider.HeaderProvider
import co.xendit.components.data.model.FieldType
import co.xendit.components.data.network.remote.session.XenditApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.Strictness
import io.nerdythings.okhttp.modifier.interceptor.OkHttpRequestModifierInterceptor
import io.nerdythings.okhttp.profiler.OkHttpProfilerInterceptor
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

internal object CoreSdkComponent {

  private lateinit var appContext: Context
  @Volatile private var baseUrl: String = "https://checkout-ui-gateway.xendit.co"
  @Volatile private var baseHttpUrl: HttpUrl = baseUrl.toHttpUrl()
  @Volatile private var retrofitInstance: Retrofit? = null
  @Volatile private var apiInstance: XenditApi? = null

  fun init(context: Context) {
    if (!::appContext.isInitialized) {
      appContext = context.applicationContext
    }
  }

  fun setBaseUrl(url: String) {
    if (baseUrl == url) return
    baseUrl = url
    baseHttpUrl = baseUrl.toHttpUrl()
    retrofitInstance = null
    apiInstance = null
  }

  val headerProvider: HeaderProvider by lazy { HeaderProvider() }
  val globalErrorHandler: GlobalErrorHandler by lazy { GlobalErrorHandler(appContext) }
  val globalLoadingHandler: GlobalLoadingHandler by lazy { GlobalLoadingHandler(appContext) }

  val gson: Gson by lazy {
    GsonBuilder()
      .setStrictness(Strictness.LENIENT)
      .registerTypeAdapter(FieldType::class.java, FieldTypeDeserializer())
      .create()
  }

  val okHttpClient: OkHttpClient by lazy {
    val headerInterceptor = HeaderInterceptor(headerProvider)
    val errorInterceptor = ErrorInterceptor(globalErrorHandler)
    val baseUrlInterceptor = BaseUrlInterceptor { baseHttpUrl }

    OkHttpClient.Builder()
      .apply {
        readTimeout(30, TimeUnit.SECONDS)
        connectTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
        addInterceptor(baseUrlInterceptor)
        addInterceptor(headerInterceptor)
        addInterceptor(errorInterceptor)
      }
      .build()
  }

  val retrofit: Retrofit
    get() =
      retrofitInstance
        ?: synchronized(this) {
          retrofitInstance
            ?: Retrofit.Builder()
              .baseUrl(baseUrl)
              .client(okHttpClient)
              .addConverterFactory(GsonConverterFactory.create(gson))
              .addConverterFactory(ScalarsConverterFactory.create())
              .build()
              .also { retrofitInstance = it }
        }

  val xenditApi: XenditApi
    get() =
      apiInstance
        ?: synchronized(this) {
          apiInstance ?: retrofit.create(XenditApi::class.java).also { apiInstance = it }
        }

  val safeApiCall: SafeApiCall by lazy { SafeApiCall(globalLoadingHandler, globalErrorHandler) }
}

internal class FieldTypeDeserializer : JsonDeserializer<FieldType> {
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
  ): FieldType {
    val jsonObject = json.asJsonObject
    val name = jsonObject.get("name")?.asString ?: "text"

    return when (name) {
      "credit_card_number" -> FieldType.CreditCardNumber()
      "credit_card_expiry" -> FieldType.CreditCardExpiry()
      "credit_card_cvn" -> FieldType.CreditCardCvn()
      "phone_number" -> FieldType.PhoneNumber()
      "email" -> FieldType.Email()
      "postal_code" -> FieldType.PostalCode()
      "country" -> FieldType.Country()
      "province" -> FieldType.Province()
      "installment_plan" -> FieldType.InstallmentPlan()
      "dropdown" -> context.deserialize(json, FieldType.Dropdown::class.java)
      else -> context.deserialize(json, FieldType.Text::class.java)
    }
  }
}
