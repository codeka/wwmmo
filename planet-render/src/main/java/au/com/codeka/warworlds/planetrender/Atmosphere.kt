package au.com.codeka.warworlds.planetrender

import au.com.codeka.warworlds.common.*
import au.com.codeka.warworlds.common.Colour.Companion.TRANSPARENT
import au.com.codeka.warworlds.common.Vector3.Companion.dot
import au.com.codeka.warworlds.planetrender.Template.*
import au.com.codeka.warworlds.planetrender.Template.AtmosphereTemplate.InnerOuterTemplate
import au.com.codeka.warworlds.planetrender.Template.AtmosphereTemplate.StarTemplate
import java.util.*
import kotlin.math.acos

/** This class will generate an atmosphere around a planet.  */
open class Atmosphere protected constructor() {
  open val blendMode: AtmosphereTemplate.BlendMode?
    get() = AtmosphereTemplate.BlendMode.Alpha

  open fun getOuterPixelColour(u: Double, v: Double, normal: Vector3,
                               distanceToSurface: Double, sunDirection: Vector3?, north: Vector3?): Colour {
    return Colour(TRANSPARENT)
  }

  open fun getInnerPixelColour(u: Double, v: Double, pt: Vector3?,
                               normal: Vector3?, sunDirection: Vector3?, north: Vector3?): Colour {
    return Colour(TRANSPARENT)
  }

  fun updateUv(uv: Vector2?) {}
  class InnerAtmosphere(tmpl: InnerOuterTemplate?, rand: Random) : Atmosphere() {
    private val colourGradient: ColourGradient?
    private val sunShadowFactor: Double
    private val sunStartShadow: Double
    private var noisiness = 0.0
    override val blendMode: AtmosphereTemplate.BlendMode?
    private var perlin: PerlinNoise? = null

    override fun getInnerPixelColour(u: Double, v: Double, pt: Vector3?, normal: Vector3?,
                                     sunDirection: Vector3?, north: Vector3?): Colour {
      if (colourGradient == null) {
        return Colour(TRANSPARENT)
      }
      val cameraDirection = Vector3(0.0, 0.0, 0.0)
      cameraDirection.subtract(pt!!)
      cameraDirection.normalize()
      var dot = dot(cameraDirection, normal!!)
      val baseColour = colourGradient.getColour(1.0 - dot)

      // if we've on the dark side of the planet, we'll want to factor in the shadow
      dot = dot(normal, sunDirection!!)
      val sunFactor = getSunShadowFactor(dot, sunStartShadow, sunShadowFactor)
      baseColour.reset(
          baseColour.a * sunFactor, baseColour.r, baseColour.g, baseColour.b)
      if (perlin != null) {
        val noiseFactor = getNoiseFactor(u, v, perlin!!, noisiness)
        baseColour.reset(
            baseColour.a * noiseFactor,
            baseColour.r * noiseFactor,
            baseColour.r * noiseFactor,
            baseColour.b * noiseFactor)
      }
      return baseColour
    }

    init {
      colourGradient = tmpl!!.getParameter(ColourGradientTemplate::class.java)!!.colourGradient
      sunShadowFactor = tmpl.sunShadowFactor
      sunStartShadow = tmpl.sunStartShadow
      val perlinTemplate = tmpl.getParameter(PerlinNoiseTemplate::class.java)
      if (perlinTemplate != null) {
        perlin = TemplatedPerlinNoise(perlinTemplate, rand)
        noisiness = tmpl.noisiness
      }
      blendMode = tmpl.blendMode
    }
  }

  open class OuterAtmosphere(tmpl: InnerOuterTemplate?, rand: Random) : Atmosphere() {
    private val colourGradient: ColourGradient?
    private val sunShadowFactor: Double
    private val sunStartShadow: Double
    private val atmosphereSize: Double
    private var noisiness = 0.0
    private var perlin: PerlinNoise? = null
    override val blendMode: AtmosphereTemplate.BlendMode?

    override fun getOuterPixelColour(u: Double, v: Double, normal: Vector3,
                                     distanceToSurface: Double, sunDirection: Vector3?,
                                     north: Vector3?): Colour {
      var distanceToSurface = distanceToSurface
      if (colourGradient == null) {
        return Colour(TRANSPARENT)
      }
      distanceToSurface /= atmosphereSize
      val baseColour = colourGradient.getColour(distanceToSurface)
      val dot = dot(normal, sunDirection!!)
      val sunFactor = getSunShadowFactor(dot, sunStartShadow, sunShadowFactor)
      baseColour.reset(
          baseColour.a * sunFactor, baseColour.r, baseColour.g, baseColour.b)
      if (perlin != null) {
        val noiseFactor = getNoiseFactor(u, v, perlin!!, noisiness)
        baseColour.reset(
            baseColour.a * noiseFactor, baseColour.r, baseColour.g, baseColour.b)
      }
      return baseColour
    }

    init {
      colourGradient = tmpl!!.getParameter(ColourGradientTemplate::class.java)!!.colourGradient
      sunShadowFactor = tmpl.sunShadowFactor
      sunStartShadow = tmpl.sunStartShadow
      atmosphereSize = tmpl.size
      val perlinTemplate = tmpl.getParameter(PerlinNoiseTemplate::class.java)
      if (perlinTemplate != null) {
        perlin = TemplatedPerlinNoise(perlinTemplate, rand)
        noisiness = tmpl.noisiness
      }
      blendMode = tmpl.blendMode
    }
  }

  class StarAtmosphere(tmpl: StarTemplate?, rand: Random) : OuterAtmosphere(tmpl, rand) {
    private val mNumPoints: Int
    private val mBaseWidth: Double
    override fun getOuterPixelColour(u: Double, v: Double, normal: Vector3,
                                     distanceToSurface: Double, sunDirection: Vector3?, north: Vector3?): Colour {
      val baseColour = super.getOuterPixelColour(
          u, v, normal, distanceToSurface, sunDirection, north)
      normal.z = 0.0
      normal.normalize()
      val dot = dot(north!!, normal)
      var angle = acos(dot)
      val pointAngle = Math.PI * 2.0 / mNumPoints.toDouble()
      while (angle > pointAngle) {
        angle -= pointAngle
      }
      angle /= pointAngle
      var distanceToPoint = angle
      if (distanceToPoint > 0.5) {
        distanceToPoint = 1.0 - distanceToPoint
      }
      distanceToPoint *= 2.0
      distanceToPoint *= mBaseWidth
      if (distanceToPoint > 1.0) distanceToPoint = 1.0
      baseColour.reset(
          baseColour.a * (1.0 - distanceToPoint),
          baseColour.r, baseColour.g, baseColour.b)
      return baseColour
    }

    init {
      mNumPoints = tmpl!!.numPoints
      mBaseWidth = tmpl.baseWidth
    }
  }

  companion object {
    fun getAtmospheres(tmpl: AtmosphereTemplate?, rand: Random): List<Atmosphere> {
      val atmospheres = ArrayList<Atmosphere>()
      getAtmospheres(atmospheres, tmpl, rand)
      return atmospheres
    }

    fun getAtmospheres(atmospheres: MutableList<Atmosphere>,
                       tmpl: AtmosphereTemplate?,
                       rand: Random) {
      if (tmpl!!.innerTemplate != null) {
        atmospheres.add(InnerAtmosphere(tmpl.innerTemplate, rand))
      }
      if (tmpl.outerTemplate != null) {
        atmospheres.add(OuterAtmosphere(tmpl.outerTemplate, rand))
      }
      if (tmpl.starTemplate != null) {
        atmospheres.add(StarAtmosphere(tmpl.starTemplate, rand))
      }
    }

    protected fun getSunShadowFactor(dot: Double, sunStartShadow: Double,
                                     sunFactor: Double): Double {
      var dot = dot
      if (dot < 0.0) {
        dot = Math.abs(dot)

        // normally, the dot product will be 1.0 if we're on the exact opposite side of
        // the planet to the sun, and 0.0 when we're 90 degrees to the sun. We want to swap
        // that around.
        dot = 1.0 - dot
      } else {
        // if it's positive, then it's on the sun side of the planet. We'll still allow you to
        // start chopping off the atmosphere on the sun side of the planet if you want.
        dot += 1.0
      }
      if (dot < sunStartShadow) {
        val min = sunStartShadow * sunFactor
        dot = if (dot < min) {
          0.0
        } else {
          (dot - min) / (sunStartShadow - min)
        }
        return dot
      }
      return 1.0
    }

    protected fun getNoiseFactor(u: Double, v: Double, perlin: PerlinNoise,
                                 noisiness: Double): Double {
      val noise = perlin.getNoise(u, v)
      return 1.0 - noise * noisiness
    }
  }
}