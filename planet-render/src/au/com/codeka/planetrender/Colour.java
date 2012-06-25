package au.com.codeka.planetrender;

/**
 * Helper class that represents an ARGB colour.
 */
public class Colour implements ObjectPool.Pooled {
    public static ObjectPool<Colour> pool = new ObjectPool<Colour>(1000, new ColourCreator());

    public double a;
    public double r;
    public double g;
    public double b;

    private Colour() {
        a = r = g = b = 0.0;
    }

    @Override
    public void reset() {
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
    public static void interpolate(Colour result, Colour rhs, double n) {
        final double a = result.a + (rhs.a - result.a) * n;
        final double r = result.r + (rhs.r - result.r) * n;
        final double g = result.g + (rhs.g - result.g) * n;
        final double b = result.b + (rhs.b - result.b) * n;

        result.reset(a, r, g, b);
    }

    /**
     * Blends the given rhs onto the given lhs, using alpha blending.
     */
    public static void blend(Colour result, Colour rhs) {
        double a = result.a + rhs.a;
        if (a > 1.0) a = 1.0;
        double r = (result.r * (1.0 - rhs.a)) + (rhs.r * rhs.a);
        double g = (result.g * (1.0 - rhs.a)) + (rhs.g * rhs.a);
        double b = (result.b * (1.0 - rhs.a)) + (rhs.b * rhs.a);

        result.reset(a, r, g, b);
    }

    static class ColourCreator implements ObjectPool.PooledCreator {
        @Override
        public ObjectPool.Pooled create() {
            return new Colour();
        }
    }

    public static Colour RED = pool.borrow().reset(1.0, 1.0, 0.0, 0.0);
    public static Colour GREEN = pool.borrow().reset(1.0, 0.0, 1.0, 0.0);
    public static Colour BLUE = pool.borrow().reset(1.0, 0.0, 0.0, 1.0);
    public static Colour WHITE = pool.borrow().reset(1.0, 1.0, 1.0, 1.0);
    public static Colour BLACK = pool.borrow().reset(1.0, 0.0, 0.0, 0.0);
    public static Colour TRANSPARENT = pool.borrow().reset(0.0, 0.0, 0.0, 0.0);
}
