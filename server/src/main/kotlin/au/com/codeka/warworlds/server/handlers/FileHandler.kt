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
    val contentType = when {
      path.endsWith(".css") -> "text/css"
      path.endsWith(".js") -> "text/javascript"
      path.endsWith(".png") -> "image/png"
      path.endsWith(".ico") -> "image/x-icon"
      else -> "text/plain"
    }
    response.contentType = contentType
    response.setHeader("Content-Type", contentType)
    try {
      val ins = FileInputStream(File(basePath + path))
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
