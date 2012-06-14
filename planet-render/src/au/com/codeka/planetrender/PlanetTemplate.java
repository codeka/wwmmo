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

    public enum BaseTextureKind {
        VoronoiMap
    }

    private Random mRand;
    private PointCloudGenerator mPointCloudGenerator;
    private double mPointCloudDensity;
    private double mPointCloudRandomness;
    private BaseTextureKind mBaseTextureKind;
    private ColourGradient mBaseTextureColourGradient;

    public PlanetTemplate() {
        mRand = new Random();
        mPointCloudGenerator = PointCloudGenerator.Poisson;
        mPointCloudDensity = 0.5;
        mPointCloudRandomness = 0.8;
        mBaseTextureKind = BaseTextureKind.VoronoiMap;
        mBaseTextureColourGradient = new ColourGradient();
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
    public PlanetTemplate setBaseTextureKind(BaseTextureKind kind) {
        mBaseTextureKind = kind;
        return this;
    }
    public PlanetTemplate setBaseTextureColourGradient(ColourGradient cg) {
        mBaseTextureColourGradient = cg;
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
    public BaseTextureKind getBaseTextureKind() {
        return mBaseTextureKind;
    }
    public ColourGradient getBaseTextureColourGradient() {
        return mBaseTextureColourGradient;
    }
}
