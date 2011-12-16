package au.com.codeka.warworlds.common.util;

import java.util.Random;

/**
 * This is an implementation of the "Complimentary Multiply With Carry"
 * algorithm for generating random numbers. See more details here:
 * http://groups.google.com/group/sci.crypt/msg/12152f657a3bb219
 */
public class CoolRandom {
    private long mInitial = 362436;
    private long[] mBuffer = new long[4096];
    private int mIndex = 0;

    /**
     * Creates a new \c CoolRandom instance with the given list of seeds. Useful
     * for ensuring the same sequence is generated over & over.
     */
    public CoolRandom(long[] seed) {
        this.seed(seed);
    }

    public CoolRandom(long seed) {
        this.seed(new long[] { seed });
    }

    public CoolRandom(long seed1, long seed2) {
        this.seed(new long[] { seed1, seed2 });
    }

    public CoolRandom(long seed1, long seed2, long seed3) {
        this.seed(new long[] { seed1, seed2, seed3 });
    }

    public CoolRandom(long seed1, long seed2, long seed3, long seed4) {
        this.seed(new long[] { seed1, seed2, seed3, seed4 });
    }

    public void seed(long[] seed) {
        Random r[] = new Random[seed.length];
        for(int i = 0; i < seed.length; i++) {
            r[i] = new Random(seed[i]);
        }

        for(int i = 0; i < 4096; i++) {
            long v = r[i % r.length].nextInt();
            v -= Integer.MIN_VALUE;
            mBuffer[i] = v;
        }

        mIndex = 4096;
    }

    public int nextInt() {
        mIndex = (mIndex + 1) & 4095;
        long t = 18782 * mBuffer[mIndex] + mInitial;
        mInitial = t >>> 32;
        long x = (t + mInitial) & 0xffffffffL;
        if (x < mInitial) {
            ++x;
            ++mInitial;
        }

        long v = 0xfffffffeL - x;
        mBuffer[mIndex] = v;
        return (int) v;
    }

    public long nextLong() {
        long a = nextInt();
        long b = nextInt();
        return (a << 32 | b);
    }

    /**
     * Returns the next random value as a float between 0 and 1.
     */
    public float nextFloat() {
        long a = nextInt();
        return (float)a / (float)Integer.MAX_VALUE;
    }

    /**
     * Returns \c true with the given probability. That is, if you pass 0.333
     * then you'll get \c true 33% of the time and false 66% of the time.
     */
    public boolean nextBoolean(float probability) {
        return (nextFloat() < probability);
    }

    public boolean nextBoolean() {
        return nextBoolean(0.5f);
    }
}
