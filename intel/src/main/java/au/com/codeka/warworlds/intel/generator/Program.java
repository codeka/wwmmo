package au.com.codeka.warworlds.intel.generator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.util.PriorityQueue;

/**
 * This program reads the .csv file returned by:
 *
 * https://game.war-worlds.com/realms/beta/stars?export=csv
 *
 * And generates Mercator-style tiles suitable for use with a mapping library like Leaflet
 * (http://leafletjs.com/) to visualize the entire universe.
 */
public class Program {
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: java -jar generator.jar \"path-to-stars.csv\" \"output-directory\"");
      System.exit(1);
    }
    String fileName = args[0];
    String basePath = args[1];

    // These are the bounds of the universe we create. These are just arbitrarily chosen because it
    // is often the case that there's a large number of stars outside the main "hub" of the
    // universe. We'll ignore those.
    long minX = -500000;
    long maxX = 500000;
    long minY = -500000;
    long maxY = 500000;

    System.out.println(
        String.format("- universe bounds: (%d, %d) - (%d, %d)", minX, minY, maxX, maxY));
    long width = maxX - minX;
    long height = maxY - minY;
    System.out.println(String.format("- universe dimensions: %dx%d", width, height));

    // Make sure it's a square, that's how the tiles are expected to be.
    if (height < width) {
      height = width;
    }
    if (width < height) {
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
    System.out.println("- generating " + maxZoomLevel + " zoom levels.");

    long numStarsIgnored = 0;
    Universe universe = Universe.create(maxZoomLevel, width);
    try (CSVParser parser = CSVFormat.RFC4180.parse(new FileReader(fileName))) {
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
    } catch (Exception e) {
      System.err.println(e);
      System.exit(1);
    }
    System.out.println("- universe generated, " + universe.getRootNode().numStars + " stars, "
        + numStarsIgnored + " out-of-bounds ("
        + (((float) numStarsIgnored / universe.getRootNode().numStars) * 100.0) + "%)");
    System.out.println("- exporting images to: " + basePath);

    new TileGenerator(basePath).generate(universe);
  }
}
