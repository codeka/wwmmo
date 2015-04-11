package au.com.codeka.warworlds.intel;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import au.com.codeka.common.Log;

/**
 * The {@link Universe} contains all the stars we have loaded. There can be quite a lot, but so far,
 * not too many that we cannot keep them all in memory at once.
 *
 * The universe is stored as a quad tree, and the tree maps exactly to different zoom levels that we
 * export as images. That is, zoom level 0 is the entire quadtree, zoom level 1 is the first four
 * children, zoom level 2 is the children of those children and so on.
 */
public class Universe {
  private static final Log log = new Log("Universe");
  public static Universe i;

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

  /** Finds the node at the given zoom level with the given x/y. */
  public Node findNode(int zoom, long x, long y) {
    if (x < 0 || y < 0) {
      return null;
    }
    int maxCoord = (int) Math.pow(2, zoom);
    if (x >= maxCoord || y >= maxCoord) {
      return null;
    }

    return findNode(rootNode, zoom, x, y);
  }

  private static Node findNode(Node node, int zoom, long x, long y) {
    if (zoom == 0) {
      return node;
    }

    int halfBoard = (int) Math.pow(2, zoom) / 2;
    int nx = (x >= halfBoard) ? 1 : 0;
    int ny = (y >= halfBoard) ? 1 : 0;
    if (nx == 0 && ny == 0) {
      return findNode(node.NW, zoom - 1, x, y);
    } else if (nx == 1 && ny == 0) {
      return findNode(node.NE, zoom - 1, x - halfBoard, y);
    } else if (nx == 0 && ny == 1) {
      return findNode(node.SW, zoom - 1, x, y - halfBoard);
    } else /* if (nx == 1 && ny == 1) */ {
      return findNode(node.SE, zoom - 1, x - halfBoard, y - halfBoard);
    }
  }

  public static void setup() throws IOException {
    // These are the bounds of the universe we create. These are just arbitrarily chosen because it
    // is often the case that there's a large number of stars outside the main "hub" of the
    // universe. We'll ignore those.
    long minX = -500000;
    long maxX = 500000;
    long minY = -500000;
    long maxY = 500000;

    log.info("Universe bounds: (%d, %d) - (%d, %d)", minX, minY, maxX, maxY);
    long width = maxX - minX;
    long height = maxY - minY;
    log.info("Universe dimensions: %dx%d", width, height);

    // Make sure it's a square, that's how the tiles are expected to be.
    if (height < width) {
      //noinspection SuspiciousNameCombination
      height = width;
    }
    if (width < height) {
      //noinspection SuspiciousNameCombination
      width = height;
    }

    // work out how many zoom levels we need so that the maximum zoom less than 256x256 (which is
    // the size of the generator image).
    int maxZoomLevel = 0;
    long zoomLevelTileSize = width;
    while (zoomLevelTileSize > 1024) {
      maxZoomLevel ++;
      zoomLevelTileSize /= 2;
    }
    log.info("Generating %d zoom levels.", maxZoomLevel);

    long numStarsIgnored = 0;
    Universe universe = Universe.create(maxZoomLevel, width);
    try (CSVParser parser = CSVFormat.RFC4180.parse(new FileReader(Configuration.i.getCsvPath()))) {
      for(CSVRecord record : parser) {
        if (parser.getCurrentLineNumber() == 1) {
          continue;
        }
        Universe.Star star = Universe.Star.fromCsv(record, minX, minY);
        if (star.x < 0 || star.y < 0 || star.x >= width || star.y >= height) {
          numStarsIgnored ++;
          continue;
        }
        universe.addStar(star);
      }
    }
    log.info("Universe generated, %d stars, %d out-of-bounds (%.2f%%)",
        universe.getRootNode().numStars, numStarsIgnored,
        (((float) numStarsIgnored / universe.getRootNode().numStars) * 100.0));

    i = universe;
  }

  /**
   * Creates an empty universe with no stars. We initialize {@code #maxLevels} of nodes.
   */
  private static Universe create(int maxLevels, long size) {
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

    public Star(long x, long y, String name, String type, @Nullable String empireName) {
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
          Strings.isNullOrEmpty(csvRecord.get(5)) ? null : csvRecord.get(5));
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
