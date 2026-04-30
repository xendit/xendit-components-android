package co.xendit.components.ui.action

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import co.xendit.components.util.XLogger
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun ActionWebViewUI(
  url: String,
  onClose: () -> Unit,
  onChallengeCompleted: () -> Unit,
  iframeCapable: Boolean = true
) {
  Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(
      factory = { ctx ->
        WebView(ctx).apply {
          val completionEmitted = AtomicBoolean(false)
          fun emitOnce() {
            if (completionEmitted.compareAndSet(false, true)) {
              onChallengeCompleted()
              try {
                stopLoading()
                loadUrl("about:blank")
              } catch (_: Exception) {
              }
            }
          }
          settings.javaScriptEnabled = true
          settings.domStorageEnabled = true
          settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
          if (android.os.Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
          } else {
            CookieManager.getInstance().setAcceptCookie(true)
          }
          addJavascriptInterface(
            object {
              @JavascriptInterface
              fun onIframeComplete(json: String) { // Accept the payload here
                try {
                  val data = org.json.JSONObject(json)
                  val type = data.optString("type")
                  XLogger.d("AcWeb:Iframe Data: $data")
                  XLogger.d("AcWeb:Iframe Received type: $type")

                  if (type == "xendit-iframe-action-complete") {
                    emitOnce()
                  }
                } catch (e: Exception) {
                  XLogger.e("AcWeb:Error Failed to parse JSON: $json")
                }
              }

              @JavascriptInterface
              fun onIframeLoad(url: String) {
                XLogger.i("AcWeb:IframeLoad $url")
              }
            },
            "AndroidBridge"
          )
          webViewClient =
            object : WebViewClient() {
              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
              ): Boolean {
                val u = request?.url
                XLogger.d("AcWeb:Redirect URL: $u")
                return false
              }

              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                XLogger.d("AcWeb:Started URL: $url")
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val currentUrl = view?.url ?: url
                XLogger.d("AcWeb:Finished URL: $currentUrl")
                val u = currentUrl?.let { Uri.parse(it) }
              }

              override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
              ): WebResourceResponse? {
                XLogger.d("AcWeb:Network Iframe requesting: ${request?.url}")
                return super.shouldInterceptRequest(view, request)
              }

              override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
              ) {
                super.onReceivedError(view, request, error)
                XLogger.e(
                  "AcWeb:Error Resource Error: ${request?.url} | Desc: ${error?.description} (Code: ${error?.errorCode})"
                )
              }

              override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
              ) {
                super.onReceivedHttpError(view, request, errorResponse)
                XLogger.e(
                  "AcWeb:Error HTTP Error: ${request?.url} | Status: ${errorResponse?.statusCode}"
                )
              }
            }
          val uri = runCatching { Uri.parse(url) }.getOrNull()
          val baseUrl =
            if (uri != null && uri.scheme != null && uri.host != null) "${uri.scheme}://${uri.host}" else "https://api.xendit.co"
          // test
//          loadUrl(url)
          if (iframeCapable) {
            val data = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        html, body { margin: 0; padding: 0; }
                        iframe { border: none; width: 100%; height: 100%; position: fixed; left: 0; top: 0; }
                    </style>
                    <script>
                        window.addEventListener('message', function(e) {
                            try {
                                var payload = e.data;
                                // If it's an object, stringify it so Kotlin receives a clean String
                                var message = (typeof payload === 'object') ? JSON.stringify(payload) : payload;
                                
                                if (window.AndroidBridge) {
                                    window.AndroidBridge.onIframeComplete(message);
                                }
                            } catch (err) {
                                console.error('Android bridge error', err);
                            }
                        }, false);
                        
                        // Function for iframe load (Loading)
                        function handleIframeLoad(iframeElement) {
                            try {
                                if (window.AndroidBridge && window.AndroidBridge.onIframeLoad) {
                                    window.AndroidBridge.onIframeLoad(iframeElement.src);
                                }
                            } catch (err) {
                               console.log('Load Bridge Error');
                            }
                        }
                    </script>
                </head>
                <body>
                    <iframe 
                        src="$url" 
                        onload="handleIframeLoad(this)">
                    </iframe>
                </body>
                </html>
            """.trimIndent()
            loadDataWithBaseURL(baseUrl, data, "text/html", "utf-8", null)
          } else {
            XLogger.i("AcWeb:Load Direct URL: $url")
            loadUrl(url)
          }
        }
      },
      modifier = Modifier.fillMaxSize()
    )
  }
}
