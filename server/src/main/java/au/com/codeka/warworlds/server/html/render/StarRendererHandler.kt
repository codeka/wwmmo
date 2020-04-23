package au.com.codeka.warworlds.server.html.render

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.world.StarManager
import au.com.codeka.warworlds.server.world.WatchableObject
import java.io.File
import java.util.*

/**
 * [RendererHandler] for rendering stars.
 */
class StarRendererHandler : RendererHandler() {
  @Throws(RequestException::class)
  public override fun get() {
    val starId = getUrlParameter("star")!!.toLong()
    val width = getUrlParameter("width")!!.toInt()
    val height = getUrlParameter("height")!!.toInt()
    val bucket = getUrlParameter("bucket")
    val factor: Float? = BUCKET_FACTORS.get(bucket)
    if (factor == null) {
      log.warning("Invalid bucket: %s", request.pathInfo)
      response.status = 404
      return
    }
    val cacheFile = File(String.format(Locale.ENGLISH,
        "data/cache/star/%d/%dx%d/%s.png", starId, width, height, bucket))
    if (cacheFile.exists()) {
      serveCachedFile(cacheFile)
      return
    } else {
      cacheFile.parentFile.mkdirs()
    }
    val rand = Random(starId)
    val star: WatchableObject<Star>? = StarManager.i.getStar(starId)
    if (star == null) {
      log.warning("Couldn't load star: %s", request.pathInfo)
      response.status = 404
      return
    }
    val templateFile = getTemplateFile(rand, "star", star.get().classification.toString())
    if (templateFile == null) {
      response.status = 500
      return
    }
    val startTime = System.nanoTime()
    if (!generateImage(cacheFile, templateFile, null, width, height, factor, rand)) {
      response.status = 500
      return
    }
    val endTime = System.nanoTime()
    log.info("%dms to generate image for %s",
        (endTime - startTime) / 1000000L, request.pathInfo)
    serveCachedFile(cacheFile)
  }

  companion object {
    private val log = Log("StarRendererHandler")
  }
}