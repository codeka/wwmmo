package au.com.codeka.warworlds.intel.generator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/** Generates the tiles a given {@link Universe}. */
public class TileGenerator {
  private final String basePath;

  public TileGenerator(String basePath) {
    this.basePath = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
  }

  public void generate(Universe universe) {
    generate(universe.getRootNode(), 0, 0, 0);
  }

  private void generate(Universe.Node node, int zoomLevel, long x, long y) {
    generateTile(node, zoomLevel, x, y);
    if (node.NE != null && node.NW != null && node.SE != null && node.SW != null) {
      generate(node.NE, zoomLevel + 1, x * 2 + 1, y * 2);
      generate(node.NW, zoomLevel + 1, x * 2, y * 2);
      generate(node.SE, zoomLevel + 1, x * 2 + 1, y * 2 + 1);
      generate(node.SW, zoomLevel + 1, x * 2, y * 2 + 1);
    }
  }

  private void generateTile(Universe.Node node, int zoomLevel, long x, long y) {
    String fileName = String.format("%s/%d/%d/%d.png", basePath, zoomLevel, x, y);
    if (node.numStars == 0) {
      return;
    }
    System.out.println("- writing " + node.numStars + " stars to " + fileName);

    File outFile = new File(fileName);
    if (!outFile.getParentFile().exists() && !outFile.getParentFile().mkdirs()) {
      System.err.println("Error creating directory!");
      System.exit(1);
    }

    BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

    try {
      if (zoomLevel < 4) {
        generateTileTiny(node, img, zoomLevel, x, y);
      } else if (zoomLevel < 7) {
        generateTileMedium(node, img, zoomLevel, x, y);
      } else {
        generateTileLarge(node, img, zoomLevel, x, y);
      }

      ImageIO.write(img, "png", new File(fileName));
    } catch (IOException e) {
      System.err.println("Error writing file!");
    }
  }

  private void generateTileTiny(
      Universe.Node node, BufferedImage img, int zoomLevel, long x, long y) {
    for (Universe.Star star : node.allStars()) {
      float sx = (float) (star.x - node.bounds.left) / node.bounds.width() * 255.0f;
      float sy = (float) (star.y - node.bounds.top) / node.bounds.height() * 255.0f;
      img.setRGB((int) sx, (int) sy, getStarColor(star));
    }
  }

  private void generateTileMedium(
      Universe.Node node, BufferedImage img, int zoomLevel, long x, long y) {
    for (Universe.Star star : node.allStars()) {
      float sx = (float) (star.x - node.bounds.left) / node.bounds.width() * 255.0f;
      float sy = (float) (star.y - node.bounds.top) / node.bounds.height() * 255.0f;
      drawCircle(img, (int) sx, (int) sy, zoomLevel - 3, getStarColor(star));
    }
  }

  private void generateTileLarge(
      Universe.Node node, BufferedImage img, int zoomLevel, long x, long y) {
    for (Universe.Star star : node.allStars()) {
      float sx = (float) (star.x - node.bounds.left) / node.bounds.width() * 255.0f;
      float sy = (float) (star.y - node.bounds.top) / node.bounds.height() * 255.0f;
      drawCircle(img, (int) sx, (int) sy, zoomLevel - 3, getStarColor(star));
    }
  }

  private int getStarColor(Universe.Star star) {
    if (star.type.toLowerCase().equals("blue")) {
      return 0xff0000ff;
    } else if (star.type.toLowerCase().equals("white")) {
      return 0xffffffff;
    } else if (star.type.toLowerCase().equals("red")) {
      return 0xffff0000;
    } else if (star.type.toLowerCase().equals("yellow")) {
      return 0xffffff00;
    } else if (star.type.toLowerCase().equals("orange")) {
      return 0xffff6600;
    } else if (star.type.toLowerCase().equals("neutron")) {
      return 0xff0000aa;
    } else if (star.type.toLowerCase().equals("black hole")) {
      return 0xff666666;
    } else {
      return 0xff00ff00;
    }
  }

  private void drawCircle(BufferedImage img, int x, int y, int radius, int color) {
    for (int px = x - radius; px < x + radius; px++) {
      for (int py = y - radius; py < y + radius; py++) {
        if (px < 0 || py < 0 || px >= 256 || py >= 256) {
          continue;
        }
        double distance = Math.sqrt((px - x) * (px - x) + (py - y) * (py - y));
        if (distance > radius) {
          continue;
        }
        img.setRGB(px, py, color);
      }
    }
  }
}
