package au.com.codeka.warworlds.server.html.render

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.world.StarManager
import java.io.File
import java.util.*

/** [RendererHandler] for rendering stars. */
class StarRendererHandler : RendererHandler() {
  private val log = Log("StarRendererHandler")

  public override fun get() {
    val starId = getUrlParameter("star")?.toLong() ?: throw RequestException(404, "Invalid star")
    val width = getUrlParameter("width")?.toInt() ?: throw RequestException(404, "Invalid width")
    val height = getUrlParameter("height")?.toInt() ?: throw RequestException(404, "Invalid height")
    val bucket = getUrlParameter("bucket")
    val factor = BUCKET_FACTORS[bucket] ?: throw RequestException(404, "Invalid bucket")

    val cacheFile = File("data/cache/star/${starId}/${width}x${height}/${bucket}.png")
    if (cacheFile.exists()) {
      serveCachedFile(cacheFile)
      return
    }
    cacheFile.parentFile.mkdirs()

    val star = StarManager.i.getStarOrError(starId)
    val rand = Random(starId)
    val templateFile = getTemplateFile(rand, "star", star.get().classification.toString())
    val startTime = System.nanoTime()
    generateImage(cacheFile, templateFile, null, width, height, factor, rand)
    val endTime = System.nanoTime()
    log.info("%dms to generate image for %s", (endTime - startTime) / 1000000L, request.pathInfo)
    serveCachedFile(cacheFile)
  }

}