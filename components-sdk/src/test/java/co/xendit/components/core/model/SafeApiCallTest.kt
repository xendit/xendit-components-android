package co.xendit.components.core.model

import co.xendit.components.util.XLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SafeApiCallTest {

  private val loadingHandler = mockk<GlobalLoadingHandler>(relaxed = true)
  private val globalErrorHandler = mockk<GlobalErrorHandler>(relaxed = true)
  private val safeApiCall = SafeApiCall(loadingHandler, globalErrorHandler)

  @Before
  fun setUp() {
    mockkObject(XLogger)
    every { XLogger.d(any()) } returns Unit
    every { XLogger.e(any(), any()) } returns Unit
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `call returns success when apiCall is successful`() = runBlocking {
    val expectedResponse = Response.success("Success")
    val apiCall = mockk<suspend () -> Response<String>>()
    coEvery { apiCall() } returns expectedResponse

    val result = safeApiCall.call(apiCall)

    assertEquals(expectedResponse, result)
    coVerify { loadingHandler.setLoading() }
    coVerify { loadingHandler.stopLoading() }
  }

  @Test
  fun `call returns error response when apiCall throws HttpException`() = runBlocking {
    val errorBody = "{\"message\":\"Error\"}"
    val response = Response.error<String>(400, errorBody.toResponseBody(null))
    val exception = HttpException(response)
    val apiCall = mockk<suspend () -> Response<String>>()
    coEvery { apiCall() } throws exception

    val result = safeApiCall.call(apiCall)

    assertEquals(400, result.code())
    assertEquals(errorBody, result.errorBody()?.string())
    coVerify { loadingHandler.setLoading() }
    coVerify { loadingHandler.stopLoading() }
  }

  @Test
  fun `call returns 503 when apiCall throws IOException`() = runBlocking {
    val apiCall = mockk<suspend () -> Response<String>>()
    coEvery { apiCall() } throws IOException("Network error")

    val result = safeApiCall.call(apiCall)

    assertEquals(503, result.code())
    verify { globalErrorHandler.postError("NETWORK_ERROR", any()) }
    coVerify { loadingHandler.setLoading() }
    coVerify { loadingHandler.stopLoading() }
  }

  @Test
  fun `call returns 500 when apiCall throws unexpected Exception`() = runBlocking {
    val apiCall = mockk<suspend () -> Response<String>>()
    coEvery { apiCall() } throws RuntimeException("Unexpected error")

    val result = safeApiCall.call(apiCall)

    assertEquals(500, result.code())
    verify { globalErrorHandler.postError("UNEXPECTED_ERROR", any()) }
    coVerify { loadingHandler.setLoading() }
    coVerify { loadingHandler.stopLoading() }
  }
}
