package au.com.codeka.warworlds.common

/** Helper class that represents an ARGB colour. */
class Colour {
  var a = 0.0
  var r = 0.0
  var g = 0.0
  var b = 0.0

  constructor() {
    b = 0.0
    g = b
    r = g
    a = r
  }

  constructor(a: Double, r: Double, g: Double, b: Double) {
    reset(a, r, g, b)
  }

  constructor(argb: Int) {
    reset(argb)
  }

  constructor(copy: Colour) {
    reset(copy)
  }

  fun reset(argb: Int): Colour {
    val components = fromArgb(argb)
    a = components[0]
    r = components[1]
    g = components[2]
    b = components[3]
    return this
  }

  fun reset(na: Double, nr: Double, ng: Double, nb: Double): Colour {
    a = na
    r = nr
    g = ng
    b = nb
    return this
  }

  fun reset(other: Colour): Colour {
    a = other.a
    r = other.r
    g = other.g
    b = other.b
    return this
  }

  fun toArgb(): Int {
    val la = (255 * a).toLong()
    val lr = (255 * r).toLong()
    val lg = (255 * g).toLong()
    val lb = (255 * b).toLong()
    return (la shl 24 or (lr shl 16) or (lg shl 8) or lb).toInt()
  }

  companion object {
    fun fromArgb(argb: Int): DoubleArray {
      return doubleArrayOf(
          (argb.toLong() and 0xff000000L shr 24) / 255.0,
          (argb and 0x00ff0000 shr 16).toDouble() / 255.0,
          (argb and 0x0000ff00 shr 8).toDouble() / 255.0,
          (argb and 0x000000ff).toDouble() / 255.0
      )
    }

    @JvmStatic
    fun multiplyAlpha(c: Colour): Colour {
      return Colour(
          c.a,
          c.r * c.a,
          c.g * c.a,
          c.b * c.a)
    }

    /**
     * Interpolates between two colours. If n <= 0 then lhs is returned. If n >= 1 then rhs
     * is returned. Otherwise, a colour "between" lhs and rhs is returned.
     */
    fun interpolate(lhs: Colour, rhs: Colour, n: Double): Colour {
      val a = lhs.a + (rhs.a - lhs.a) * n
      val r = lhs.r + (rhs.r - lhs.r) * n
      val g = lhs.g + (rhs.g - lhs.g) * n
      val b = lhs.b + (rhs.b - lhs.b) * n
      return Colour(a, r, g, b)
    }

    /**
     * Blends the given rhs onto the given lhs, using alpha blending.
     */
    fun blend(lhs: Colour, rhs: Colour): Colour {
      var a = lhs.a + rhs.a * (1.0 - lhs.a)
      if (a > 1.0) a = 1.0
      if (a <= 0.0) {
        return TRANSPARENT
      }
      val r = (lhs.r * lhs.a + rhs.r * rhs.a * (1.0 - lhs.a)) / a
      val g = (lhs.g * lhs.a + rhs.g * rhs.a * (1.0 - lhs.a)) / a
      val b = (lhs.b * lhs.a + rhs.b * rhs.a * (1.0 - lhs.a)) / a
      return Colour(a, r, g, b)
    }

    /** Multiplies two colours together.  */
    fun multiply(lhs: Colour, rhs: Colour): Colour {
      return Colour(lhs.a * rhs.a, lhs.r * rhs.r, lhs.g * rhs.g, lhs.b * rhs.b)
    }

    /**
     * Adds the given rhs onto the given lhs, using additive blending.
     */
    fun add(lhs: Colour, rhs: Colour): Colour {
      var a = lhs.a + rhs.a
      if (a > 1.0) a = 1.0
      var r = lhs.r + rhs.r
      if (r > 1.0) r = 1.0
      var g = lhs.g + rhs.g
      if (g > 1.0) g = 1.0
      var b = lhs.b + rhs.b
      if (b > 1.0) b = 1.0
      return Colour(a, r, g, b)
    }

    var RED = Colour(1.0, 1.0, 0.0, 0.0)
    var GREEN = Colour(1.0, 0.0, 1.0, 0.0)
    var BLUE = Colour(1.0, 0.0, 0.0, 1.0)
    var WHITE = Colour(1.0, 1.0, 1.0, 1.0)
    var BLACK = Colour(1.0, 0.0, 0.0, 0.0)
    var TRANSPARENT = Colour(0.0, 0.0, 0.0, 0.0)
  }
}
