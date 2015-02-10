package au.com.codeka.warworlds.intel.generator;

import com.google.common.collect.Iterables;

import org.apache.commons.csv.CSVRecord;

import javax.annotation.Nullable;

import java.util.ArrayList;

/**
 * The {@link Universe} contains all the stars we have loaded. There can be quite a lot, but so far,
 * not too many that we cannot keep them all in memory at once.
 *
 * The universe is stored as a quad tree, and the tree maps exactly to different zoom levels that we
 * export as images. That is, zoom level 0 is the entire quadtree, zoom level 1 is the first four
 * children, zoom level 2 is the children of those children and so on.
 */
public class Universe {
  private final Node rootNode;

  private Universe(Node rootNode) {
    this.rootNode = rootNode;
  }

  public void addStar(Star star) {
    rootNode.addStar(star);
  }

  public Node getRootNode() {
    return rootNode;
  }

  /**
   * Creates an empty universe with no stars. We initialize {@code #maxLevels} of nodes.
   */
  public static Universe create(int maxLevels, long size) {
    RectL bounds = new RectL(0, 0, size, size);
    Node rootNode = new Node(0, 0, 0, bounds);
    addChildren(rootNode, 1, maxLevels - 1, bounds);
    return new Universe(rootNode);
  }

  /** Called by {@link #create} to recursively add the child nodes to the given node. */
  private static void addChildren(Node node, int level, int remainingLevels, RectL bounds) {
    final long baseX = node.x * 2;
    final long baseY = node.y * 2;
    node.NE = new Node(level, baseX + 1, baseY, bounds.NE());
    node.NW = new Node(level, baseX, baseY, bounds.NW());
    node.SE = new Node(level, baseX + 1, baseY + 1, bounds.SE());
    node.SW = new Node(level, baseX, baseY + 1, bounds.SW());

    if (remainingLevels > 0) {
      addChildren(node.NE, level + 1, remainingLevels - 1, bounds.NE());
      addChildren(node.NW, level + 1, remainingLevels - 1, bounds.NW());
      addChildren(node.SE, level + 1, remainingLevels - 1, bounds.SE());
      addChildren(node.SW, level + 1, remainingLevels - 1, bounds.SW());
    }
  }

  /** Represents a star in the universe. */
  public static class Star {
    public final long x;
    public final long y;
    public final String name;
    public final String type;
    @Nullable public final String empireName;

    public Star(long x, long y, String name, String type, String empireName) {
      this.x = x;
      this.y = y;
      this.name = name;
      this.type = type;
      this.empireName = empireName;
    }

    public static Star fromCsv(CSVRecord csvRecord, long minX, long minY) {
      return new Star(
          (long)(Double.parseDouble(csvRecord.get(0)) * 1024) - minX,
          (long)(Double.parseDouble(csvRecord.get(1)) * 1024) - minY,
          csvRecord.get(2),
          csvRecord.get(4),
          csvRecord.get(5));
    }
  }

  /** Represents a node in the quad-tree the universe is stored as. */
  public static class Node {
    public final long level;
    public final long x;
    public final long y;
    public final RectL bounds;
    @Nullable public Node NW, NE, SE, SW;
    @Nullable public ArrayList<Star> stars;

    /** The number of stars in this, and all child, nodes. */
    public long numStars;

    public Node(long level, long x, long y, RectL bounds) {
      this.level = level;
      this.x = x;
      this.y = y;
      this.bounds = bounds;
    }

    public void addStar(Star star) {
      if (NW != null && NE != null && SW != null && SE != null) {
        // Add it to the leaf node, just work out which child it belongs to.
        if (NW.bounds.within(star.x, star.y)) {
          NW.addStar(star);
        } else if (NE.bounds.within(star.x, star.y)) {
          NE.addStar(star);
        } else if (SW.bounds.within(star.x, star.y)) {
          SW.addStar(star);
        } else if (SE.bounds.within(star.x, star.y)) {
          SE.addStar(star);
        } else {
          //??
        }
      } else {
        if (stars == null) {
          stars = new ArrayList<>();
        }
        stars.add(star);
      }

      numStars ++;
    }

    public Iterable<Star> allStars() {
      if (stars != null) {
        return stars;
      }

      if (NE == null || NW == null || SE == null || SW == null) {
        return new ArrayList<>();
      }

      return Iterables.concat(NE.allStars(), NW.allStars(), SE.allStars(), SW.allStars());
    }
  }

  /** A rect of longs. */
  public static class RectL {
    public final long top;
    public final long left;
    public final long bottom;
    public final long right;

    public RectL(long top, long left, long bottom, long right) {
      this.top = top;
      this.left = left;
      this.bottom = bottom;
      this.right = right;
    }

    public long width() {
      return right - left;
    }

    public long height() {
      return bottom - top;
    }

    /** Returns true if the given x,y are within the bounds of this rect. */
    public boolean within(long x, long y) {
      return (x > left && x <= right && y > top && y <= bottom);
    }

    public RectL NE() {
      return new RectL(top, left + width() / 2, top + height() / 2, right);
    }

    public RectL NW() {
      return new RectL(top, left, top + height() / 2, left + width() / 2);
    }

    public RectL SE() {
      return new RectL(top + height() / 2, left + width() / 2, bottom, right);
    }

    public RectL SW() {
      return new RectL(top + height() / 2, left, bottom, left + width() / 2);
    }
  }
}
