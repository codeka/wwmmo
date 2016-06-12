package au.com.codeka.warworlds.common;

/**
 * Helper class that represents an ARGB colour.
 */
public class Colour {
  public double a;
  public double r;
  public double g;
  public double b;

  public Colour() {
    a = r = g = b = 0.0;
  }

  public Colour(double a, double r, double g, double b) {
    reset(a, r, g, b);
  }

  public Colour(int argb) {
    reset(argb);
  }

  public Colour(Colour copy) {
    reset(copy);
  }

  public Colour reset(int argb) {
    double[] components = fromArgb(argb);
    a = components[0];
    r = components[1];
    g = components[2];
    b = components[3];
    return this;
  }

  public Colour reset(double na, double nr, double ng, double nb) {
    this.a = na;
    this.r = nr;
    this.g = ng;
    this.b = nb;
    return this;
  }

  public Colour reset(Colour other) {
    this.a = other.a;
    this.r = other.r;
    this.g = other.g;
    this.b = other.b;
    return this;
  }

  public int toArgb() {
    long la = (long) (255 * a);
    long lr = (long) (255 * r);
    long lg = (long) (255 * g);
    long lb = (long) (255 * b);

    return (int) ((la << 24) | (lr << 16) | (lg << 8) | lb);
  }

  public static double[] fromArgb(int argb) {
    return new double[]{
        ((((long) argb) & 0xff000000L) >> 24) / 255.0,
        (double) ((argb & 0x00ff0000) >> 16) / 255.0,
        (double) ((argb & 0x0000ff00) >> 8) / 255.0,
        (double) (argb & 0x000000ff) / 255.0
    };
  }

  public static Colour multiplyAlpha(Colour c) {
    return new Colour(
        c.a,
        c.r * c.a,
        c.g * c.a,
        c.b * c.a);
  }

  /**
   * Interpolates between two colours. If n <= 0 then lhs is returned. If n >= 1 then rhs
   * is returned. Otherwise, a colour "between" lhs and rhs is returned.
   */
  public static Colour interpolate(Colour lhs, Colour rhs, double n) {
    final double a = lhs.a + (rhs.a - lhs.a) * n;
    final double r = lhs.r + (rhs.r - lhs.r) * n;
    final double g = lhs.g + (rhs.g - lhs.g) * n;
    final double b = lhs.b + (rhs.b - lhs.b) * n;

    return new Colour(a, r, g, b);
  }

  /**
   * Blends the given rhs onto the given lhs, using alpha blending.
   */
  public static Colour blend(Colour lhs, Colour rhs) {
    double a = lhs.a + rhs.a * (1.0 - lhs.a);
    if (a > 1.0)
      a = 1.0;
    if (a <= 0.0) {
      a = 0.0;
      return Colour.TRANSPARENT;
    }

    double r = ((lhs.r * lhs.a) + (rhs.r * rhs.a * (1.0 - lhs.a))) / a;
    double g = ((lhs.g * lhs.a) + (rhs.g * rhs.a * (1.0 - lhs.a))) / a;
    double b = ((lhs.b * lhs.a) + (rhs.b * rhs.a * (1.0 - lhs.a))) / a;
    return new Colour(a, r, g, b);
  }

  /** Multiplies two colours together. */
  public static Colour multiply(Colour lhs, Colour rhs) {
    return new Colour(lhs.a * rhs.a, lhs.r * rhs.r, lhs.g * rhs.g, lhs.b * rhs.b);
  }

  /**
   * Adds the given rhs onto the given lhs, using additive blending.
   */
  public static Colour add(Colour lhs, Colour rhs) {
    double a = lhs.a + rhs.a;
    if (a > 1.0)
      a = 1.0;
    double r = lhs.r + rhs.r;
    if (r > 1.0)
      r = 1.0;
    double g = lhs.g + rhs.g;
    if (g > 1.0)
      g = 1.0;
    double b = lhs.b + rhs.b;
    if (b > 1.0)
      b = 1.0;

    return new Colour(a, r, g, b);
  }

  public static Colour RED = new Colour(1.0, 1.0, 0.0, 0.0);
  public static Colour GREEN = new Colour(1.0, 0.0, 1.0, 0.0);
  public static Colour BLUE = new Colour(1.0, 0.0, 0.0, 1.0);
  public static Colour WHITE = new Colour(1.0, 1.0, 1.0, 1.0);
  public static Colour BLACK = new Colour(1.0, 0.0, 0.0, 0.0);
  public static Colour TRANSPARENT = new Colour(0.0, 0.0, 0.0, 0.0);
}
