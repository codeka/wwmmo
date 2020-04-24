package au.com.codeka.warworlds.server.html.render

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.Vector3
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.server.world.StarManager
import au.com.codeka.warworlds.server.world.WatchableObject
import java.io.File
import java.util.*

/**
 * [RendererHandler] for rendering planets.
 */
class PlanetRendererHandler : RendererHandler() {
  private val log = Log("PlanetRendererHandler")

  override fun get() {
    val starId = getUrlParameter("star")!!.toLong()
    val planetIndex = getUrlParameter("planet")!!.toInt()
    val width = getUrlParameter("width")!!.toInt()
    val height = getUrlParameter("height")!!.toInt()
    val bucket = getUrlParameter("bucket")
    val factor: Float? = BUCKET_FACTORS[bucket]
    if (factor == null) {
      log.warning("Invalid bucket: %s", request.pathInfo)
      response.status = 404
      return
    }

    val star: WatchableObject<Star>? = StarManager.i.getStar(starId)
    if (star == null) {
      log.warning("No star!")
      response.status = 404
      return
    }

    if (planetIndex >= star.get().planets.size || planetIndex < 0) {
      log.warning("PlanetIndex is out of bounds.")
      response.status = 404
      return
    }

    val planet = star.get().planets[planetIndex]
    val cacheFile = File(String.format(Locale.ENGLISH,
        "data/cache/planet/%d/%d/%dx%d/%s.png", starId, planetIndex, width, height, bucket))
    if (cacheFile.exists()) {
      serveCachedFile(cacheFile)
      return
    } else {
      cacheFile.parentFile.mkdirs()
    }

    val rand = Random(starId + planetIndex)
    val templateFile = getTemplateFile(rand, "planet", planet.planet_type.toString())
    val startTime = System.nanoTime()
    val sunDirection = getSunDirection(star.get(), planetIndex)
    generateImage(cacheFile, templateFile, sunDirection, width, height, factor, rand)
    val endTime = System.nanoTime()
    log.info("%dms to generate image for %s", (endTime - startTime) / 1000000L, request.pathInfo)
    serveCachedFile(cacheFile)
  }

  private fun getSunDirection(star: Star?, planetIndex: Int): Vector3 {
    val numPlanets = star!!.planets.size
    var angle = 0.5f / (numPlanets + 1)
    angle = (angle * planetIndex * Math.PI + angle * Math.PI).toFloat()
    val sunDirection = Vector3(0.0, 1.0, -1.0)
    sunDirection.rotateZ(angle.toDouble())
    sunDirection.scale(200.0)
    return sunDirection
  }
}