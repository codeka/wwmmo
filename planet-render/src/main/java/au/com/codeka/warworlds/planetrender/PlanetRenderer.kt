package au.com.codeka.warworlds.planetrender

import au.com.codeka.warworlds.common.Colour
import au.com.codeka.warworlds.common.Colour.Companion.blend
import au.com.codeka.warworlds.common.Image
import au.com.codeka.warworlds.planetrender.Template.PlanetTemplate
import au.com.codeka.warworlds.planetrender.Template.PlanetsTemplate
import java.awt.image.BufferedImage
import java.util.*

/**
 * This is actually a very simple ray-tracing engine. The simplicity comes from the fact that
 * we assume there's only one object in the scene (the planet) and one light source (the sun).
 */
class PlanetRenderer {
  private val singlePlanetGenerators = ArrayList<SinglePlanetGenerator>()

  constructor(tmpl: PlanetTemplate, rand: Random) {
    singlePlanetGenerators.add(SinglePlanetGenerator(tmpl, rand))
  }

  constructor(tmpl: PlanetsTemplate, rand: Random) {
    for (planetTmpl in tmpl.getParameters(PlanetTemplate::class.java)) {
      singlePlanetGenerators.add(SinglePlanetGenerator(planetTmpl, rand))
    }
  }

  /**
   * Renders a planet into the given [Image].
   */
  fun render(img: Image) {
    var i = 0
    for (planetGenerator in singlePlanetGenerators) {
      for (y in 0 until img.height) {
        for (x in 0 until img.width) {
          val nx = x.toDouble() / img.width.toDouble() - 0.5
          val ny = y.toDouble() / img.height.toDouble() - 0.5
          val c = planetGenerator.getPixelColour(nx, ny)
          if (i == 0) {
            img.setPixelColour(x, y, c)
          } else {
            img.blendPixelColour(x, y, c)
          }
        }
      }
      i++
    }
  }

  /**
   * Renders a planet into the given [BufferedImage].
   */
  fun render(img: BufferedImage) {
    var i = 0
    for (planetGenerator in singlePlanetGenerators) {
      for (y in 0 until img.height) {
        for (x in 0 until img.width) {
          val nx = x.toDouble() / img.width.toDouble() - 0.5
          val ny = y.toDouble() / img.height.toDouble() - 0.5
          val c = planetGenerator.getPixelColour(nx, ny)
          if (i == 0) {
            img.setRGB(x, y, c.toArgb())
          } else {
            var imgColour = Colour(img.getRGB(x, y))
            imgColour = blend(imgColour, c)
            img.setRGB(x, y, imgColour.toArgb())
          }
        }
      }
      i++
    }
  }
}
