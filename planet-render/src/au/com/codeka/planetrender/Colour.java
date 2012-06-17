package au.com.codeka.planetrender;

/**
 * Helper class that represents an ARGB colour.
 */
public class Colour {
    public int argb;

    public Colour() {
        argb = 0x00000000;
    }
    public Colour(int argb) {
        this.argb = argb;
    }

    public void setAlpha(double a) {
        long n = (long)(255 * a);
        argb = (int) ((n << 24) | (argb & 0x00ffffff));
    }
    public void setRed(double r) {
        int n = (int)(255 * r);
        argb = (n << 16) | (argb & 0xff00ffff);
    }
    public void setGreen(double g) {
        int n = (int)(255 * g);
        argb = (n << 8) | (argb & 0xffff00ff);
    }
    public void setBlue(double b) {
        int n = (int)(255 * b);
        argb = n | (argb & 0xffffff00);
    }

    public double getAlpha() {
        return (double) ((((long) argb) & 0xff000000L) >> 24) / 255.0;
    }
    public double getRed() {
        return (double) ((argb & 0x00ff0000) >> 16) / 255.0;
    }
    public double getGreen() {
        return (double) ((argb & 0x0000ff00) >> 8) / 255.0;
    }
    public double getBlue() {
        return (double) (argb & 0x000000ff) / 255.0;
    }

    public static Colour fromArgb(double a, double r, double g, double b) {
        Colour c = new Colour();
        c.setAlpha(a);
        c.setRed(r);
        c.setGreen(g);
        c.setBlue(b);
        return c;
    }

    /**
     * Interpolates between two colours. If n <= 0 then lhs is returned. If n >= 1 then rhs
     * is returned. Otherwise, a colour "between" lhs and rhs is returned.
     */
    public static Colour interpolate(Colour lhs, Colour rhs, double n) {
        final double la = lhs.getAlpha();
        final double ra = rhs.getAlpha();
        final double lr = lhs.getRed();
        final double rr = rhs.getRed();
        final double lg = lhs.getGreen();
        final double rg = rhs.getGreen();
        final double lb = lhs.getBlue();
        final double rb = rhs.getBlue();

        final double a = la + (ra - la) * n;
        final double r = lr + (rr - lr) * n;
        final double g = lg + (rg - lg) * n;
        final double b = lb + (rb - lb) * n;

        return Colour.fromArgb(a, r, g, b);
    }

    /**
     * Blends the given rhs onto the given lhs, using alpha blending.
     */
    public static Colour blend(Colour lhs, Colour rhs) {
        final double la = lhs.getAlpha();
        final double ra = rhs.getAlpha();
        final double lr = lhs.getRed();
        final double rr = rhs.getRed();
        final double lg = lhs.getGreen();
        final double rg = rhs.getGreen();
        final double lb = lhs.getBlue();
        final double rb = rhs.getBlue();

        double a = la + ra;
        if (a > 1.0)
            a = 1.0;
        double r = (lr * (1.0 - ra)) + (rr * ra);
        double g = (lg * (1.0 - ra)) + (rg * ra);
        double b = (lb * (1.0 - ra)) + (rb * ra);

        return Colour.fromArgb(a, r, g, b);
    }

    public static Colour RED = new Colour(0xffff0000);
    public static Colour GREEN = new Colour(0xff00ff00);
    public static Colour BLUE = new Colour(0xff0000ff);
    public static Colour WHITE = new Colour(0xffffffff);
    public static Colour BLACK = new Colour(0xff000000);
    public static Colour TRANSPARENT = new Colour(0x00000000);
}
