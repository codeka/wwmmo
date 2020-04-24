package au.com.codeka.warworlds.client.util

import android.os.Build
import au.com.codeka.warworlds.client.BuildConfig
import java.util.*

/**
 * Some helper functions for display version information.
 */
object Version {
  @JvmStatic
  fun string(): String {
    return String.format(Locale.US, "%s.%d%s",
        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, if (BuildConfig.DEBUG) "-dbg" else "")
  }

  val isEmulator: Boolean
    get() = Build.FINGERPRINT.contains("generic")
}