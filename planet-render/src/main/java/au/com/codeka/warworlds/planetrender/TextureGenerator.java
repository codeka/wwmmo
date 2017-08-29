package au.com.codeka.warworlds.planetrender;

import au.com.codeka.warworlds.common.Colour;
import au.com.codeka.warworlds.common.ColourGradient;
import au.com.codeka.warworlds.common.Image;
import au.com.codeka.warworlds.common.PerlinNoise;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.Voronoi;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TextureGenerator {
  private Generator generator;
  private double scaleX;
  private double scaleY;

  public TextureGenerator(Template.TextureTemplate tmpl, Random rand) {
    if (tmpl.getGenerator() == Template.TextureTemplate.Generator.VoronoiMap) {
      generator = new VoronoiMapGenerator(tmpl, rand);
    } else if (tmpl.getGenerator() == Template.TextureTemplate.Generator.PerlinNoise) {
      generator = new PerlinNoiseGenerator(tmpl, rand);
    }

    scaleX = tmpl.getScaleX();
    scaleY = tmpl.getScaleY();
  }

  /**
   * Gets the colour of the texel at the given (u,v) coordinates.
   */
  public Colour getTexel(double u, double v) {
    return generator.getTexel(u * scaleX, v * scaleY);
  }

  /**
   * Renders the complete texture to the given \c Image, mostly useful for debugging.
   */
  public void renderTexture(Image img) {
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        double u = (double) x / (double) img.getWidth();
        double v = (double) y / (double) img.getHeight();
        img.setPixelColour(x, y, getTexel(u, v));
      }
    }
  }

  static abstract class Generator {
    public abstract Colour getTexel(double u, double v);
  }

  /**
   * This generator just returns a colour based on how from the points the texel is.
   */
  static class VoronoiMapGenerator extends Generator {
    private Voronoi voronoi;
    private ColourGradient colourGradient;
    private PerlinNoise noise;
    private double noisiness;

    public VoronoiMapGenerator(Template.TextureTemplate tmpl, Random rand) {
      Template.VoronoiTemplate voronoiTmpl = tmpl.getParameter(Template.VoronoiTemplate.class);
      voronoi = new TemplatedVoronoi(voronoiTmpl, rand);
      colourGradient = tmpl.getParameter(Template.ColourGradientTemplate.class).getColourGradient();
      noisiness = tmpl.getNoisiness();

      Template.PerlinNoiseTemplate noiseTemplate =
          tmpl.getParameter(Template.PerlinNoiseTemplate.class);
      if (noiseTemplate != null) {
        noise = new TemplatedPerlinNoise(noiseTemplate, rand);
      }
    }

    public Colour getTexel(double u, double v) {
      final Vector2 uv = new Vector2(u, v);
      final Vector2 pt = voronoi.findClosestPoint(uv);

      // find the closest neighbour
      Vector2 neighbour = null;
      double neighbourDistance2 = 1.0;
      List<Vector2> neighbours = voronoi.getNeighbours(pt);
      if (neighbours == null) {
        neighbours = new ArrayList<>();
      }

      int num = neighbours.size();
      for (int i = 0; i < num; i++) {
        Vector2 n = neighbours.get(i);
        if (neighbour == null) {
          neighbour = n;
          neighbourDistance2 = uv.distanceTo2(n);
        } else {
          double distance2 = uv.distanceTo2(n);
          if (distance2 < neighbourDistance2) {
            neighbour = n;
            neighbourDistance2 = distance2;
          }
        }
      }

      final double neighbourDistance = Math.sqrt(neighbourDistance2);
      final double distance = uv.distanceTo(pt);
      final double totalDistance = distance + neighbourDistance;

      double normalizedDistance = distance / (totalDistance / 2.0);
      if (noise != null) {
        double noise = this.noise.getNoise(u, v);
        normalizedDistance += (noisiness / 2.0) - (noise * noisiness);
      }

      return colourGradient.getColour(normalizedDistance);
    }
  }

  /**
   * A texture generator that generates textures based on perlin noise.
   */
  static class PerlinNoiseGenerator extends Generator {
    private PerlinNoise noise;
    private ColourGradient colourGradient;

    public PerlinNoiseGenerator(Template.TextureTemplate tmpl, Random rand) {
      noise = new TemplatedPerlinNoise(tmpl.getParameter(Template.PerlinNoiseTemplate.class), rand);
      colourGradient = tmpl.getParameter(Template.ColourGradientTemplate.class).getColourGradient();
    }

    public Colour getTexel(double u, double v) {
      final double noise = this.noise.getNoise(u, v);
      return colourGradient.getColour(noise);
    }
  }
}
