package au.com.codeka.warworlds.client

import au.com.codeka.warworlds.common.Log

/**
 * This class works with the Log class to provide a concrete implementation we can use in Android.
 */
object LogImpl {
  fun setup() {
    Log.setImpl(LogImplImpl())
  }

  private val LevelMap = intArrayOf(
      android.util.Log.ERROR,
      android.util.Log.WARN,
      android.util.Log.INFO,
      android.util.Log.DEBUG
  )

  private class LogImplImpl : Log.LogImpl {
    override fun isLoggable(tag: String, level: Int): Boolean {
      return BuildConfig.DEBUG || android.util.Log.isLoggable("wwmmo", LevelMap[level])
    }

    override fun write(tag: String, level: Int, msg: String) {
      android.util.Log.println(LevelMap[level], "wwmmo", "$tag: $msg")
    }
  }
}