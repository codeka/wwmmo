package au.com.codeka.planetrender;

import java.util.Random;

import au.com.codeka.common.PerlinNoise;

public class TemplatedPerlinNoise extends PerlinNoise {

    public TemplatedPerlinNoise(Template.PerlinNoiseTemplate tmpl, Random rand) {
        mRawSeed = rand.nextLong();
        mPersistence = tmpl.getPersistence();
        mStartOctave = tmpl.getStartOctave();
        mEndOctave = tmpl.getEndOctave();
        mRawRand = new Random();

        if (tmpl.getInterpolation() == Template.PerlinNoiseTemplate.Interpolation.None) {
            mInterpolator = new PerlinNoise.NoneInterpolator();
        } else if (tmpl.getInterpolation() == Template.PerlinNoiseTemplate.Interpolation.Linear) {
            mInterpolator = new PerlinNoise.LinearInterpolator();
        } else if (tmpl.getInterpolation() == Template.PerlinNoiseTemplate.Interpolation.Cosine) {
            mInterpolator = new PerlinNoise.CosineInterpolator();
        } else {
            mInterpolator = new PerlinNoise.NoneInterpolator(); // ??
        }
    }
}
