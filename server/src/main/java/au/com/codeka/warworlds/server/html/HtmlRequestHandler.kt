package au.com.codeka.warworlds.server.html

import au.com.codeka.carrot.CarrotEngine
import au.com.codeka.carrot.CarrotException
import au.com.codeka.carrot.Configuration
import au.com.codeka.carrot.bindings.MapBindings
import au.com.codeka.carrot.resource.FileResourceLocator
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.handlers.RequestHandler
import java.io.File
import java.io.IOException

/**
 * Handler for requests out of the html directory.
 */
open class HtmlRequestHandler : RequestHandler() {
  @Throws(RequestException::class)
  protected fun render(
      tmplName: String?,
      data: Map<String?, Any?>?) {
    response.contentType = "text/html"
    response.setHeader("Content-Type", "text/html; charset=utf-8")
    try {
      response.writer.write(CARROT.process(tmplName, MapBindings(data)))
    } catch (e: CarrotException) {
      throw RequestException(e)
    } catch (e: IOException) {
      throw RequestException(e)
    }
  }

  companion object {
    private val CARROT = CarrotEngine(Configuration.Builder()
        .setResourceLocator(
            FileResourceLocator.Builder(File("data/html/tmpl").absolutePath))
        .build())
  }
}