package au.com.codeka.warworlds.planetrender

import au.com.codeka.warworlds.common.*
import au.com.codeka.warworlds.planetrender.Template.*
import java.util.*

class TextureGenerator(tmpl: TextureTemplate, rand: Random) {
  private val generator: Generator
  private val scaleX: Double
  private val scaleY: Double

  /**
   * Gets the colour of the texel at the given (u,v) coordinates.
   */
  fun getTexel(u: Double, v: Double): Colour {
    return generator.getTexel(u * scaleX, v * scaleY)
  }

  /**
   * Renders the complete texture to the given \c Image, mostly useful for debugging.
   */
  fun renderTexture(img: Image) {
    for (y in 0 until img.height) {
      for (x in 0 until img.width) {
        val u = x.toDouble() / img.width.toDouble()
        val v = y.toDouble() / img.height.toDouble()
        img.setPixelColour(x, y, getTexel(u, v))
      }
    }
  }

  internal abstract class Generator {
    abstract fun getTexel(u: Double, v: Double): Colour
  }

  /**
   * This generator just returns a colour based on how from the points the texel is.
   */
  internal class VoronoiMapGenerator(tmpl: TextureTemplate, rand: Random) : Generator() {
    private val voronoi: Voronoi
    private val colourGradient: ColourGradient
    private val noise: PerlinNoise?
    private val noisiness: Double

    override fun getTexel(u: Double, v: Double): Colour {
      val uv = Vector2(u, v)
      val pt = voronoi.findClosestPoint(uv)

      // find the closest neighbour
      var neighbour: Vector2? = null
      var neighbourDistance2 = 1.0
      val neighbours = voronoi.getNeighbours(pt!!)
      val num = neighbours!!.size
      for (i in 0 until num) {
        val n = neighbours[i]
        if (neighbour == null) {
          neighbour = n
          neighbourDistance2 = uv.distanceTo2(n)
        } else {
          val distance2 = uv.distanceTo2(n)
          if (distance2 < neighbourDistance2) {
            neighbour = n
            neighbourDistance2 = distance2
          }
        }
      }
      val neighbourDistance = Math.sqrt(neighbourDistance2)
      val distance = uv.distanceTo(pt)
      val totalDistance = distance + neighbourDistance
      var normalizedDistance = distance / (totalDistance / 2.0)
      if (noise != null) {
        val noise = noise.getNoise(u, v)
        normalizedDistance += noisiness / 2.0 - noise * noisiness
      }
      return colourGradient.getColour(normalizedDistance)
    }

    init {
      val voronoiTmpl = tmpl.getParameter(VoronoiTemplate::class.java)!!
      voronoi = TemplatedVoronoi(voronoiTmpl, rand)
      colourGradient = tmpl.getParameter(ColourGradientTemplate::class.java)!!.colourGradient
      noisiness = tmpl.noisiness
      val noiseTemplate = tmpl.getParameter(PerlinNoiseTemplate::class.java)
      noise = if (noiseTemplate != null) {
        TemplatedPerlinNoise(noiseTemplate, rand)
      } else {
        null
      }
    }
  }

  /**
   * A texture generator that generates textures based on perlin noise.
   */
  internal class PerlinNoiseGenerator(tmpl: TextureTemplate, rand: Random) : Generator() {
    private val noise: PerlinNoise
    private val colourGradient: ColourGradient
    override fun getTexel(u: Double, v: Double): Colour {
      val noise = noise.getNoise(u, v)
      return colourGradient.getColour(noise)
    }

    init {
      noise = TemplatedPerlinNoise(tmpl.getParameter(PerlinNoiseTemplate::class.java)!!, rand)
      colourGradient = tmpl.getParameter(ColourGradientTemplate::class.java)!!.colourGradient
    }
  }

  init {
    generator = when (tmpl.generator) {
      TextureTemplate.Generator.VoronoiMap -> {
        VoronoiMapGenerator(tmpl, rand)
      }
      TextureTemplate.Generator.PerlinNoise -> {
        PerlinNoiseGenerator(tmpl, rand)
      }
      else -> {
        throw Exception("Unexpected generator type: ${tmpl.generator}")
      }
    }
    scaleX = tmpl.scaleX
    scaleY = tmpl.scaleY
  }
}
