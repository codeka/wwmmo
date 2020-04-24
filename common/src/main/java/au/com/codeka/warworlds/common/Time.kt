package au.com.codeka.warworlds.common

import java.text.SimpleDateFormat
import java.util.*

/**
 * Some helper properties to do with time.
 */
object Time {
  const val SECOND = 1000L
  const val MINUTE = 60 * SECOND
  const val HOUR = 60 * MINUTE
  const val DAY = 24 * HOUR
  private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

  fun toHours(time: Long): Float {
    return time.toFloat() / HOUR
  }

  fun format(time: Long): String {
    return formatter.format(Date(time))
  }
}