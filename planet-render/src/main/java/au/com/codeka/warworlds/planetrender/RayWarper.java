package au.com.codeka.warworlds.planetrender;

import au.com.codeka.warworlds.common.PerlinNoise;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.Vector3;
import java.util.Random;

/**
 * This class takes a ray that's going in a certain direction and warps it based on a noise pattern.
 * This is used to generate misshapen asteroid images, for example.
 */
public class RayWarper {
  private NoiseGenerator noiseGenerator;
  private double warpFactor;

  public RayWarper(Template.WarpTemplate tmpl, Random rand) {
    if (tmpl.getNoiseGenerator() == Template.WarpTemplate.NoiseGenerator.Perlin) {
      noiseGenerator = new PerlinGenerator(tmpl, rand);
    } else if (tmpl.getNoiseGenerator() == Template.WarpTemplate.NoiseGenerator.Spiral) {
      noiseGenerator = new SpiralGenerator();
    }
    warpFactor = tmpl.getWarpFactor();
  }

  public void warp(Vector3 vec, double u, double v) {
    noiseGenerator.warp(vec, u, v, warpFactor);
  }

  static abstract class NoiseGenerator {
    protected double getNoise(double u, double v) {
      return 0.0;
    }

    protected Vector3 getValue(double u, double v) {
      double x = getNoise(u * 0.25, v * 0.25);
      double y = getNoise(0.25 + u * 0.25, v * 0.25);
      double z = getNoise(u * 0.25, 0.25 + v * 0.25);
      return new Vector3(x, y, z);
    }

    protected void warp(Vector3 vec, double u, double v, double factor) {
      Vector3 warpVector = getValue(u, v);
      warpVector.reset(warpVector.x * factor + (1.0 - factor),
          warpVector.y * factor + (1.0 - factor),
          warpVector.z * factor + (1.0 - factor));
      vec.reset(vec.x * warpVector.x,
          vec.y * warpVector.y,
          vec.z * warpVector.z);
    }
  }

  static class PerlinGenerator extends NoiseGenerator {
    private PerlinNoise noise;

    public PerlinGenerator(Template.WarpTemplate tmpl, Random rand) {
      noise = new TemplatedPerlinNoise(tmpl.getParameter(Template.PerlinNoiseTemplate.class), rand);
    }

    @Override
    public double getNoise(double u, double v) {
      return noise.getNoise(u, v);
    }
  }

  static class SpiralGenerator extends NoiseGenerator {
    public SpiralGenerator() {
    }

    @Override
    protected void warp(Vector3 vec, double u, double v, double factor) {
      Vector2 uv = new Vector2(u, v);
      uv.rotate(factor * uv.length() * 2.0 * Math.PI * 2.0 / 360.0);
      vec.reset(uv.x, -uv.y, 1.0);
    }
  }
}
