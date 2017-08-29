package au.com.codeka.warworlds.planetrender;

import au.com.codeka.warworlds.common.Colour;
import au.com.codeka.warworlds.common.ColourGradient;
import au.com.codeka.warworlds.common.PerlinNoise;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.Vector3;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** This class will generate an atmosphere around a planet. */
public class Atmosphere {
  protected Atmosphere() {
  }

  public static List<Atmosphere> getAtmospheres(Template.AtmosphereTemplate tmpl, Random rand) {
    ArrayList<Atmosphere> atmospheres = new ArrayList<>();
    getAtmospheres(atmospheres, tmpl, rand);
    return atmospheres;
  }

  public static void getAtmospheres(List<Atmosphere> atmospheres,
      Template.AtmosphereTemplate tmpl,
      Random rand) {
    if (tmpl.getInnerTemplate() != null) {
      atmospheres.add(new InnerAtmosphere(tmpl.getInnerTemplate(), rand));
    }
    if (tmpl.getOuterTemplate() != null) {
      atmospheres.add(new OuterAtmosphere(tmpl.getOuterTemplate(), rand));
    }
    if (tmpl.getStarTemplate() != null) {
      atmospheres.add(new StarAtmosphere(tmpl.getStarTemplate(), rand));
    }
  }

  public Template.AtmosphereTemplate.BlendMode getBlendMode() {
    return Template.AtmosphereTemplate.BlendMode.Alpha;
  }

  public Colour getOuterPixelColour(double u, double v, Vector3 normal,
      double distanceToSurface, Vector3 sunDirection, Vector3 north) {
    return new Colour(Colour.TRANSPARENT);
  }

  public Colour getInnerPixelColour(double u, double v, Vector3 pt,
      Vector3 normal, Vector3 sunDirection, Vector3 north) {
    return new Colour(Colour.TRANSPARENT);
  }

  public void updateUv(Vector2 uv) {
  }

  protected static double getSunShadowFactor(double dot, double sunStartShadow,
      double sunFactor) {
    if (dot < 0.0) {
      dot = Math.abs(dot);

      // normally, the dot product will be 1.0 if we're on the exact opposite side of
      // the planet to the sun, and 0.0 when we're 90 degrees to the sun. We want to swap
      // that around.
      dot = 1.0 - dot;
    } else {
      // if it's positive, then it's on the sun side of the planet. We'll still allow you to
      // start chopping off the atmosphere on the sun side of the planet if you want.
      dot += 1.0;
    }

    if (dot < sunStartShadow) {
      final double min = sunStartShadow * sunFactor;
      if (dot < min) {
        dot = 0.0;
      } else {
        dot = (dot - min) / (sunStartShadow - min);
      }

      return dot;
    }

    return 1.0;
  }

  protected static double getNoiseFactor(double u, double v, PerlinNoise perlin,
      double noisiness) {
    double noise = perlin.getNoise(u, v);
    return 1.0 - (noise * noisiness);
  }

  public static class InnerAtmosphere extends Atmosphere {
    private ColourGradient colourGradient;
    private double sunShadowFactor;
    private double sunStartShadow;
    private double noisiness;
    private Template.AtmosphereTemplate.BlendMode blendMode;
    private PerlinNoise perlin;

    public InnerAtmosphere(Template.AtmosphereTemplate.InnerOuterTemplate tmpl, Random rand) {
      colourGradient = tmpl.getParameter(Template.ColourGradientTemplate.class).getColourGradient();
      sunShadowFactor = tmpl.getSunShadowFactor();
      sunStartShadow = tmpl.getSunStartShadow();

      Template.PerlinNoiseTemplate perlinTemplate =
          tmpl.getParameter(Template.PerlinNoiseTemplate.class);
      if (perlinTemplate != null) {
        perlin = new TemplatedPerlinNoise(perlinTemplate, rand);
        noisiness = tmpl.getNoisiness();
      }
      blendMode = tmpl.getBlendMode();
    }

    @Override
    public Template.AtmosphereTemplate.BlendMode getBlendMode() {
      return blendMode;
    }

    @Override
    public Colour getInnerPixelColour(double u, double v, Vector3 pt, Vector3 normal,
        Vector3 sunDirection, Vector3 north) {
      if (colourGradient == null) {
        return new Colour(Colour.TRANSPARENT);
      }

      Vector3 cameraDirection = new Vector3(0, 0, 0);
      cameraDirection.subtract(pt);
      cameraDirection.normalize();
      double dot = Vector3.dot(cameraDirection, normal);

      Colour baseColour = colourGradient.getColour(1.0 - dot);

      // if we've on the dark side of the planet, we'll want to factor in the shadow
      dot = Vector3.dot(normal, sunDirection);
      double sunFactor = getSunShadowFactor(dot, sunStartShadow, sunShadowFactor);
      baseColour.reset(baseColour.a * sunFactor, baseColour.r, baseColour.g, baseColour.b);

      if (perlin != null) {
        double noiseFactor = getNoiseFactor(u, v, perlin, noisiness);
        baseColour.reset(baseColour.a * noiseFactor,
            baseColour.r * noiseFactor,
            baseColour.g * noiseFactor,
            baseColour.b * noiseFactor);
      }

      return baseColour;
    }
  }

  public static class OuterAtmosphere extends Atmosphere {
    private ColourGradient colourGradient;
    private double sunShadowFactor;
    private double sunStartShadow;
    private double atmosphereSize;
    private double noisiness;
    private PerlinNoise perlin;
    private Template.AtmosphereTemplate.BlendMode blendMode;

    public OuterAtmosphere(Template.AtmosphereTemplate.InnerOuterTemplate tmpl, Random rand) {
      colourGradient =
          tmpl.getParameter(Template.ColourGradientTemplate.class).getColourGradient();
      sunShadowFactor = tmpl.getSunShadowFactor();
      sunStartShadow = tmpl.getSunStartShadow();
      atmosphereSize = tmpl.getSize();

      Template.PerlinNoiseTemplate perlinTemplate =
          tmpl.getParameter(Template.PerlinNoiseTemplate.class);
      if (perlinTemplate != null) {
        perlin = new TemplatedPerlinNoise(perlinTemplate, rand);
        noisiness = tmpl.getNoisiness();
      }
      blendMode = tmpl.getBlendMode();
    }

    @Override
    public Template.AtmosphereTemplate.BlendMode getBlendMode() {
      return blendMode;
    }

    @Override
    public Colour getOuterPixelColour(double u, double v, Vector3 normal,
        double distanceToSurface, Vector3 sunDirection,
        Vector3 north) {
      if (colourGradient == null) {
        return new Colour(Colour.TRANSPARENT);
      }

      distanceToSurface /= atmosphereSize;
      Colour baseColour = colourGradient.getColour(distanceToSurface);

      double dot = Vector3.dot(normal, sunDirection);
      double sunFactor = getSunShadowFactor(dot, sunStartShadow, sunShadowFactor);
      baseColour.reset(baseColour.a * sunFactor, baseColour.r, baseColour.g, baseColour.b);

      if (perlin != null) {
        double noiseFactor = getNoiseFactor(u, v, perlin, noisiness);
        baseColour.reset(
            baseColour.a * noiseFactor, baseColour.r, baseColour.g, baseColour.b);
      }

      return baseColour;
    }
  }

  public static class StarAtmosphere extends OuterAtmosphere {
    private int mNumPoints;
    private double mBaseWidth;

    public StarAtmosphere(Template.AtmosphereTemplate.StarTemplate tmpl, Random rand) {
      super(tmpl, rand);

      mNumPoints = tmpl.getNumPoints();
      mBaseWidth = tmpl.getBaseWidth();
    }

    @Override
    public Colour getOuterPixelColour(double u, double v, Vector3 normal,
        double distanceToSurface, Vector3 sunDirection, Vector3 north) {
      Colour baseColour = super.getOuterPixelColour(
          u, v, normal, distanceToSurface, sunDirection, north);

      normal.z = 0;
      normal.normalize();
      double dot = Vector3.dot(north, normal);
      double angle = Math.acos(dot);

      double pointAngle = (Math.PI * 2.0) / (double) mNumPoints;
      while (angle > pointAngle) {
        angle -= pointAngle;
      }
      angle /= pointAngle;
      double distanceToPoint = angle;
      if (distanceToPoint > 0.5) {
        distanceToPoint = 1.0 - distanceToPoint;
      }
      distanceToPoint *= 2.0;

      distanceToPoint *= mBaseWidth;
      if (distanceToPoint > 1.0)
        distanceToPoint = 1.0;

      baseColour.reset(
          baseColour.a * (1.0 - distanceToPoint), baseColour.r, baseColour.g, baseColour.b);
      return baseColour;
    }
  }
}
