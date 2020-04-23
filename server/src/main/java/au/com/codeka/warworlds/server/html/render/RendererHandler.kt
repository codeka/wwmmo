package au.com.codeka.warworlds.server.html.render

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.Vector3
import au.com.codeka.warworlds.planetrender.PlanetRenderer
import au.com.codeka.warworlds.planetrender.Template
import au.com.codeka.warworlds.planetrender.Template.PlanetTemplate
import au.com.codeka.warworlds.planetrender.Template.PlanetsTemplate
import au.com.codeka.warworlds.server.handlers.RequestHandler
import com.google.common.collect.ImmutableMap
import com.google.common.io.ByteStreams
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.ceil

/**
 * Base class for the handlers that render images.
 */
open class RendererHandler : RequestHandler() {
  private val log = Log("RendererHandler")

  protected fun generateImage(
      cacheFile: File, templateFile: File, sunDirection: Vector3?, width: Int, height: Int,
      factor: Float, rand: Random): Boolean {
    var width = width
    var height = height
    width = ceil(width * factor.toDouble()).toInt()
    height = ceil(height * factor.toDouble()).toInt()
    val tmpl = FileInputStream(templateFile).use { ins -> Template.parse(ins) }
    val renderer: PlanetRenderer
    if (tmpl.template is PlanetsTemplate) {
      val planetsTemplate = tmpl.template as PlanetsTemplate
      if (sunDirection != null) {
        for (child in planetsTemplate.parameters) {
          (child as PlanetTemplate).sunLocation = sunDirection
        }
      }
      renderer = PlanetRenderer(planetsTemplate, rand)
    } else if (tmpl.template is PlanetTemplate) {
      val planetTemplate = tmpl.template as PlanetTemplate
      if (sunDirection != null) {
        planetTemplate.sunLocation = sunDirection
      }
      renderer = PlanetRenderer(planetTemplate, rand)
    } else {
      log.warning("Unknown template: %s", tmpl.template.javaClass.simpleName)
      return false
    }
    val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    renderer.render(img)
    try {
      ImageIO.write(img, "png", cacheFile)
    } catch (e: IOException) {
      log.warning("Error writing image.", e)
      return false
    }
    return true
  }

  protected fun serveCachedFile(file: File) {
    response.contentType = "image/png"
    response.setHeader("Cache-Control", "max-age=2592000") // 30 days
    FileInputStream(file).use { ins -> ByteStreams.copy(ins, response.outputStream) }
  }

  /**
   * Gets a [File] that refers to a planet renderer template for rendering the given type
   * (i.e. star vs planet) and classification (black hole, swamp, etc).
   *
   * @param rand A [Random] that we'll use to select from one of multiple possible templates.
   * @param type The type of the object (one of "star" or "planet").
   * @param classification The classification of the object ("blackhole", "swamp", etc).
   * @return A [File] pointing to a template for rendering that object, or null if no template
   * can be found (e.g. invalid type or classifcation, etc).
   */
  protected fun getTemplateFile(rand: Random, type: String, classification: String): File? {
    val parentDirectory = File(String.format("data/renderer/%s/%s",
        type.toLowerCase(), classification.toLowerCase()))
    if (!parentDirectory.exists()) {
      log.warning("Could not load template for %s/%s: %s",
          type, classification, parentDirectory.absolutePath)
      return null
    }
    val files = parentDirectory.listFiles { dir: File?, name: String -> name.endsWith(".xml") }
        ?: return null
    return files[rand.nextInt(files.size)]
  }

  companion object {
    val BUCKET_FACTORS: Map<String?, Float> = ImmutableMap.builder<String?, Float>()
        .put("ldpi", 0.75f)
        .put("mdpi", 1.0f)
        .put("hdpi", 1.5f)
        .put("xhdpi", 2.0f)
        .put("xxhdpi", 3.0f)
        .put("xxxhdpi", 4.0f)
        .build()
  }
}