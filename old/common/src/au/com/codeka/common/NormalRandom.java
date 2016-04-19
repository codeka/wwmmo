package au.com.codeka.common;

import java.util.Random;

public class NormalRandom extends Random {
    private static final long serialVersionUID = 1L;

    /**
     * Gets a random double between -1..1, but with a normal distribution around 0.0
     * (i.e. the majority of values will be close to 0.0, falling off in both directions).
     */
    public double next() {
        return (((double) normalRandom(1000)) / 500.0) - 1.0;
    }

    private int normalRandom(int max) {
        final int rounds = 5;

        int n = 0;
        int step = max / rounds;
        for (int i = 0; i < rounds; i++) {
            n += nextInt(step - 1);
        }

        return n;
    }
}
