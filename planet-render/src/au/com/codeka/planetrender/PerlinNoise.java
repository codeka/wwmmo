package au.com.codeka.planetrender;

import java.util.Random;

/**
 * This class generates perlin noise, which we can apply to various parts of the planet.
 */
public class PerlinNoise {
    private boolean mSmooth;
    private double mPersistence;
    private Interpolator mInterpolator;
    private long mRawSeed;
    private int mStartOctave;
    private int mEndOctave;
    private Random mRawRand;

    public PerlinNoise(Template.PerlinNoiseTemplate tmpl, Random rand) {
        mRawSeed = rand.nextLong();
        mSmooth = tmpl.getSmooth();
        mPersistence = tmpl.getPersistence();
        mStartOctave = tmpl.getStartOctave();
        mEndOctave = tmpl.getEndOctave();
        mRawRand = new Random();

        if (tmpl.getInterpolation() == Template.PerlinNoiseTemplate.Interpolation.None) {
            mInterpolator = new NoneInterpolator();
        } else if (tmpl.getInterpolation() == Template.PerlinNoiseTemplate.Interpolation.Linear) {
            mInterpolator = new LinearInterpolator();
        } else if (tmpl.getInterpolation() == Template.PerlinNoiseTemplate.Interpolation.Cosine) {
            mInterpolator = new CosineInterpolator();
        } else {
            mInterpolator = new NoneInterpolator(); // ??
        }
    }

    /**
     * Renders this \c PerlinNoise to the given \c Image. Useful mainly for testing/debugging.
     */
    public void render(Image img) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                double u = (double) x / (double) img.getWidth();
                double v = (double) y / (double) img.getHeight();

                double noise = getNoise(u, v);
                Colour c = Colour.fromArgb(1.0, noise, noise, noise);
                img.setPixelColour(x, y, c);
            }
        }
    }

    /**
     * Gets the noise value at the given (u,v) coordinate (which we assume range
     * from 0..1);
     */
    public double getNoise(double u, double v) {
        double total = 0.0;

        for (int octave = 0; octave <= mEndOctave - mStartOctave; octave++) {
            double freq = Math.pow(2, octave + mStartOctave) + 1;
            double amplitude = Math.pow(mPersistence, octave);

            double x = (u * freq);
            double y = (v * freq);

            double n = interpolatedNoise(x, y, octave);
            total += n * amplitude;
        }

        total = (total / 2.0) + 0.5;

        if (total < 0.0)
            total = 0.0;
        if (total > 1.0)
            total = 1.0;

        return total;
    }

    private double rawNoise(int x, int y, int octave) {
        long seed = ((octave * 1000000L) + (x * 1000000000L) + (y * 100000000000L)) ^ mRawSeed;
        mRawRand.setSeed(seed);
        double r = mRawRand.nextDouble();

        // we want the value to be between -1 and +1
        return (r * 2.0) - 1.0;
    }

    private double smoothNoise(int x, int y, int octave) {
        if (!mSmooth) {
            return rawNoise(x, y, octave);
        }

        final double[] noiseMatrix = {
                rawNoise(x - 1, y - 1, octave) / 16.0, rawNoise(x, y - 1, octave) / 8.0, rawNoise(x + 1, y - 1, octave) / 16.0,
                rawNoise(x - 1, y, octave) / 8.0,      rawNoise(x, y, octave) / 4.0,     rawNoise(x + 1, y, octave) / 8.0,
                rawNoise(x - 1, y + 1, octave) / 16.0, rawNoise(x, y + 1, octave) / 8.0, rawNoise(x + 1, y + 1, octave) / 16.0
            };
        double sum = 0.0;
        for (double d : noiseMatrix) {
            sum += d;
        }
        return sum;
    }

    private double interpolatedNoise(double x, double y, int octave) {
        final int ix = (int) x;
        final double fx = x - (double) ix;

        final int iy = (int) y;
        final double fy = y - (double) iy;

        final double nx1y1 = smoothNoise(ix, iy, octave);
        final double nx2y1 = smoothNoise(ix + 1, iy, octave);
        final double nx1y2 = smoothNoise(ix, iy + 1, octave);
        final double nx2y2 = smoothNoise(ix + 1, iy + 1, octave);

        final double ny1 = mInterpolator.interpolate(nx1y1, nx2y1, fx);
        final double ny2 = mInterpolator.interpolate(nx1y2, nx2y2, fx);

        return mInterpolator.interpolate(ny1, ny2, fy);
    }

    private interface Interpolator {
        double interpolate(double a, double b, double n);
    }

    private static class NoneInterpolator implements Interpolator {
        public double interpolate(double a, double b, double n) {
            return a;
        }
    }

    private static class LinearInterpolator implements Interpolator {
        public double interpolate(double a, double b, double n) {
            return a + n * (b - a);
        }
    }

    private static class CosineInterpolator implements Interpolator {
        public double interpolate(double a, double b, double n) {
            double radians = n * Math.PI;
            n = (1 - Math.cos(radians)) * 0.5;

            return a + n * (b - a);
        }
    }
}
