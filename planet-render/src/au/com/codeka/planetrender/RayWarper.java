package au.com.codeka.planetrender;

import java.util.Random;

import au.com.codeka.common.PerlinNoise;
import au.com.codeka.common.Vector3;

/**
 * This class takes a ray that's going in a certain direction and warps it based on a noise pattern. This is used
 * to generate misshapen asteroid images, for example.
 */
public class RayWarper {
    private NoiseGenerator mNoiseGenerator;
    private double mWarpFactor;

    public RayWarper(Template.WarpTemplate tmpl, Random rand) {
        if (tmpl.getNoiseGenerator() == Template.WarpTemplate.NoiseGenerator.Perlin) {
            mNoiseGenerator = new PerlinGenerator(tmpl, rand);
        }
        mWarpFactor = tmpl.getWarpFactor();
    }

    public void warp(Vector3 vec, double u, double v) {
        Vector3 warpVector = mNoiseGenerator.getValue(u, v);
        warpVector.reset(warpVector.x * mWarpFactor + (1.0 - mWarpFactor),
                         warpVector.y * mWarpFactor + (1.0 - mWarpFactor),
                         warpVector.z * mWarpFactor + (1.0 - mWarpFactor));
        vec.reset(vec.x * warpVector.x,
                  vec.y * warpVector.y,
                  vec.z * warpVector.z);
        Vector3.pool.release(warpVector);
    }

    static abstract class NoiseGenerator {
        protected abstract double getNoise(double u, double v);

        protected Vector3 getValue(double u, double v) {
            double x = getNoise(u * 0.25, v * 0.25);
            double y = getNoise(0.25 + u * 0.25, v * 0.25);
            double z = getNoise(u * 0.25, 0.25 + v * 0.25);
            return Vector3.pool.borrow().reset(x, y, z);
        }
    }

    static class PerlinGenerator extends NoiseGenerator {
        private PerlinNoise mNoise;

        public PerlinGenerator(Template.WarpTemplate tmpl, Random rand) {
            mNoise = new TemplatedPerlinNoise(tmpl.getParameter(Template.PerlinNoiseTemplate.class), rand);
        }

        @Override
        public double getNoise(double u, double v) {
            return mNoise.getNoise(u, v);
        }
    }
}
