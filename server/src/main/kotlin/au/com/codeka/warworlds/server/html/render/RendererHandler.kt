package au.com.codeka.warworlds.server.html.render

import au.com.codeka.warworlds.common.Vector3
import au.com.codeka.warworlds.planetrender.PlanetRenderer
import au.com.codeka.warworlds.planetrender.Template
import au.com.codeka.warworlds.planetrender.Template.PlanetTemplate
import au.com.codeka.warworlds.planetrender.Template.PlanetsTemplate
import au.com.codeka.warworlds.server.handlers.RequestException
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
  companion object {
    val BUCKET_FACTORS: Map<String, Float> = ImmutableMap.builder<String, Float>()
        .put("ldpi", 0.75f)
        .put("mdpi", 1.0f)
        .put("hdpi", 1.5f)
        .put("xhdpi", 2.0f)
        .put("xxhdpi", 3.0f)
        .put("xxxhdpi", 4.0f)
        .build()
  }

  /**
   * Generates an image for the given template to the given cache file.
   *
   * @throws RequestException if an error occurs generating the image.
   * @throws IOException if there is an error writing the image.
   */
  protected fun generateImage(
      cacheFile: File, templateFile: File, sunDirection: Vector3?, w: Int, h: Int,
      factor: Float, rand: Random) {
    val width = ceil(w * factor.toDouble()).toInt()
    val height = ceil(h * factor.toDouble()).toInt()
    val tmpl = FileInputStream(templateFile).use { ins -> Template.parse(ins) }
    val renderer: PlanetRenderer
    when (tmpl.template) {
      is PlanetsTemplate -> {
        val planetsTemplate = tmpl.template as PlanetsTemplate
        if (sunDirection != null) {
          for (child in planetsTemplate.parameters) {
            (child as PlanetTemplate).sunLocation = sunDirection
          }
        }
        renderer = PlanetRenderer(planetsTemplate, rand)
      }
      is PlanetTemplate -> {
        val planetTemplate = tmpl.template as PlanetTemplate
        if (sunDirection != null) {
          planetTemplate.sunLocation = sunDirection
        }
        renderer = PlanetRenderer(planetTemplate, rand)
      }
      else -> {
        throw RequestException(500, "Unknown template: ${tmpl.template!!.javaClass.simpleName}")
      }
    }
    val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    renderer.render(img)
    ImageIO.write(img, "png", cacheFile)
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
   * @return A [File] pointing to a template for rendering that object
   * @throws RequestException if the template file cannot be found.
   */
  protected fun getTemplateFile(rand: Random, type: String, classification: String): File {
    val parentDirectory = File(String.format("data/renderer/%s/%s",
        type.lowercase(Locale.ENGLISH), classification.lowercase(Locale.ENGLISH)))
    if (!parentDirectory.exists()) {
      throw RequestException(
          500,
          "Could not load template for ${type}/${classification}: ${parentDirectory.absolutePath}")
    }
    val files = parentDirectory.listFiles { _: File?, name: String -> name.endsWith(".xml") }
        ?: throw RequestException(
            500, "Could not find any files ending in '.xml' in $parentDirectory")
    return files[rand.nextInt(files.size)]
  }
}