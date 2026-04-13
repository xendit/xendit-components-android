package co.xendit.paymentsdk.core

import android.content.Context
import co.xendit.paymentsdk.BuildConfig
import co.xendit.paymentsdk.core.model.GlobalErrorHandler
import co.xendit.paymentsdk.core.model.GlobalLoadingHandler
import co.xendit.paymentsdk.core.model.SafeApiCall
import co.xendit.paymentsdk.core.network.interceptor.ErrorInterceptor
import co.xendit.paymentsdk.core.network.interceptor.HeaderInterceptor
import co.xendit.paymentsdk.core.network.provider.HeaderProvider
import co.xendit.paymentsdk.core.network.provider.SecretProvider
import co.xendit.paymentsdk.data.network.remote.session.XenditApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.nerdythings.okhttp.modifier.interceptor.OkHttpRequestModifierInterceptor
import io.nerdythings.okhttp.profiler.OkHttpProfilerInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

/**
 * Manual Dependency Injection container for the Payment SDK. This object holds all the singletons
 * needed by the SDK.
 */
object CoreSdkComponent {

  private lateinit var appContext: Context

  /**
   * Initialize the SDK component with application context. This should be called by
   * [co.xendit.paymentsdk.XenditComponents.present].
   */
  fun init(context: Context) {
    if (!::appContext.isInitialized) {
      appContext = context.applicationContext
    }
  }

  val secretProvider: SecretProvider by lazy { SecretProvider() }
  val headerProvider: HeaderProvider by lazy { HeaderProvider() }
  val globalErrorHandler: GlobalErrorHandler by lazy { GlobalErrorHandler(appContext) }
  val globalLoadingHandler: GlobalLoadingHandler by lazy { GlobalLoadingHandler(appContext) }

  val gson: Gson by lazy {
    GsonBuilder()
      .setStrictness(Strictness.LENIENT)
      .registerTypeAdapterFactory(
        object : TypeAdapterFactory {
          override fun <T : Any> create(
            gson: Gson,
            type: TypeToken<T>
          ): TypeAdapter<T> {
            val kclass = Reflection.getOrCreateKotlinClass(type.rawType)
            return if (kclass.sealedSubclasses.any()) {
              SealedClassTypeAdapter<T>(kclass as KClass<Any>, gson)
            } else gson.getDelegateAdapter(this, type)
          }
        }
      )
      .create()
  }

  val okHttpClient: OkHttpClient by lazy {
    val headerInterceptor = HeaderInterceptor(secretProvider, headerProvider)
    val errorInterceptor = ErrorInterceptor(globalErrorHandler)

    OkHttpClient.Builder()
      .apply {
        readTimeout(30, TimeUnit.SECONDS)
        connectTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
        addInterceptor(headerInterceptor)
        addInterceptor(errorInterceptor)
        if (BuildConfig.DEBUG) {
          addInterceptor(OkHttpProfilerInterceptor())
          addInterceptor(OkHttpRequestModifierInterceptor(appContext))
        }
      }
      .build()
  }

  val retrofit: Retrofit by lazy {
    val url: String =
      when (BuildConfig.FLAVOR) {
        "qa", "stage" -> "https://checkout-ui-gateway-prod-dev.xendit.co/"
        else -> "https://checkout-ui-gateway-prod-dev.xendit.co/"
      }

    Retrofit.Builder()
      .baseUrl(url)
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create(gson))
      .addConverterFactory(ScalarsConverterFactory.create())
      .build()
  }

  val xenditApi: XenditApi by lazy { retrofit.create(XenditApi::class.java) }

  val safeApiCall: SafeApiCall by lazy { SafeApiCall(globalLoadingHandler) }
}

class SealedClassTypeAdapter<T : Any>(val kclass: KClass<Any>, val gson: Gson) : TypeAdapter<T>() {
  override fun read(jsonReader: JsonReader): T? {
    jsonReader.beginObject() // start reading the object
    val nextName = jsonReader.nextName() // get the name on the object
    val innerClass =
      kclass.sealedSubclasses.firstOrNull { it.simpleName!!.contains(nextName) }
        ?: throw Exception(
          "$nextName is not found to be a data class of the sealed class ${kclass.qualifiedName}"
        )
    val x = gson.fromJson<T>(jsonReader, innerClass.javaObjectType)
    jsonReader.endObject()
    // if there a static object, actually return that back to ensure equality and such!
    return (innerClass.objectInstance as? T) ?: x
  }

  override fun write(out: JsonWriter, value: T) {
    val jsonString = gson.toJson(value)
    out.beginObject()
    out.name(value.javaClass.canonicalName?.split(".")?.last() ?: "unknown").jsonValue(jsonString)
    out.endObject()
  }
}
