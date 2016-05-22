package au.com.codeka.warworlds.common.planetrender;

import java.util.Random;

import au.com.codeka.warworlds.common.PerlinNoise;

/** A {@link PerlinNoise} which takes it's parameters from a {@link Template}. */
public class TemplatedPerlinNoise extends PerlinNoise {
  public TemplatedPerlinNoise(Template.PerlinNoiseTemplate tmpl, Random rand) {
    rawSeed = rand.nextLong();
    persistence = tmpl.getPersistence();
    startOctave = tmpl.getStartOctave();
    endOctave = tmpl.getEndOctave();
    rawRand = new Random();

    if (tmpl.getInterpolation() == Template.PerlinNoiseTemplate.Interpolation.None) {
      interpolator = new PerlinNoise.NoneInterpolator();
    } else if (tmpl.getInterpolation() == Template.PerlinNoiseTemplate.Interpolation.Linear) {
      interpolator = new PerlinNoise.LinearInterpolator();
    } else if (tmpl.getInterpolation() == Template.PerlinNoiseTemplate.Interpolation.Cosine) {
      interpolator = new PerlinNoise.CosineInterpolator();
    } else {
      interpolator = new PerlinNoise.NoneInterpolator(); // ??
    }
  }
}
