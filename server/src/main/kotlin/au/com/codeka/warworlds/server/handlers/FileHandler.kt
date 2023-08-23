package au.com.codeka.warworlds.server.handlers

import au.com.codeka.warworlds.common.Log
import com.google.api.client.util.ByteStreams
import java.io.*

/**
 * Simple handler for handling static files (and 'templated' HTML files with no templated data).
 */
open class FileHandler(private val basePath: String) : RequestHandler() {
  private val log = Log("AdminGenericHandler")

  fun canHandle(): Boolean {
    val file = File(basePath + path)
    return file.exists()
  }

  override fun get() {
    var file = File(basePath + path)
    if (file.isDirectory) {
      file = File(file, "index.html")
    }

    val contentType = when (file.extension) {
      "css" -> "text/css"
      "js" -> "text/javascript"
      "png" -> "image/png"
      "ico" -> "image/x-icon"
      "html" -> "text/html"
      else -> "text/plain"
    }
    response.contentType = contentType
    response.setHeader("Content-Type", contentType)
    try {
      val ins = FileInputStream(file)
      val outs = response.outputStream
      ByteStreams.copy(ins, outs)
      ins.close()
    } catch (e: FileNotFoundException) {
      log.error("Error", e)
      throw RequestException(404, e.message)
    }
  }

  public override fun post() {
    throw RequestException(405, path)
  }

  private val path: String
    get() {
      var path = extraOption
      if (path == null) {
        path = ""
      }
      path += getUrlParameter("path")
      return path
    }
}
