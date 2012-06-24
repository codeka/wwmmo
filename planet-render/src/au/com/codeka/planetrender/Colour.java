package au.com.codeka.planetrender;

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
    public Colour(int argb) {
        double[] components = fromArgb(argb);
        a = components[0];
        r = components[1];
        g = components[2];
        b = components[3];
    }
    public Colour(double a, double r, double g, double b) {
        this.a = a;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int toArgb() {
        long la = (long)(255 * a);
        long lr = (long)(255 * r);
        long lg = (long)(255 * g);
        long lb = (long)(255 * b);

        return (int) ((la << 24) | (lr << 16) | (lg << 8) | lb);
    }

    public static double[] fromArgb(int argb) {
        return new double[] {
                ((((long) argb) & 0xff000000L) >> 24) / 255.0,
                (double) ((argb & 0x00ff0000) >> 16) / 255.0,
                (double) ((argb & 0x0000ff00) >> 8) / 255.0,
                (double) (argb & 0x000000ff) / 255.0
        };
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
        double a = lhs.a + rhs.a;
        if (a > 1.0)
            a = 1.0;
        double r = (lhs.r * (1.0 - rhs.a)) + (rhs.r * rhs.a);
        double g = (lhs.g * (1.0 - rhs.a)) + (rhs.g * rhs.a);
        double b = (lhs.b * (1.0 - rhs.a)) + (rhs.b * rhs.a);

        return new Colour(a, r, g, b);
    }

    public static Colour RED = new Colour(1.0, 1.0, 0.0, 0.0);
    public static Colour GREEN = new Colour(1.0, 0.0, 1.0, 0.0);
    public static Colour BLUE = new Colour(1.0, 0.0, 0.0, 1.0);
    public static Colour WHITE = new Colour(1.0, 1.0, 1.0, 1.0);
    public static Colour BLACK = new Colour(1.0, 0.0, 0.0, 0.0);
    public static Colour TRANSPARENT = new Colour(0.0, 0.0, 0.0, 0.0);
}
