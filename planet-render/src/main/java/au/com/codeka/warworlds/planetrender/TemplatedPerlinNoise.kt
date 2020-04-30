package au.com.codeka.warworlds.planetrender

import au.com.codeka.warworlds.common.PerlinNoise
import au.com.codeka.warworlds.planetrender.Template.PerlinNoiseTemplate
import au.com.codeka.warworlds.planetrender.Template.PerlinNoiseTemplate.Interpolation
import java.util.*

/** A [PerlinNoise] which takes it's parameters from a [Template].  */
class TemplatedPerlinNoise(tmpl: PerlinNoiseTemplate, rand: Random) : PerlinNoise() {
  init {
    rawSeed = rand.nextLong()
    persistence = tmpl.persistence
    startOctave = tmpl.startOctave
    endOctave = tmpl.endOctave
    interpolator = when (tmpl.interpolation) {
      Interpolation.None -> {
        NoneInterpolator()
      }
      Interpolation.Linear -> {
        LinearInterpolator()
      }
      Interpolation.Cosine -> {
        CosineInterpolator()
      }
      else -> {
        NoneInterpolator() // ??
      }
    }
  }
}
