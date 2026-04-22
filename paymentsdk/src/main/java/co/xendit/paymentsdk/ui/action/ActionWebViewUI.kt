package co.xendit.paymentsdk.ui.action

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import co.xendit.paymentsdk.R
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
                  Log.d("AcWeb:Iframe", "Data: $data")
                  Log.d("AcWeb:Iframe", "Received type: $type")

                  if (type == "xendit-iframe-action-complete") {
                    emitOnce()
                  }
                } catch (e: Exception) {
                  Log.e("AcWeb:Error", "Failed to parse JSON: $json")
                }
              }

              @JavascriptInterface
              fun onIframeLoad(url: String) {
                Log.i("AcWeb:IframeLoad", url)
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
                Log.d("AcWeb:Redirect", "URL: $u")
                return false
              }

              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("AcWeb:Started", "URL: $url")
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val currentUrl = view?.url ?: url
                Log.d("AcWeb:Finished", "URL: $currentUrl")
                val u = currentUrl?.let { Uri.parse(it) }
              }

              override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
              ): WebResourceResponse? {
                Log.d("AcWeb:Network", "Iframe requesting: ${request?.url}")
                return super.shouldInterceptRequest(view, request)
              }

              override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
              ) {
                super.onReceivedError(view, request, error)
                Log.e(
                  "AcWeb:Error",
                  "Resource Error: ${request?.url} | Desc: ${error?.description} (Code: ${error?.errorCode})"
                )
              }

              override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
              ) {
                super.onReceivedHttpError(view, request, errorResponse)
                Log.e(
                  "AcWeb:Error",
                  "HTTP Error: ${request?.url} | Status: ${errorResponse?.statusCode}"
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
            Log.i("AcWeb:Load", "Direct URL: $url")
            loadUrl(url)
          }
        }
      },
      modifier = Modifier.fillMaxSize()
    )

    Button(onClick = onClose, modifier = Modifier.align(Alignment.BottomCenter)) {
      Text(stringResource(id = R.string.sessiondialog_close))
    }
  }
}
