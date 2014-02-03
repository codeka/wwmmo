package au.com.codeka.planetrender;

import java.util.Random;

import au.com.codeka.common.PerlinNoise;
import au.com.codeka.common.Vector2;
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
        } else if (tmpl.getNoiseGenerator() == Template.WarpTemplate.NoiseGenerator.Spiral) {
            mNoiseGenerator = new SpiralGenerator(tmpl, rand);
        }
        mWarpFactor = tmpl.getWarpFactor();
    }

    public void warp(Vector3 vec, double u, double v) {
        mNoiseGenerator.warp(vec, u, v, mWarpFactor);
    }

    static abstract class NoiseGenerator {
        protected double getNoise(double u, double v) {
            return 0.0;
        }

        protected Vector3 getValue(double u, double v) {
            double x = getNoise(u * 0.25, v * 0.25);
            double y = getNoise(0.25 + u * 0.25, v * 0.25);
            double z = getNoise(u * 0.25, 0.25 + v * 0.25);
            return Vector3.pool.borrow().reset(x, y, z);
        }

        protected void warp(Vector3 vec, double u, double v, double factor) {
            Vector3 warpVector = getValue(u, v);
            warpVector.reset(warpVector.x * factor + (1.0 - factor),
                             warpVector.y * factor + (1.0 - factor),
                             warpVector.z * factor + (1.0 - factor));
            vec.reset(vec.x * warpVector.x,
                      vec.y * warpVector.y,
                      vec.z * warpVector.z);
            Vector3.pool.release(warpVector);
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

    static class SpiralGenerator extends NoiseGenerator {
        public SpiralGenerator(Template.WarpTemplate tmpl, Random rand) {
        }

        @Override
        protected void warp(Vector3 vec, double u, double v, double factor) {
            Vector2 uv = Vector2.pool.borrow().reset(u, v);
            uv.rotate(factor * uv.length() * 2.0 * Math.PI * 2.0 / 360.0);
            vec.reset(uv.x, -uv.y, 1.0);

            Vector2.pool.release(uv);
        }
    }
}
