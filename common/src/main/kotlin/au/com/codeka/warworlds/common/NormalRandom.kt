package au.com.codeka.warworlds.common

import java.util.*

class NormalRandom : Random() {
  /**
   * Gets a random double between -1..1, but with a normal distribution around 0.0  (i.e. the
   * majority of values will be close to 0.0, falling off in both directions).
   */
  operator fun next(): Double {
    return normalRandom(1000).toDouble() / 500.0 - 1.0
  }

  private fun normalRandom(max: Int): Int {
    val rounds = 5
    var n = 0
    val step = max / rounds
    for (i in 0 until rounds) {
      n += nextInt(step - 1)
    }
    return n
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}