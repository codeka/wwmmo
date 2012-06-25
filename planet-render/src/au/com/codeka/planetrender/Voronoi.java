package au.com.codeka.planetrender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * This class represents a Voronoi diagram (and corresponds Delaunay triangulation) of a
 * \c PointCloud. It's used as the staring point of our texture generator.
 */
public class Voronoi {
    private PointCloud mPointCloud;
    private ArrayList<Triangle> mTriangles;

    // this mapping maps points in the point cloud to the triangles that share the
    // point as a vertex
    private HashMap<Vector2, List<Triangle>> mPointCloudToTriangles;

    // maps from points to a list of the neighbouring points
    private HashMap<Vector2, List<Vector2>> mPointNeighbours;

    final static double EPSILON = 0.000000001;

    /**
     * Constructs a \c Voronoi diagram from the given \c PointCloud. You must call \c generate()
     * to actually generate the triangulation/voronoi.
     */
    public Voronoi(PointCloud pc) {
        mPointCloud = pc;
        generate();
    }

    public Voronoi(Template.VoronoiTemplate tmpl, Random rand) {
        Template.PointCloudTemplate pcTmpl = tmpl.getParameter(Template.PointCloudTemplate.class);
        mPointCloud = new PointCloud(pcTmpl, rand);
        generate();
    }

    /**
     * Generates the delaunay trianglation/voronoi diagram of the point cloud. 
     */
    private void generate() {
        List<Triangle> triangles = new ArrayList<Triangle>();
        List<Vector2> points = mPointCloud.getPoints();

        // first, create a "super triangle" that encompasses the whole point cloud. This is
        // easy because the point cloud is confined to the rang (0,0)-(1,1) so we just add
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
            edgeBuffer.add(new Edge(t.a, t.b));
            edgeBuffer.add(new Edge(t.b, t.c));
            edgeBuffer.add(new Edge(t.c, t.a));
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
        points.add(Vector2.pool.borrow().reset(-0.1, -0.1));
        points.add(Vector2.pool.borrow().reset(-0.1, 1.1));
        points.add(Vector2.pool.borrow().reset(1.1, 1.1));
        points.add(Vector2.pool.borrow().reset(1.1, -0.1));

        ArrayList<Triangle> superTriangles = new ArrayList<Triangle>();
        superTriangles.add(new Triangle(points, points.size() - 4, points.size() - 3, points.size() - 2));
        superTriangles.add(new Triangle(points, points.size() - 4, points.size() - 2, points.size() - 1));

        for (Triangle t : superTriangles) {
            triangles.add(t);
        }

        return superTriangles;
    }

    /**
     * A triangle is simply three vertices from the point cloud.
     */
    public class Triangle {
        private List<Vector2> mPoints;
        public int a;
        public int b;
        public int c;

        // properties of the circumscribed circle of this triangle
        public Vector2 centre;
        public double radius;

        public Triangle(List<Vector2> points, int a, int b, int c) {
            mPoints = points;
            this.a = a;
            this.b = b;
            this.c = c;

            calculateCircumscribedCircle(points);
        }

        public boolean hasVertex(int index) {
            return (a == index || b == index || c == index);
        }

        /**
         * Checks whether this triangle shared a vertex with the given other triangle.
         */
        public boolean shareVertex(Triangle t) {
            return (t.hasVertex(a) || t.hasVertex(b) || t.hasVertex(c));
        }

        /**
         * Finds an \c Edge of this triangle that shared the two given points, or \c null if
         * no edge of this triangle shares those points.
         */
        public Edge findEdge(Vector2 p1, Vector2 p2) {
            Vector2 av = mPoints.get(a);
            Vector2 bv = mPoints.get(b);
            Vector2 cv = mPoints.get(c);

            int i1 = -1;
            int i2 = -1;

            if (av.equals(p1, EPSILON) || av.equals(p2, EPSILON)) {
                if (i1 < 0)
                    i1 = a;
                else
                    i2 = a;
            }
            if (bv.equals(p1, EPSILON) || bv.equals(p2, EPSILON)) {
                if (i1 < 0)
                    i1 = b;
                else
                    i2 = b;
            }
            if (cv.equals(p1, EPSILON) || cv.equals(p2, EPSILON)) {
                if (i1 < 0)
                    i1 = c;
                else
                    i2 = c;
            }

            if (i1 >= 0 && i2 >= 0)
                return new Edge(i1, i2);
            else
                return null;
        }

        public Edge findOppositeEdge(Vector2 pt) {
            if (pt.equals(mPoints.get(a), EPSILON)) {
                return new Edge(b, c);
            } else if (pt.equals(mPoints.get(b), EPSILON)) {
                return new Edge(a, c);
            } else if (pt.equals(mPoints.get(c), EPSILON)) {
                return new Edge(a, b);
            } else {
                return null;
            }
        }

        /**
         * Given \c pt (one of our vertices) we return the \c Edge that is on the
         * left hand side (that is, the other \c Edge contining this vertex will be
         * clockwise away from this one)
         */
        public Edge findCcwEdge(Vector2 pt) {
            int p1, p2, p3;
            if (pt.equals(mPoints.get(a), 0.00000001)) { 
                p1 = a; p2 = b; p3 = c;
            } else if (pt.equals(mPoints.get(b), 0.00000001)) { 
                p1 = b; p2 = c; p3 = a;
            } else if (pt.equals(mPoints.get(c), 0.00000001)) { 
                p1 = c; p2 = b; p3 = b;
            } else {
                return null;
            }

            // p1 is guaranteed to be pt by this point. We just need to work out
            // if p2 or p3 is the other side of the edge. We'll assume the other one
            // is p2 and check if that results in clockwise winding. If not then it
            // must be p3.
            // See: http://stackoverflow.com/a/1165943/241462
            Vector2 o1 = mPoints.get(p2);
            Vector2 o2 = mPoints.get(p3);

            double sum = (o1.x - pt.x) * (o1.y + pt.y);
            sum += (o2.x - o1.x) * (o2.y + o1.y);
            sum += (pt.x - o2.x) * (pt.y + o2.y);

            if (sum >= 0) {
                return new Edge(p1, p2);
            } else {
                return new Edge(p1, p3);
            }
        }

        public Edge findCwEdge(Vector2 pt) {
            Edge ccwEdge = findCcwEdge(pt);
            Vector2 oppositePt = this.findOppositePoint(ccwEdge);
            return findEdge(oppositePt, pt);
        }

        /**
         * Given an edge of this triangles, returns the \c Vector2 point that represents the
         * opposite point (i.e. the point that does \i not belong to this edge).
         */
        public Vector2 findOppositePoint(Edge edge) {
            if (edge.a != a && edge.b != a) {
                return mPoints.get(a);
            }
            if (edge.a != b && edge.b != b) {
                return mPoints.get(b);
            }
            if (edge.a != c && edge.b != c) {
                return mPoints.get(c);
            }

            return null;
        }

        /**
         * Calculates the centre and radius of the circumscribed circle around this triangle.
         */
        private void calculateCircumscribedCircle(List<Vector2> points) {
            final Vector2 v1 = points.get(a);
            final Vector2 v2 = points.get(b);
            final Vector2 v3 = points.get(c);

            if (Math.abs(v1.y - v2.y) < EPSILON && Math.abs(v2.y - v3.y) < EPSILON) {
                // the points are coincident, we can't do this...
                return;
            }

            double xc, yc;
            if (Math.abs(v2.y - v1.y) < EPSILON) {
                final double m = - (v3.x - v2.x) / (v3.y - v2.y);
                final double mx = (v2.x + v3.x) / 2.0;
                final double my = (v2.y + v3.y) / 2.0;
                xc = (v2.x + v1.x) / 2.0;
                yc = m * (xc - mx) + my;
            } else if (Math.abs(v3.y - v2.y) < EPSILON ) {
                final double m = - (v2.x - v1.x) / (v2.y - v1.y);
                final double mx = (v1.x + v2.x) / 2.0;
                final double my = (v1.y + v2.y) / 2.0;
                xc = (v3.x + v2.x) / 2.0;
                yc = m * (xc - mx) + my; 
            } else {
                final double m1 = - (v2.x - v1.x) / (v2.y - v1.y);
                final double m2 = - (v3.x - v2.x) / (v3.y - v2.y);
                final double mx1 = (v1.x + v2.x) / 2.0;
                final double mx2 = (v2.x + v3.x) / 2.0;
                final double my1 = (v1.y + v2.y) / 2.0;
                final double my2 = (v2.y + v3.y) / 2.0;
                xc = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2);
                yc = m1 * (xc - mx1) + my1;
            }

            centre = Vector2.pool.borrow().reset(xc, yc);
            radius = centre.distanceTo(v2);
        }

        /**
         * Checks whether the given point is inside a circle that is defined by the three
         * points in this triangle.
         *
         * That is, if we trace a circle such that the circle touches all three points of this
         * triangle, then this method will return \c true if the given point is inside that circle,
         * or \c false if it is not.
         */
        public boolean isInCircumscribedCircle(Vector2 pt) {
            if (centre == null)
                return false;

            return pt.distanceTo(centre) < radius;
        }
    }

    /**
     * An edge is a single edge of a triangle, the connection between two points.
     */
    public class Edge {
        public int a;
        public int b;

        public Edge(int a, int b) {
            this.a = a;
            this.b = b;
        }

        public boolean isIn(Collection<Edge> edges) {
            for (Edge other : edges) {
                if (other == this) {
                    // ignore ourselves in the collection
                    continue;
                }

                if ((other.a == a && other.b == b) ||
                    (other.b == a && other.a == b)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return a ^ b;
        }
    }
}
