package au.com.codeka.planetrender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class represents a Voroni diagram (and corresponds Delaunay triangulation) of a
 * \c PointCloud. It's used as the staring point of our texture generator.
 */
public class Voroni {
    private PointCloud mPointCloud;
    private ArrayList<Triangle> mTriangles;

    /**
     * Constructs a \c Voroni diagram from the given \c PointCloud. You must call \c generate()
     * to actually generate the triangulation/voroni.
     */
    public Voroni(PointCloud pc) {
        mPointCloud = pc;
    }

    public void generate() {
        ArrayList<Triangle> triangles = new ArrayList<Triangle>();
        List<Vector2> points = mPointCloud.getPoints();

        // first, create a "super triangle" that encompasses the whole point cloud. This is
        // easy because the point cloud is confined to the rang (0,0)-(1,1) so we just add
        // two triangles to encompass that (plus a little bit of leway)
        List<Triangle> superTriangles = createSuperTriangles(points, triangles);

        // go through the vertices and add them...
        for (int i = 0; i < points.size(); i++) {
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
        for (Triangle t : triangles) {
            boolean sharesVertex = false;
            for (Triangle st : superTriangles) {
                if (st.shareVertex(t)) {
                    sharesVertex = true;
                    break;
                }
            }

            if (!sharesVertex) {
                mTriangles.add(t);
            }
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

    private void drawLine(Image img, Colour c, Vector2 p1, Vector2 p2) {
        int x1 = (int)(img.getWidth() * p1.x);
        int x2 = (int)(img.getWidth() * p2.x);
        int y1 = (int)(img.getHeight() * p1.y);
        int y2 = (int)(img.getHeight() * p2.y);
        img.drawLine(x1, y1, x2, y2, c);
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
        points.add(new Vector2(-0.1, -0.1));
        points.add(new Vector2(-0.1, 1.1));
        points.add(new Vector2(1.1, 1.1));
        points.add(new Vector2(1.1, -0.1));

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
        public int a;
        public int b;
        public int c;

        // properties of the circumscribed circle of this triangle
        public Vector2 centre;
        public double radius;

        public Triangle(List<Vector2> points, int a, int b, int c) {
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
         * Calculates the centre and radius of the circumscribed circle around this triangle.
         */
        private void calculateCircumscribedCircle(List<Vector2> points) {
            final Vector2 v1 = points.get(a);
            final Vector2 v2 = points.get(b);
            final Vector2 v3 = points.get(c);

            final double EPSILON = 0.000000001;

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

            centre = new Vector2(xc, yc);
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
    }
}
