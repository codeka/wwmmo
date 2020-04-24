package au.com.codeka.warworlds.server

import au.com.codeka.warworlds.common.Log
import org.joda.time.DateTime
import java.util.logging.Level
import java.util.logging.Logger

/**
 * This class works with the Log class to provide a concrete implementation we can use in the
 * server.
 */
object LogImpl {
  private val log = Logger.getLogger("wwmmo")

  fun setup() {
    Log.setImpl(LogImplImpl())
  }

  private val LevelMap = arrayOf(
      Level.SEVERE,
      Level.WARNING,
      Level.INFO,
      Level.FINE
  )

  private class LogImplImpl : Log.LogImpl {
    override fun isLoggable(tag: String, level: Int): Boolean {
      return log.isLoggable(LevelMap[level])
    }

    override fun write(tag: String, level: Int, msg: String) {
      // TODO: if debug
      val sb = StringBuilder()
      LogFormatter.DATE_TIME_FORMATTER.printTo(sb, DateTime.now())
      sb.append(" ")
      sb.append(LevelMap[level])
      sb.append(" ")
      sb.append(tag)
      sb.append(": ")
      sb.append(msg)
      println(sb.toString())
      log.log(LevelMap[level], "$tag: $msg")
    }
  }
}
