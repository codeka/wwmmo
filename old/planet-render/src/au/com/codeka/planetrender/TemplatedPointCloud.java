package au.com.codeka.planetrender;

import java.util.ArrayList;
import java.util.Random;

import au.com.codeka.common.PointCloud;
import au.com.codeka.common.Vector2;

public class TemplatedPointCloud extends PointCloud {
    public TemplatedPointCloud(Template.PointCloudTemplate tmpl, Random rand) {
        Generator g = null;
        if (tmpl.getGenerator() == Template.PointCloudTemplate.Generator.Random) {
            g = new TemplatedRandomGenerator();
        } else if (tmpl.getGenerator() == Template.PointCloudTemplate.Generator.Poisson) {
            g = new TemplatedPoissonGenerator();
        } else {
            throw new RuntimeException("Unknown PointCloudGenerator: "+tmpl.getGenerator());
        }

        mPoints = g.generate(tmpl, rand);
    }

    /**
     * This is the base class for implementations that generate point clouds. We contain the
     * various properties the control how many points to generate, "randomness" etc.
     */
    public interface Generator {
        ArrayList<Vector2> generate(Template.PointCloudTemplate tmpl, Random rand);
    }

    /**
     * Generates points by simply generating random (x,y) coordinates. This isn't usually
     * very useful, because the points tend to clump up and look unrealistic.
     */
    public static class TemplatedRandomGenerator extends RandomGenerator implements Generator {
        @Override
        public ArrayList<Vector2> generate(Template.PointCloudTemplate tmpl, Random rand) {
            return generate(tmpl.getDensity(), rand);
        }
    }

    /**
     * Uses a poisson generator to generate more "natural" looking random points than the
     * basic \c RandomGenerator does.
     */
    public static class TemplatedPoissonGenerator extends PoissonGenerator implements Generator {
        @Override
        public ArrayList<Vector2> generate(Template.PointCloudTemplate tmpl, Random rand) {
            return generate(tmpl.getDensity(), tmpl.getRandomness(), rand);
        }
    }
}
