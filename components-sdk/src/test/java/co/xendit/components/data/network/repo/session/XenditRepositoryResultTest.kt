package co.xendit.components.data.network.repo.session

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class XenditRepositoryResultTest {

  @Test
  fun `successful retrofit response maps to Success`() {
    val response = Response.success("ok")

    val result = response.toXenditRepositoryResult()

    assertTrue(result is XenditRepositoryResult.Success)
    assertEquals("ok", (result as XenditRepositoryResult.Success).data)
  }

  @Test
  fun `error retrofit response maps to Failure with parsed api error`() {
    val errorBody = """
      {
        "error_code": "NETWORK_ERROR",
        "message": "Network error"
      }
    """.trimIndent().toResponseBody("application/json".toMediaType())
    val response = Response.error<String>(503, errorBody)

    val result = response.toXenditRepositoryResult()

    assertTrue(result is XenditRepositoryResult.Failure)
    result as XenditRepositoryResult.Failure
    assertEquals(503, result.statusCode)
    assertEquals("NETWORK_ERROR", result.errorCode)
    assertEquals("Network error", result.message)
  }
}
