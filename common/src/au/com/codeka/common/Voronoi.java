package au.com.codeka.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents a Voronoi diagram (and corresponds Delaunay triangulation) of a
 * \c PointCloud. It's used as the staring point of our texture generator, control field
 * generator and lots of other things.
 */
public class Voronoi {
    protected PointCloud mPointCloud;
    protected ArrayList<Triangle> mTriangles;

    // this mapping maps points in the point cloud to the triangles that share the
    // point as a vertex
    protected HashMap<Vector2, List<Triangle>> mPointCloudToTriangles;

    // maps from points to a list of the neighbouring points
    protected HashMap<Vector2, List<Vector2>> mPointNeighbours;

    final static double EPSILON = 0.000000001;

    /**
     * Constructs a \c Voronoi diagram from the given \c PointCloud. You must call \c generate()
     * to actually generate the triangulation/voronoi.
     */
    public Voronoi(PointCloud pc) {
        mPointCloud = pc;
        generate();
    }

    /**
     * Generates the delaunay trianglation/voronoi diagram of the point cloud. 
     */
    protected void generate() {
        List<Triangle> triangles = new ArrayList<Triangle>();
        List<Vector2> points = mPointCloud.getPoints();

        // first, create a "super triangle" that encompasses the whole point cloud. This is
        // easy because the point cloud is confined to the range (0,0)-(1,1) so we just add
        // two triangles to encompass that (plus a little bit of leway)
        List<Triangle> superTriangles = createSuperTriangles(points, triangles);

        // go through the vertices and add them...
        final int size = points.size();
        for (int i = 0; i < size; i++) {
            for (Triangle t : superTriangles) {
                if (t.hasVertex(i)) {
                    continue;
                }
            }

            // add this vertex to the triangulation
            addVertex(points, triangles, i);
        }

        // now go through the triangles and copy any that don't share a vertex with the super
        // triangles to the final array
        mTriangles = new ArrayList<Triangle>();
        int numTriangles = triangles.size();
        int numSuperTriangles = superTriangles.size();

        for (int i = 0; i < numTriangles; i++) {
            Triangle t = triangles.get(i);
            boolean sharesVertex = false;
            for (int j = 0; j < numSuperTriangles; j++) {
                Triangle st = superTriangles.get(j);
                if (st.shareVertex(t)) {
                    sharesVertex = true;
                    break;
                }
            }

            if (!sharesVertex) {
                mTriangles.add(t);
            }
        }

        // remove the super triangle points (they'll be the last four we added)
        for (int i = 0; i < 4; i++) {
            mPointCloud.getPoints().remove(mPointCloud.getPoints().size() - 1);
        }

        // next, go through the list of triangles and populate mPointCloudToTriangles
        mPointCloudToTriangles = new HashMap<Vector2, List<Triangle>>();
        numTriangles = mTriangles.size();
        for (int i = 0; i < numTriangles; i++) {
            Triangle t = mTriangles.get(i);
            addTriangleToPointCloudToTrianglesMap(points.get(t.a), t);
            addTriangleToPointCloudToTrianglesMap(points.get(t.b), t);
            addTriangleToPointCloudToTrianglesMap(points.get(t.c), t);
        }
        sortPointCloudToTrianglesMap();

        // finally, go through the points again and work out all of that point's neighbours
        mPointNeighbours = new HashMap<Vector2, List<Vector2>>();
        int numPoints = points.size();
        for (int i = 0; i < numPoints; i++) {
            Vector2 pt = points.get(i);
            triangles = mPointCloudToTriangles.get(pt);
            if (triangles != null) {
                Set<Vector2> neighbours = new HashSet<Vector2>();
                numTriangles = triangles.size();
                for (int j = 0; j < numTriangles; j++) {
                    Triangle t = triangles.get(j);
                    Edge edge = t.findOppositeEdge(pt);
                    neighbours.add(points.get(edge.a));
                    neighbours.add(points.get(edge.b));
                }
                mPointNeighbours.put(pt, new ArrayList<Vector2>(neighbours));
            }
        }
    }

    /**
     * Finds the point closest to the given input point.
     */
    public Vector2 findClosestPoint(Vector2 uv) {
        Vector2 closestPoint = null;
        double closestDistance2 = 0.0;

        List<Vector2> points = mPointCloud.getPoints();
        int numPoints = points.size();
        for (int i = 0; i < numPoints; i++) {
            Vector2 pt = points.get(i);
            if (closestPoint == null) {
                closestPoint = pt;
                closestDistance2 = pt.distanceTo2(uv);
            } else {
                double distance2 = pt.distanceTo2(uv);
                if (distance2 < closestDistance2) {
                    closestDistance2 = distance2;
                    closestPoint = pt;
                }
            }
        }

        return closestPoint;
    }

    /**
     * Gets the points that neighbour the given point.
     */
    public List<Vector2> getNeighbours(Vector2 pt) {
        return mPointNeighbours.get(pt);
    }

    /**
     * When we first add triangles to the \c mPointCloudToTriangles map, they're just added in
     * any old order. This doesn't work when trying to determine if a point is inside the cell
     * or drawing the outline etc, so we need to make sure the triangles are added in clockwise
     * order around each point. This method does that.
     */
    private void sortPointCloudToTrianglesMap() {
        for (Vector2 pt : mPointCloud.getPoints()) {
            List<Triangle> unsorted = mPointCloudToTriangles.get(pt);
            if (unsorted == null) {
                continue;
            }

            ArrayList<Triangle> sorted = new ArrayList<Triangle>();
            sorted.add(unsorted.get(0));
            unsorted.remove(0);

            Edge nextEdge = sorted.get(0).findCcwEdge(pt);
            while (!unsorted.isEmpty()) {
                Triangle t = sorted.get(sorted.size() - 1);
                Vector2 otherPt = t.findOppositePoint(nextEdge);

                for (Triangle nextTriangle : unsorted) {
                    nextEdge = nextTriangle.findEdge(pt, otherPt);
                    if (nextEdge != null) {
                        unsorted.remove(nextTriangle);
                        sorted.add(nextTriangle);
                        break;
                    }
                }

                if (nextEdge == null) {
                    // if there's no more edges in the clockwise direction then it means we're
                    // at the edge of the diagram. We'll break out of this loop and start working
                    // anti-clockwise the other way...
                    break;
                }
            }

            if (!unsorted.isEmpty()) {
                // if we come in here, then means we got to the edge of the world and we now have
                // to start working anti-clockwise...
                nextEdge = sorted.get(0).findCwEdge(pt);

                while (!unsorted.isEmpty()) {
                    Triangle t = sorted.get(0);
                    Vector2 otherPt = t.findOppositePoint(nextEdge);

                    for (Triangle nextTriangle : unsorted) {
                        nextEdge = nextTriangle.findEdge(pt, otherPt);
                        if (nextEdge != null) {
                            unsorted.remove(nextTriangle);
                            sorted.add(0, nextTriangle);
                            break;
                        }
                    }
                }
            }


            mPointCloudToTriangles.put(pt, sorted);
        }
    }

    /**
     * Helper class to render the delaunay triangluation to the given \c Image. We draw lines
     * of the given \c Colour.
     */
    public void renderDelaunay(Image img, Colour c) {
        List<Vector2> points = mPointCloud.getPoints();

        for (Triangle t : mTriangles) {
            Vector2 p1 = points.get(t.a);
            Vector2 p2 = points.get(t.b);
            drawLine(img, c, p1, p2);

            p1 = points.get(t.c);
            drawLine(img, c, p1, p2);

            p2 = points.get(t.a);
            drawLine(img, c, p1, p2);
        }
    }

    /**
     * Helper class to render the Voronoi diagram to the given \c Image. We draw lines of the
     * given \c Colour.
     */
    public void renderVoronoi(Image img, Colour c) {
        for (Vector2 pt : mPointCloud.getPoints()) {
            List<Triangle> triangles = mPointCloudToTriangles.get(pt);
            if (triangles == null) {
                // shouldn't happen, but just in case...
                continue;
            }

            for (int i = 0; i < triangles.size() - 1; i++) {
                drawLine(img, c, triangles.get(i).centre, triangles.get(i+1).centre);
            }
            drawLine(img, c, triangles.get(0).centre, triangles.get(triangles.size() - 1).centre);
        }
    }

    /**
     * Gets a list of triangles that share the given point as a vertex.
     */
    public List<Triangle> getTrianglesForPoint(Vector2 pt) {
        return mPointCloudToTriangles.get(pt);
    }

    private void drawLine(Image img, Colour c, Vector2 p1, Vector2 p2) {
        int x1 = (int)(img.getWidth() * p1.x);
        int x2 = (int)(img.getWidth() * p2.x);
        int y1 = (int)(img.getHeight() * p1.y);
        int y2 = (int)(img.getHeight() * p2.y);
        img.drawLine(x1, y1, x2, y2, c);
    }

    private void addTriangleToPointCloudToTrianglesMap(Vector2 pt, Triangle t) {
        List<Triangle> value = mPointCloudToTriangles.get(pt);
        if (value == null) {
            value = new ArrayList<Triangle>();
            mPointCloudToTriangles.put(pt, value);
        }
        value.add(t);
    }

    /**
     * Adds a new point vertex (given by \c vIndex) to the given triangulation.
     */
    private void addVertex(List<Vector2> points, List<Triangle> triangles, int vIndex) {
        Vector2 pt = points.get(vIndex);

        // extract all triangles in the circumscribed circle around this point
        ArrayList<Triangle> circumscribedTriangles = new ArrayList<Triangle>();
        for (Triangle t : triangles) {
            if (t.isInCircumscribedCircle(pt)) {
                circumscribedTriangles.add(t);
            }
        }
        triangles.removeAll(circumscribedTriangles);

        // create an edge buffer of all edges in those triangles
        ArrayList<Edge> edgeBuffer = new ArrayList<Edge>();
        for (Triangle t : circumscribedTriangles) {
            edgeBuffer.add(new Edge(points, t.a, t.b));
            edgeBuffer.add(new Edge(points, t.b, t.c));
            edgeBuffer.add(new Edge(points, t.c, t.a));
        }

        // extract all of the edges that aren't doubled-up
        ArrayList<Edge> uniqueEdges = new ArrayList<Edge>();
        for (Edge e : edgeBuffer) {
            if (!e.isIn(edgeBuffer)) {
                uniqueEdges.add(e);
            }
        }

        // now add triangles using the unique edges and the new point!
        for (Edge e : uniqueEdges) {
            triangles.add(new Triangle(points, vIndex, e.a, e.b));
        }
    }

    /**
     * Creates two "super" triangles that encompasses all points in the point cloud.
     * 
     * @param points The point cloud points.
     * @param triangles The triangle list we'll be generating.
     * @return The super triangles we added. Any triangles in the final triangulation that share
     *   a vertex with these will need to be removed as well.
     */
    private List<Triangle> createSuperTriangles(List<Vector2> points, List<Triangle> triangles) {
        double minX = 1.0, minY = 1.0, maxX = 0.0, maxY = 0.0;
        for (Vector2 pt : points) {
            if (pt.x < minX) {
                minX = pt.x;
            }
            if (pt.x > maxX) {
                maxX = pt.x;
            }
            if (pt.y < minY) {
                minY = pt.y;
            }
            if (pt.y > maxY) {
                maxY = pt.y;
            }
        }

        points.add(Vector2.pool.borrow().reset(minX - 0.1, minY - 0.1));
        points.add(Vector2.pool.borrow().reset(minX - 0.1, maxY + 0.1));
        points.add(Vector2.pool.borrow().reset(maxX + 0.1, maxY + 0.1));
        points.add(Vector2.pool.borrow().reset(maxX + 0.1, minY - 0.1));

        ArrayList<Triangle> superTriangles = new ArrayList<Triangle>();
        superTriangles.add(new Triangle(points, points.size() - 4, points.size() - 3, points.size() - 2));
        superTriangles.add(new Triangle(points, points.size() - 4, points.size() - 2, points.size() - 1));

        for (Triangle t : superTriangles) {
            triangles.add(t);
        }

        return superTriangles;
    }
}
