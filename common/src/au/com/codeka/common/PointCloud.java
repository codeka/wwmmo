package au.com.codeka.common;

import java.util.ArrayList;
import java.util.List;

/**
 * A point cloud is, well, a cloud of points. We use it to generate a voroni/delauny mapping
 * that is then use to generate planet textures.
 * 
 * The points are always bound to the square (0,0), (1,1).
 */
public class PointCloud {
    protected ArrayList<Vector2> mPoints;

    public PointCloud() {
        mPoints = new ArrayList<Vector2>();
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
}
