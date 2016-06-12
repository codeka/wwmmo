package au.com.codeka.warworlds.common;

import java.util.ArrayList;
import java.util.List;

/**
 * A colour gradient is a a transformation between a floating point number between 0 and 1,
 * and a colour. The simplest gradient has two different colours at 0 and 1, and we will
 * return a value in between those two colours. More complicated options are possible by
 * having additional points in the middle.
 */
public class ColourGradient {
  private List<Node> nodes;

  public ColourGradient() {
    nodes = new ArrayList<>();
  }

  /**
   * Adds a node at the specified location on the gradient, with the specified colour.
   *
   * @param n      A value between 0 and 1.
   * @param colour The colour to return at that point.
   */
  public void addNode(double n, Colour colour) {
    int index;
    for (index = 0; index < nodes.size() - 1; index++) {
      if (nodes.get(index).n > n) {
        break;
      }
    }

    if (nodes.size() > 0) {
      index++;
    }
    nodes.add(index, new Node(n, colour));
  }

  /**
   * Gets the {@link Colour} at the corresponding point on the gradient.
   */
  public Colour getColour(double n) {
    if (nodes.size() == 0) {
      return new Colour(Colour.TRANSPARENT);
    }

    // if the value they gave us is less that our first node, return it's colour.
    if (nodes.get(0).n > n) {
      return new Colour(nodes.get(0).colour);
    }

    final int last = nodes.size() - 1;
    for (int i = 0; i < last; i++) {
      Node lhs = nodes.get(i);
      Node rhs = nodes.get(i + 1);
      if (rhs.n > n) {
        double factor = (n - lhs.n) / (rhs.n - lhs.n);

        Colour c = new Colour(lhs.colour);
        return Colour.interpolate(c, rhs.colour, factor);
      }
    }

    // if we get here, it's because the n they gave us is bigger than all nodes we've got
    return new Colour(nodes.get(nodes.size() - 1).colour);
  }

  /**
   * A node on the {@link ColourGradient}. We represent a value between 0 and 1, and the
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
