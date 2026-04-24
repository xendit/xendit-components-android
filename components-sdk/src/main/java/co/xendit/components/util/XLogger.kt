package co.xendit.components.util

import android.util.Log
import co.xendit.components.BuildConfig

/**
 * Internal logger for Xendit Components SDK.
 * Logs are only printed if BuildConfig.DEBUG is true to prevent leaking information in production.
 */
internal object XLogger {
  private const val TAG = "XenditComponents"

  fun d(message: String) {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, message)
    }
  }

  fun i(message: String) {
    if (BuildConfig.DEBUG) {
      Log.i(TAG, message)
    }
  }

  fun e(message: String, throwable: Throwable? = null) {
    // We usually want to log errors even in production, but keep it clean.
    // Or you can also wrap this in BuildConfig.DEBUG if you want to be completely silent.
    if (BuildConfig.DEBUG) {
      Log.e(TAG, message, throwable)
    }
  }

  fun w(message: String) {
    if (BuildConfig.DEBUG) {
      Log.w(TAG, message)
    }
  }
}
