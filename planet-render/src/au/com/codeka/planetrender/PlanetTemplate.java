package au.com.codeka.planetrender;

import java.util.Random;

/**
 * Encapsulate all of the properties used to generate a planet.
 */
public class PlanetTemplate {
    public enum PointCloudGenerator {
        Random,
        Poisson
    }

    private PointCloudGenerator mPointCloudGenerator;
    private double mPointCloudDensity;
    private double mPointCloudRandomness;
    private Random mRand;

    public PlanetTemplate() {
        mRand = new Random();
        mPointCloudGenerator = PointCloudGenerator.Poisson;
        mPointCloudDensity = 0.75;
        mPointCloudRandomness = 0.5;
    }

    public PlanetTemplate setRandomSeed(long seed) {
        mRand = new Random(seed);
        return this;
    }
    public PlanetTemplate setPointCloudGenerator(PointCloudGenerator pcg) {
        mPointCloudGenerator = pcg;
        return this;
    }
    public PlanetTemplate setPointCloudRandomness(double randomness) {
        mPointCloudRandomness = randomness;
        return this;
    }
    public PlanetTemplate setPointCloudDensity(double density) {
        mPointCloudDensity = density;
        return this;
    }

    public Random getRandom() {
        return mRand;
    }
    public PointCloudGenerator getPointCloudGenerator() {
        return mPointCloudGenerator;
    }
    public double getPointCloudDensity() {
        return mPointCloudDensity;
    }
    public double getPointCloudRandomness() {
        return mPointCloudRandomness;
    }
}
