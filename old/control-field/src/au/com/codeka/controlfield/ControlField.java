package au.com.codeka.controlfield;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.Colour;
import au.com.codeka.common.Image;
import au.com.codeka.common.PointCloud;
import au.com.codeka.common.Triangle;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.Voronoi;

/**
 * A control field is basically a helper class that takes a point cloud representing stars, a list
 * of points within that cloud that represent the stars "owned" by an empire, and allows us to draw
 * a "control field" around all of the stars the empire owns.
 */
public class ControlField {
    protected PointCloud mPointCloud;
    protected Voronoi mVoronoi;
    protected ArrayList<Vector2> mOwnedPoints;

    public ControlField(PointCloud pointCloud, Voronoi voronoi) {
        mPointCloud = pointCloud;
        mVoronoi = voronoi;
        mOwnedPoints = new ArrayList<Vector2>();
    }

    /**
     * Added the given point to this control field.
     * 
     * @param pt The point to add. We assume this point exists within the point cloud.
     */
    public void addPointToControlField(Vector2 pt) {
        mOwnedPoints.add(0, pt);
    }

    public void clear() {
        mOwnedPoints.clear();
    }

    public void render(Image img, Colour c) {
        for (Vector2 pt : mOwnedPoints) {
            List<Triangle> triangles = mVoronoi.getTrianglesForPoint(pt);
            if (triangles == null) {
                continue;
            }

            for (int i = 0; i < triangles.size() - 1; i++) {
                Vector2 pt1 = triangles.get(i).centre;
                Vector2 pt2 = triangles.get(i + 1).centre;
                drawTriangle(img, c, pt1, pt2, pt);
            }
            Vector2 pt1 = triangles.get(0).centre;
            Vector2 pt2 = triangles.get(triangles.size() - 1).centre;
            drawTriangle(img, c, pt1, pt2, pt);
        }
    }

    private void drawTriangle(Image img, Colour c, Vector2 pt1, Vector2 pt2, Vector2 pt3) {
        int x1 = (int)(img.getWidth() * pt1.x);
        int x2 = (int)(img.getWidth() * pt2.x);
        int x3 = (int)(img.getWidth() * pt3.x);
        int y1 = (int)(img.getHeight() * pt1.y);
        int y2 = (int)(img.getHeight() * pt2.y);
        int y3 = (int)(img.getHeight() * pt3.y);
        img.drawTriangle(x1, y1, x2, y2, x3, y3, c);
    }
}
