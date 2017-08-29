package au.com.codeka.warworlds.planetrender;

import au.com.codeka.warworlds.common.Colour;
import au.com.codeka.warworlds.common.Image;
import au.com.codeka.warworlds.common.Vector3;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The {@see PlanetGenerator} uses this class to render a single planet image. It may (or may not)
 * then combine multiple planet images into on (e.g. for asteroids).
 */
public class SinglePlanetGenerator {
  private double planetRadius;
  private Vector3 planetOrigin;
  private double ambient;
  private Vector3 sunOrigin;
  private TextureGenerator texture;
  private Vector3 north;
  private List<au.com.codeka.warworlds.planetrender.Atmosphere> atmospheres;
  private RayWarper rayWarper;

  public SinglePlanetGenerator(Template.PlanetTemplate tmpl, Random rand) {
    planetOrigin = new Vector3(tmpl.getOriginFrom());
    Vector3.interpolate(planetOrigin, tmpl.getOriginTo(), rand.nextDouble());

    Template.WarpTemplate warpTemplate = tmpl.getParameter(Template.WarpTemplate.class);
    if (warpTemplate != null) {
      rayWarper = new RayWarper(warpTemplate, rand);
    }

    Template.TextureTemplate textureTemplate = tmpl.getParameter(Template.TextureTemplate.class);
    if (textureTemplate == null) {
      return;
    }
    texture = new TextureGenerator(textureTemplate, rand);
    sunOrigin = tmpl.getSunLocation();
    ambient = tmpl.getAmbient();
    planetRadius = tmpl.getPlanetSize();

    List<Template.AtmosphereTemplate> atmosphereTemplates =
        tmpl.getParameters(Template.AtmosphereTemplate.class);
    if (atmosphereTemplates != null && atmosphereTemplates.size() > 0) {
      atmospheres = new ArrayList<>();
      for (Template.AtmosphereTemplate atmosphereTemplate : atmosphereTemplates) {
        Atmosphere.getAtmospheres(atmospheres, atmosphereTemplate, rand);
      }
    }

    north = new Vector3(tmpl.getNorthFrom());
    Vector3.interpolate(north, tmpl.getNorthTo(), rand.nextDouble());
    north.normalize();
  }

  /**
   * Renders a planet into the given \c Image.
   */
  public void render(Image img) {
    if (texture == null) {
      return;
    }

    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        double nx = ((double) x / (double) img.getWidth()) - 0.5;
        double ny = ((double) y / (double) img.getHeight()) - 0.5;
        Colour c = getPixelColour(nx, ny);
        img.setPixelColour(x, y, c);
      }
    }
  }

  /**
   * Computes the colour of the pixel at (x,y) where each coordinate is defined to
   * be in the range (-0.5, +0.5).
   *
   * @param x The x-coordinate, between -0.5 and +0.5.
   * @param y The y-coordinate, between -0.5 and +0.5.
   * @return The colour at the given pixel.
   */
  public Colour getPixelColour(double x, double y) {
    Colour c = new Colour(Colour.TRANSPARENT);

    Vector3 ray = new Vector3(x, -y, 1.0);
    if (rayWarper != null) {
      rayWarper.warp(ray, x, y);
    }
    ray.normalize();

    Vector3 intersection = raytrace(ray);
    if (intersection != null) {
      // we intersected with the planet. Now we need to work out the colour at this point
      // on the planet.
      Colour t = queryTexture(intersection);
      double intensity = lightSphere(intersection);
      c.reset(1.0, t.r * intensity, t.g * intensity, t.b * intensity);

      if (atmospheres != null) {
        Vector3 surfaceNormal = new Vector3(intersection);
        surfaceNormal.subtract(planetOrigin);
        surfaceNormal.normalize();

        Vector3 sunDirection = new Vector3(sunOrigin);
        sunDirection.subtract(intersection);
        sunDirection.normalize();

        final int numAtmospheres = atmospheres.size();
        for (int i = 0; i < numAtmospheres; i++) {
          final Atmosphere atmosphere = atmospheres.get(i);
          Colour atmosphereColour = atmosphere.getInnerPixelColour(x + 0.5, y + 0.5,
              intersection,
              surfaceNormal,
              sunDirection,
              north);
          c = blendAtmosphere(atmosphere, c, atmosphereColour);
        }
      }
    } else if (atmospheres != null) {
      // if we're rendering an atmosphere, we need to work out the distance of this ray
      // to the planet's surface
      double u = Vector3.dot(planetOrigin, ray);
      Vector3 closest = new Vector3(ray);
      closest.scale(u);

      double distance = (Vector3.distanceBetween(closest, planetOrigin) - planetRadius);

      Vector3 surfaceNormal = new Vector3(closest);
      surfaceNormal.subtract(planetOrigin);
      surfaceNormal.normalize();

      Vector3 sunDirection = new Vector3(sunOrigin);
      sunDirection.subtract(closest);
      sunDirection.normalize();

      final int numAtmospheres = atmospheres.size();
      for (int i = 0; i < numAtmospheres; i++) {
        final Atmosphere atmosphere = atmospheres.get(i);
        Colour atmosphereColour = atmosphere.getOuterPixelColour(x + 0.5, y + 0.5,
            surfaceNormal,
            distance,
            sunDirection,
            north);
        c = blendAtmosphere(atmosphere, c, atmosphereColour);
      }
    }

    return c;
  }

  private Colour blendAtmosphere(Atmosphere atmosphere, Colour imgColour, Colour atmosphereColour) {
    switch (atmosphere.getBlendMode()) {
      case Additive:
        return Colour.add(imgColour, Colour.multiplyAlpha(atmosphereColour));
      case Alpha:
        return Colour.blend(imgColour, atmosphereColour);
      case Multiply:
        return Colour.multiply(imgColour, atmosphereColour);
    }
    return Colour.TRANSPARENT;
  }

  /**
   * Query the texture for the colour at the given intersection (in 3D space).
   */
  private Colour queryTexture(Vector3 intersection) {
    Vector3 Vn = north;

    @SuppressWarnings("SuspiciousNameCombination")
    Vector3 Ve = new Vector3(Vn.y, -Vn.x, 0.0); // (AKA Vn.cross(0, 0, 1))

    Vector3 Vp = new Vector3(intersection);
    Vp.subtract(planetOrigin);

    Ve.normalize();
    Vp.normalize();

    double phi = Math.acos(-1.0 * Vector3.dot(Vn, Vp));
    double v = phi / Math.PI;

    double theta = (Math.acos(Vector3.dot(Vp, Ve) / Math.sin(phi))) / (Math.PI * 2.0);
    double u;

    Vector3 c = Vector3.cross(Vn, Ve);
    if (Vector3.dot(c, Vp) > 0) {
      u = theta;
    } else {
      u = 1.0 - theta;
    }

    return texture.getTexel(u, v);
  }

  /**
   * Calculates light intensity from the sun.
   *
   * @param intersection Point where the ray we're currently tracing intersects with the planet.
   */
  private double lightSphere(Vector3 intersection) {
    Vector3 surfaceNormal = new Vector3(intersection);
    surfaceNormal.subtract(planetOrigin);
    surfaceNormal.normalize();

    double intensity = diffuse(surfaceNormal, intersection);
    intensity = Math.max(ambient, Math.min(1.0, intensity));

    return intensity;
  }

  /**
   * Calculates diffuse lighting at the given point with the given normal.
   *
   * @param normal Normal at the point we're calculating diffuse lighting for.
   * @param point  The point at which we're calculating diffuse lighting.
   * @return The diffuse factor of lighting.
   */
  private double diffuse(Vector3 normal, Vector3 point) {
    Vector3 directionToLight = new Vector3(sunOrigin);
    directionToLight.subtract(point);
    directionToLight.normalize();
    return Vector3.dot(normal, directionToLight);
  }

  /**
   * Traces a ray along the given direction. We assume the origin is (0,0,0) (i.e. the eye).
   *
   * @param direction The direction of the ray we're going to trace.
   * @return A \c Vector3 representing the point in space where we intersect with the planet,
   * or \c null if there's no intersection.
   */
  private Vector3 raytrace(Vector3 direction) {
    // intersection of a sphere and a line
    final double a = Vector3.dot(direction, direction);
    Vector3 tmp = new Vector3(planetOrigin);
    tmp.scale(-1);
    final double b = 2.0 * Vector3.dot(direction, tmp);
    final double c = Vector3.dot(planetOrigin, planetOrigin) - (planetRadius * planetRadius);
    final double d = (b * b) - (4.0 * a * c);

    if (d > 0.0) {
      double sign = (c < -0.00001) ? 1.0 : -1.0;
      double distance = (-b + (sign * Math.sqrt(d))) / (2.0 * a);
      Vector3 intersection = new Vector3(direction);
      intersection.scale(distance);
      return intersection;
    } else {
      return null;
    }
  }
}
