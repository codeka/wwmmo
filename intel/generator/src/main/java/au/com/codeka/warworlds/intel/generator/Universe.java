package au.com.codeka.warworlds.intel.generator;

import com.sun.istack.internal.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link Universe} contains all the stars we have loaded. There can be quite a lot, but so far,
 * not too many that we cannot keep them all in memory at once.
 *
 * The universe is stored as a quad tree, and the tree maps exactly to different zoom levels that we
 * export as images. That is, zoom level 0 is the entire quadtree, zoom level 1 is the first four
 * children, zoom level 2 is the children of those children and so on.
 */
public class Universe {

  /** Creates an empty universe with no stars. */
  public static Universe create() {
    return null;
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
  }

  /** Represents a node in the quad-tree the universe is stored as. */
  public static class Node {
    public final long level;
    public final long x;
    public final long y;
    public Node NW, NE, SE, SW;
    public final ArrayList<Star> stars = new ArrayList<>();

    public Node(long level, long x, long y) {
      this.level = level;
      this.x = x;
      this.y = y;
    }
  }
}
