package au.com.codeka.warworlds.server

import au.com.codeka.warworlds.common.Log
import org.joda.time.DateTime
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files

/**
 * This class works with the Log class to provide a concrete implementation we can use in the
 * server.
 */
object LogImpl {
  private lateinit var config: Configuration.LoggingConfig

  fun setup(config: Configuration.LoggingConfig) {
    LogImpl.config = config
    Log.setImpl(LogImplImpl())
  }

  val levelMap = arrayOf("ERROR", "WARNING", "INFO", "DEBUG")

  private class LogImplImpl : Log.LogImpl {
    private var lastOpenTime: DateTime? = null
    private var file: File? = null
    private var writer: PrintWriter? = null

    private val maxLevel: Int = levelMap.indexOf(config.maxLevel)

    override fun isLoggable(tag: String, level: Int): Boolean {
      return level <= maxLevel
    }

    override fun write(tag: String, level: Int, msg: String) {
      if (level > maxLevel) {
        return
      }

      val sb = StringBuilder()
      LogFormatter.DATE_TIME_FORMATTER.printTo(sb, DateTime.now())
      sb.append(" ")
      sb.append(levelMap[level])
      sb.append(" ")
      sb.append(tag)
      sb.append(": ")
      sb.append(msg)

      // First write to the console.
      val str = sb.toString()
      println(str)

      // Then to the log file.
      synchronized(LogImpl) {
        ensureOpen()
        with(writer!!) {
          println(str)
          flush()
        }
      }
    }

    private fun ensureOpen() {
      if (isFileTooOld()) {
        close()
      }
      if (file == null) {
        open()
      }
    }

    private fun open() {
      if (file == null) {
        file = File(config.fileName + ".log")
      }
      val f = file ?: return

      if (f.exists()) {
        backup()
      }

      f.parentFile.mkdirs()
      writer = PrintWriter(f, Charsets.UTF_8)
    }

    private fun close() {
      val w = writer ?: return
      w.close()
      writer = null
    }

    private fun backup() {
      val date = lastOpenTime ?: DateTime.now()
      val f = file ?: return

      var n = 1
      var backupFile: File
      do {
        backupFile = File(
            String.format(
                config.fileName + "-%04d-%02d-%02d-%04d.log",
                date.year, date.monthOfYear, date.dayOfMonth, n))
        n++
      } while (backupFile.exists())

      Files.move(f.toPath(), backupFile.toPath())
      cleanup()
    }

    private fun isFileTooOld(): Boolean {
      val time = lastOpenTime ?: return false
      val now = DateTime.now()

      // It's a different day, it's too old.
      return !now.toDate().equals(time.toDate())
    }

    /** When have too many log files, delete some of the older ones. */
    private fun cleanup() {
      val f = file ?: return

      val existingFiles = ArrayList<File>()
      for (existing in f.parentFile.listFiles() ?: arrayOf()) {
        if (existing.isFile && existing.extension == "log") {
          existingFiles.add(existing)
        }
      }

      // Sort them from newest to oldest
      existingFiles.sortBy { file -> file.lastModified() }

      // Once we get above the limit for cumulative file size, start deleting.
      var totalSize = 0L
      for (existingFile in existingFiles) {
        totalSize += existingFile.length()
        if (totalSize > 10L * 1024L * 1024L) {
          existingFile.delete()
        }
      }
    }
  }
}
