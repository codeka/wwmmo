package au.com.codeka.warworlds.planetrender

import au.com.codeka.warworlds.common.Colour
import au.com.codeka.warworlds.common.Colour.Companion.TRANSPARENT
import au.com.codeka.warworlds.common.Colour.Companion.add
import au.com.codeka.warworlds.common.Colour.Companion.blend
import au.com.codeka.warworlds.common.Colour.Companion.multiply
import au.com.codeka.warworlds.common.Colour.Companion.multiplyAlpha
import au.com.codeka.warworlds.common.Image
import au.com.codeka.warworlds.common.Vector3
import au.com.codeka.warworlds.common.Vector3.Companion.cross
import au.com.codeka.warworlds.common.Vector3.Companion.distanceBetween
import au.com.codeka.warworlds.common.Vector3.Companion.dot
import au.com.codeka.warworlds.common.Vector3.Companion.interpolate
import au.com.codeka.warworlds.planetrender.Template.*
import java.util.*
import kotlin.math.acos
import kotlin.math.sin

/**
 * The {@see PlanetGenerator} uses this class to render a single planet image. It may (or may not)
 * then combine multiple planet images into on (e.g. for asteroids).
 */
class SinglePlanetGenerator(tmpl: PlanetTemplate, rand: Random) {
  private val planetRadius: Double
  private val planetOrigin: Vector3 = Vector3(tmpl.originFrom)
  private val ambient: Double
  private val sunOrigin: Vector3?
  private val texture: TextureGenerator?
  private val north: Vector3
  private var atmospheres: MutableList<Atmosphere>? = null
  private var rayWarper: RayWarper? = null

  init {
    interpolate(planetOrigin, tmpl.originFrom, rand.nextDouble())
    val warpTemplate = tmpl.getParameter(WarpTemplate::class.java)
    if (warpTemplate != null) {
      rayWarper = RayWarper(warpTemplate, rand)
    }
    val textureTemplate = tmpl.getParameter(TextureTemplate::class.java)
    if (textureTemplate != null) {
      texture = TextureGenerator(textureTemplate, rand)
      sunOrigin = tmpl.sunLocation
      ambient = tmpl.ambient
      planetRadius = tmpl.planetSize
      val atmosphereTemplates = tmpl.getParameters(AtmosphereTemplate::class.java)
      if (atmosphereTemplates.isNotEmpty()) {
        atmospheres = ArrayList()
        for (atmosphereTemplate in atmosphereTemplates) {
          Atmosphere.getAtmospheres(atmospheres!!, atmosphereTemplate, rand)
        }
      }
      north = Vector3(tmpl.northFrom)
      interpolate(north, tmpl.northTo, rand.nextDouble())
      north.normalize()
    } else {
      planetRadius = 0.0
      ambient = 0.0
      sunOrigin = null
      texture = null
      north = Vector3(0.0, 0.0, 0.0)
    }
  }

  /**
   * Renders a planet into the given \c Image.
   */
  fun render(img: Image) {
    if (texture == null) {
      return
    }
    for (y in 0 until img.height) {
      for (x in 0 until img.width) {
        val nx = x.toDouble() / img.width.toDouble() - 0.5
        val ny = y.toDouble() / img.height.toDouble() - 0.5
        val c = getPixelColour(nx, ny)
        img.setPixelColour(x, y, c)
      }
    }
  }

  /**
   * Computes the colour of the pixel at (x,y) where each coordinate is defined to be in the range
   * (-0.5, +0.5).
   *
   * @param x The x-coordinate, between -0.5 and +0.5.
   * @param y The y-coordinate, between -0.5 and +0.5.
   * @return The colour at the given pixel.
   */
  fun getPixelColour(x: Double, y: Double): Colour {
    var c = Colour(TRANSPARENT)
    val ray = Vector3(x, -y, 1.0)
    rayWarper?.warp(ray, x, y)
    ray.normalize()
    val intersection = raytrace(ray)
    if (intersection != null) {
      // we intersected with the planet. Now we need to work out the colour at this point
      // on the planet.
      val t = queryTexture(intersection)
      val intensity = lightSphere(intersection)
      c.reset(1.0, t.r * intensity, t.g * intensity, t.b * intensity)
      if (atmospheres != null) {
        val surfaceNormal = Vector3(intersection)
        surfaceNormal.subtract(planetOrigin)
        surfaceNormal.normalize()
        val sunDirection = Vector3(sunOrigin!!)
        sunDirection.subtract(intersection)
        sunDirection.normalize()
        val numAtmospheres = atmospheres!!.size
        for (i in 0 until numAtmospheres) {
          val atmosphere = atmospheres!![i]
          val atmosphereColour = atmosphere.getInnerPixelColour(x + 0.5, y + 0.5,
              intersection,
              surfaceNormal,
              sunDirection,
              north)
          c = blendAtmosphere(atmosphere, c, atmosphereColour)
        }
      }
    } else if (atmospheres != null) {
      // if we're rendering an atmosphere, we need to work out the distance of this ray
      // to the planet's surface
      val u = dot(planetOrigin, ray)
      val closest = Vector3(ray)
      closest.scale(u)
      val distance = distanceBetween(closest, planetOrigin) - planetRadius
      val surfaceNormal = Vector3(closest)
      surfaceNormal.subtract(planetOrigin)
      surfaceNormal.normalize()
      val sunDirection = Vector3(sunOrigin!!)
      sunDirection.subtract(closest)
      sunDirection.normalize()
      val numAtmospheres = atmospheres!!.size
      for (i in 0 until numAtmospheres) {
        val atmosphere = atmospheres!![i]
        val atmosphereColour = atmosphere.getOuterPixelColour(x + 0.5, y + 0.5,
            surfaceNormal,
            distance,
            sunDirection,
            north)
        c = blendAtmosphere(atmosphere, c, atmosphereColour)
      }
    }
    return c
  }

  private fun blendAtmosphere(
      atmosphere: Atmosphere, imgColour: Colour, atmosphereColour: Colour): Colour {
    return when (atmosphere.blendMode) {
      AtmosphereTemplate.BlendMode.Additive -> add(imgColour, multiplyAlpha(atmosphereColour))
      AtmosphereTemplate.BlendMode.Alpha -> blend(imgColour, atmosphereColour)
      AtmosphereTemplate.BlendMode.Multiply -> multiply(imgColour, atmosphereColour)
      else -> TRANSPARENT
    }
  }

  /**
   * Query the texture for the colour at the given intersection (in 3D space).
   */
  private fun queryTexture(intersection: Vector3): Colour {
    val Vn = north
    val Ve = Vector3(Vn.y, -Vn.x, 0.0) // (AKA Vn.cross(0, 0, 1))
    val Vp = Vector3(intersection)
    Vp.subtract(planetOrigin)
    Ve.normalize()
    Vp.normalize()
    val phi = acos(-1.0 * dot(Vn, Vp))
    val v = phi / Math.PI
    val theta = acos(dot(Vp, Ve) / sin(phi)) / (Math.PI * 2.0)
    val u: Double
    val c = cross(Vn, Ve)
    u = if (dot(c, Vp) > 0) {
      theta
    } else {
      1.0 - theta
    }
    return texture!!.getTexel(u, v)
  }

  /**
   * Calculates light intensity from the sun.
   *
   * @param intersection Point where the ray we're currently tracing intersects with the planet.
   */
  private fun lightSphere(intersection: Vector3): Double {
    val surfaceNormal = Vector3(intersection)
    surfaceNormal.subtract(planetOrigin)
    surfaceNormal.normalize()
    var intensity = diffuse(surfaceNormal, intersection)
    intensity = Math.max(ambient, Math.min(1.0, intensity))
    return intensity
  }

  /**
   * Calculates diffuse lighting at the given point with the given normal.
   *
   * @param normal Normal at the point we're calculating diffuse lighting for.
   * @param point  The point at which we're calculating diffuse lighting.
   * @return The diffuse factor of lighting.
   */
  private fun diffuse(normal: Vector3, point: Vector3): Double {
    val directionToLight = Vector3(sunOrigin!!)
    directionToLight.subtract(point)
    directionToLight.normalize()
    return dot(normal, directionToLight)
  }

  /**
   * Traces a ray along the given direction. We assume the origin is (0,0,0) (i.e. the eye).
   *
   * @param direction The direction of the ray we're going to trace.
   * @return A \c Vector3 representing the point in space where we intersect with the planet,
   * or \c null if there's no intersection.
   */
  private fun raytrace(direction: Vector3): Vector3? {
    // intersection of a sphere and a line
    val a = dot(direction, direction)
    val tmp = Vector3(planetOrigin)
    tmp.scale(-1.0)
    val b = 2.0 * dot(direction, tmp)
    val c = dot(planetOrigin, planetOrigin) - planetRadius * planetRadius
    val d = b * b - 4.0 * a * c
    return if (d > 0.0) {
      val sign = if (c < -0.00001) 1.0 else -1.0
      val distance = (-b + sign * Math.sqrt(d)) / (2.0 * a)
      val intersection = Vector3(direction)
      intersection.scale(distance)
      intersection
    } else {
      null
    }
  }
}