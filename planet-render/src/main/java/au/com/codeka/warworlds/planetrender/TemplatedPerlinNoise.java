package au.com.codeka.warworlds.planetrender;

import au.com.codeka.warworlds.common.PerlinNoise;
import java.util.Random;

/** A {@link PerlinNoise} which takes it's parameters from a {@link Template}. */
public class TemplatedPerlinNoise extends PerlinNoise {
  public TemplatedPerlinNoise(Template.PerlinNoiseTemplate tmpl, Random rand) {
    setRawSeed(rand.nextLong());
    setPersistence(tmpl.getPersistence());
    setStartOctave(tmpl.getStartOctave());
    setEndOctave(tmpl.getEndOctave());

    if (tmpl.getInterpolation() == Template.PerlinNoiseTemplate.Interpolation.None) {
      setInterpolator(new PerlinNoise.NoneInterpolator());
    } else if (tmpl.getInterpolation() == Template.PerlinNoiseTemplate.Interpolation.Linear) {
      setInterpolator(new PerlinNoise.LinearInterpolator());
    } else if (tmpl.getInterpolation() == Template.PerlinNoiseTemplate.Interpolation.Cosine) {
      setInterpolator(new PerlinNoise.CosineInterpolator());
    } else {
      setInterpolator(new PerlinNoise.NoneInterpolator()); // ??
    }
  }
}
