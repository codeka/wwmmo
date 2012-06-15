package au.com.codeka.planetrender;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A point cloud is, well, a cloud of points. We use it to generate a voroni/delauny mapping
 * that is then use to generate planet textures.
 * 
 * The points are always bound to the square (0,0), (1,1).
 */
public class PointCloud {
    private ArrayList<Vector2> mPoints;

    public PointCloud() {
        mPoints = new ArrayList<Vector2>();
    }

    public PointCloud(Template.PointCloudTemplate tmpl, Random rand) {
        Generator g = null;
        if (tmpl.getGenerator() == Template.PointCloudTemplate.Generator.Random) {
            g = new RandomGenerator();
        } else if (tmpl.getGenerator() == Template.PointCloudTemplate.Generator.Poisson) {
            g = new PoissonGenerator();
        } else {
            throw new RuntimeException("Unknown PointCloudGenerator: "+tmpl.getGenerator());
        }

        mPoints = g.generate(tmpl, rand);
    }

    public List<Vector2> getPoints() {
        return mPoints;
    }

    /**
     * Helper class to render this point cloud to the given \c Image (mostly for debugging).
     */
    public void render(Image img) {
        for (Vector2 p : mPoints) {
            int x = (int)(img.getWidth() * p.x);
            int y = (int)(img.getHeight() * p.y);
            img.drawCircle(x, y, 5.0, Colour.RED);
        }
    }

    /**
     * This is the base class for implementations that generate point clouds. We contain the
     * various properties the control how many points to generate, "randomness" etc.
     */
    public static abstract class Generator {
        protected abstract ArrayList<Vector2> generate(Template.PointCloudTemplate tmpl, Random rand);
    }

    /**
     * Generates points by simply generating random (x,y) coordinates. This isn't usually
     * very useful, because the points tend to clump up and look unrealistic.
     */
    public static class RandomGenerator extends Generator {
        @Override
        protected ArrayList<Vector2> generate(Template.PointCloudTemplate tmpl, Random rand) {
            // numPointsFactor will be a number between 0.75 and 1.25 which we'll use
            // to adjust the number of points we generate
            double numPointsFactor = rand.nextDouble();
            numPointsFactor = 0.75 + 0.5*numPointsFactor;

            int numPoints = 25 + (int) (475 * tmpl.getDensity() * numPointsFactor);
            if (numPoints < 25) {
                numPoints = 25;
            }

            ArrayList<Vector2> points = new ArrayList<Vector2>();
            for (int i = 0; i < numPoints; i++) {
                Vector2 p = new Vector2(rand.nextDouble(), rand.nextDouble());
                points.add(p);
            }
            return points;
        }
    }

    /**
     * Uses a poisson generator to generate more "natural" looking random points than the
     * basic \c RandomGenerator does.
     */
    public static class PoissonGenerator extends Generator {
        @Override
        protected ArrayList<Vector2> generate(Template.PointCloudTemplate tmpl, Random rand) {
            ArrayList<Vector2> points = new ArrayList<Vector2>();
            ArrayList<Vector2> unprocessed = new ArrayList<Vector2>();
            unprocessed.add(new Vector2(rand.nextDouble(), rand.nextDouble()));

            // we want minDistance to be small when density is high and big when density
            // is small.
            double minDistance = 0.001 + ((1.0 / tmpl.getDensity()) * 0.03);

            // packing is how many points around each point we'll test for a new location.
            // a high randomness means we'll have a low number (more random) and a low randomness
            // means a high number (more uniform).
            int packing = 10 + (int) ((1.0 - tmpl.getRandomness()) * 90);

            while (!unprocessed.isEmpty()) {
                // get a random point from the unprocessed list
                int index = rand.nextInt(unprocessed.size());
                Vector2 point = unprocessed.get(index);
                unprocessed.remove(index);

                // if there's another point too close to this one, ignore it
                if (inNeighbourhood(points, point, minDistance)) {
                    continue;
                }

                // otherwise, this is a good one
                points.add(point);

                // now generate a bunch of points around this one...
                for (int i = 0; i < packing; i++) {
                    Vector2 newPoint = generatePointAround(rand, point, minDistance);
                    if (newPoint.x < 0.0 || newPoint.x > 1.0) {
                        continue;
                    }
                    if (newPoint.y < 0.0 || newPoint.y > 1.0) {
                        continue;
                    }

                    unprocessed.add(newPoint);
                }
            }

            return points;
        }

        /**
         * Generates a new point around the given centre point and at least \c minDistance from it.
         */
        private Vector2 generatePointAround(Random rand, Vector2 point, double minDistance) {
            double radius = minDistance * (1.0 + rand.nextDouble());
            double angle = 2.0 * Math.PI * rand.nextDouble();

            return new Vector2(point.x + radius * Math.cos(angle),
                                point.y + radius * Math.sin(angle));
        }

        /**
         * Checks whether the given new point is too close to any previously-generated points.
         * 
         * @param points The list of points we've already generated.
         * @param point The point we've just generated and need to check.
         * @param minDistance The minimum distance we accept as being valid.
         * @param cellSize The "cell s
         * @return
         */
        private boolean inNeighbourhood(List<Vector2> points, Vector2 point, double minDistance) {
            for (Vector2 otherPoint : points) {
                if (point.distanceTo(otherPoint) < minDistance) {
                    return true;
                }
            }

            return false;
        }

    }
}
