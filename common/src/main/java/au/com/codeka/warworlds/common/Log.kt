package au.com.codeka.warworlds.common

import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

/**
 * This is a helper class for working with logging. We support both Android Log.x() and Java's
 * built-in logger (for server-side).
 */
class Log {
  private var hook: LogHook? = null
  private var tag: String
  private var prefix: String? = null

  /** Create a [Log] that'll log with the given tag.  */
  constructor(tag: String) {
    this.tag = tag
  }

  /** Create a [Log] that will write to the given [LogHook] instead of the log file.  */
  constructor(hook: LogHook?) {
    tag = "Log"
    this.hook = hook
  }

  fun setPrefix(prefix: String?) {
    this.prefix = prefix
  }

  fun error(fmt: String, vararg args: Any?) {
    write(ERROR, fmt, *args)
  }

  fun warning(fmt: String, vararg args: Any?) {
    write(WARNING, fmt, *args)
  }

  fun info(fmt: String, vararg args: Any?) {
    write(INFO, fmt, *args)
  }

  fun debug(fmt: String, vararg args: Any?) {
    write(DEBUG, fmt, *args)
  }

  private fun write(level: Int, fmt: String, vararg args: Any?) {
    if (hook != null) {
      hook!!.write(formatMsg(fmt, args))
      return
    }
    if (impl == null || !impl!!.isLoggable(tag, level)) {
      return
    }
    impl!!.write(tag, level, formatMsg(fmt, args))
  }

  val isDebugEnabled: Boolean
    get() = impl != null && impl!!.isLoggable(tag, DEBUG)

  /**
   * Formats the given message. If the last argument is an exception, we'll append the exception to
   * the end of the message.
   */
  private fun formatMsg(fmt: String, args: Array<out Any?>): String {
    val sb = StringBuilder()
    if (prefix != null) {
      sb.append(prefix)
      sb.append(" ")
    }
    try {
      Formatter(sb, Locale.ENGLISH).use { formatter -> formatter.format(fmt, *args) }
    } catch (e: Exception) {
      return fmt // ??
    }
    if (args.isNotEmpty() && args[args.size - 1] is Throwable) {
      val throwable = args[args.size - 1] as Throwable
      val writer = StringWriter()
      throwable.printStackTrace(PrintWriter(writer))
      sb.append("\n")
      sb.append(writer.toString())
    }
    return sb.toString()
  }

  /** This interface is implemented by the client/server to provide the actual logging.  */
  interface LogImpl {
    fun isLoggable(tag: String, level: Int): Boolean
    fun write(tag: String, level: Int, msg: String)
  }

  /** You can implement this interface if you want to hook into the log messages.  */
  interface LogHook {
    fun write(msg: String?)
  }

  companion object {
    private const val ERROR = 0
    private const val WARNING = 1
    private const val INFO = 2
    private const val DEBUG = 3
    private var impl: LogImpl? = null

    fun setImpl(impl: LogImpl?) {
      Companion.impl = impl
    }
  }
}