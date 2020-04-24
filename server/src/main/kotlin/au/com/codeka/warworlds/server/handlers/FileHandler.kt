package au.com.codeka.warworlds.server.handlers

import au.com.codeka.warworlds.common.Log
import com.google.api.client.util.ByteStreams
import java.io.*

/** Simple handler for handling static files (and 'templated' HTML files with no templated data).  */
open class FileHandler(private val basePath: String) : RequestHandler() {
  private val log = Log("AdminGenericHandler")
  fun canHandle(): Boolean {
    val file = File(basePath + path)
    return file.exists()
  }

  @Throws(RequestException::class)
  override fun get() {
    val path = path
    val contentType: String
    contentType = if (path.endsWith(".css")) {
      "text/css"
    } else if (path.endsWith(".js")) {
      "text/javascript"
    } else if (path.endsWith(".png")) {
      "image/png"
    } else if (path.endsWith(".ico")) {
      "image/x-icon"
    } else {
      "text/plain"
    }
    response.contentType = contentType
    response.setHeader("Content-Type", contentType)
    try {
      val ins: InputStream = FileInputStream(File(basePath + path))
      val outs: OutputStream = response.outputStream
      ByteStreams.copy(ins, outs)
      ins.close()
    } catch (e: FileNotFoundException) {
      log.error("Error", e)
      throw RequestException(404, e.message)
    } catch (e: IOException) {
      log.error("Error sending static file!", e)
    }
  }

  @Throws(RequestException::class)
  public override fun post() {
    throw RequestException(405, path)
  }

  private val path: String
    private get() {
      var path = extraOption
      if (path == null) {
        path = ""
      }
      path += getUrlParameter("path")
      return path
    }
}
