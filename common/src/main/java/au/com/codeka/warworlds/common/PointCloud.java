package au.com.codeka.warworlds.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A point cloud is, well, a cloud of points. We use it to generate a voroni/delauny mapping
 * that is then use to generate planet textures.
 * <p/>
 * The points are always bound to the square (0,0), (1,1).
 */
public class PointCloud {
  protected ArrayList<Vector2> points;

  public PointCloud() {
    points = new ArrayList<>();
  }

  public PointCloud(ArrayList<Vector2> points) {
    this.points = points;
  }

  public List<Vector2> getPoints() {
    return points;
  }

  /**
   * Helper class to render this point cloud to the given \c Image (mostly for debugging).
   */
  public void render(Image img) {
    for (Vector2 p : points) {
      int x = (int) (img.getWidth() * p.x);
      int y = (int) (img.getHeight() * p.y);
      img.drawCircle(x, y, 5.0, Colour.RED);
    }
  }

  /**
   * Generates points by simply generating random (x,y) coordinates. This isn't usually
   * very useful, because the points tend to clump up and look unrealistic.
   */
  public static class RandomGenerator {
    public ArrayList<Vector2> generate(double density, Random rand) {
      // numPointsFactor will be a number between 0.75 and 1.25 which we'll use
      // to adjust the number of points we generate
      double numPointsFactor = rand.nextDouble();
      numPointsFactor = 0.75 + 0.5 * numPointsFactor;

      int numPoints = 25 + (int) (475 * density * numPointsFactor);
      if (numPoints < 25) {
        numPoints = 25;
      }

      ArrayList<Vector2> points = new ArrayList<>(numPoints);
      for (int i = 0; i < numPoints; i++) {
        points.add(new Vector2(rand.nextDouble(), rand.nextDouble()));
      }
      return points;
    }
  }

  /**
   * Uses a poisson generator to generate more "natural" looking random points than the
   * basic \c RandomGenerator does.
   */
  public static class PoissonGenerator {
    public ArrayList<Vector2> generate(double density, double randomness, Random rand) {
      ArrayList<Vector2> points = new ArrayList<>(30); // give us some initial capacity
      ArrayList<Vector2> unprocessed = new ArrayList<>(50); // give us some initial capacity
      unprocessed.add(new Vector2(rand.nextDouble(), rand.nextDouble()));

      // we want minDistance to be small when density is high and big when density is small.
      double minDistance = 0.001 + ((1.0 / density) * 0.03);

      // packing is how many points around each point we'll test for a new location.
      // a high randomness means we'll have a low number (more random) and a low randomness
      // means a high number (more uniform).
      int packing = 10 + (int) ((1.0 - randomness) * 90);

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

      return new Vector2(
          point.x + radius * Math.cos(angle),
          point.y + radius * Math.sin(angle));
    }

    /**
     * Checks whether the given new point is too close to any previously-generated points.
     *
     * @param points      The list of points we've already generated.
     * @param point       The point we've just generated and need to check.
     * @param minDistance The minimum distance we accept as being valid.
     * @return Whether the given point is within the neighbourhood of the existing points.
     */
    private boolean inNeighbourhood(List<Vector2> points, Vector2 point, double minDistance) {
      final int n = points.size();
      for (int i = 0; i < n; i++) {
        Vector2 otherPoint = points.get(i);
        if (point.distanceTo(otherPoint) < minDistance) {
          return true;
        }
      }

      return false;
    }
  }
}
