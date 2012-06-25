package au.com.codeka.planetrender;

import java.util.ArrayList;
import java.util.List;

/**
 * A colour gradient is a a transformation between a floating point number between 0 and 1,
 * and a colour. The simplest gradient has two different colours at 0 and 1, and we will
 * return a value in between those two colours. More complicated options are possible by
 * having additional points in the middle.
 */
public class ColourGradient {
    private List<Node> mNodes;

    public ColourGradient() {
        mNodes = new ArrayList<Node>();
    }

    /**
     * Adds a node at the specified location on the gradient, with the specified colour.
     * 
     * @param n A value between 0 and 1.
     * @param colour The colour to return at that point.
     */
    public void addNode(double n, Colour colour) {
        int index;
        for (index = 0; index < mNodes.size() - 1; index++) {
            if (mNodes.get(index).n > n) {
                break;
            }
        }

        if (mNodes.size() > 0) {
            index++;
        }
        mNodes.add(index, new Node(n, colour));
    }

    /**
     * Gets the \c Colour at the corresponding point on the gradient.
     */
    public Colour getColour(double n) {
        if (mNodes.size() == 0) {
            return Colour.pool.borrow().reset(Colour.TRANSPARENT);
        }

        // if the value they gave us is less that our first node, return it's colour.
        if (mNodes.get(0).n > n) {
            return Colour.pool.borrow().reset(mNodes.get(0).colour);
        }

        for (int i = 0; i < mNodes.size() - 1; i++) {
            Node lhs = mNodes.get(i);
            Node rhs = mNodes.get(i + 1);
            if (rhs.n > n) {
                double factor = (n - lhs.n) / (rhs.n - lhs.n);

                Colour c = Colour.pool.borrow().reset(lhs.colour);
                Colour.interpolate(c, rhs.colour, factor);
                return c;
            }
        }

        // if we get here, it's because the n they gave us is bigger than all
        // nodes we've got
        return Colour.pool.borrow().reset(mNodes.get(mNodes.size() - 1).colour);
    }

    /**
     * A node on the \c ColourGradient. We represent a value between 0 and 1, and the
     * corresponding colour.
     */
    class Node {
        public double n;
        public Colour colour;

        public Node(double n, Colour colour) {
            this.n = n;
            this.colour = colour;
        }
    }
}
