package au.com.codeka.warworlds.intel;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import au.com.codeka.common.Log;

/** Generates the tiles a given {@link Universe}. */
public class TileGenerator {
  private static final Log log = new Log("TileGenerator");
  private boolean debugTileCoords;

  public TileGenerator() {
    debugTileCoords = false;
  }

  public void setDebugTileCoords(boolean value) {
    debugTileCoords = true;
  }

  public BufferedImage generateTile(int zoomLevel, long x, long y) {
    BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    Universe.Node node = Universe.i.findNode(zoomLevel, x, y);
    if (node != null) {
      log.info("Writing %d stars for tile %d (%d,%d)", node.numStars, zoomLevel, x, y);

      if (zoomLevel < 4) {
        generateTileTiny(node, img);
      } else if (zoomLevel < 7) {
        generateTileMedium(node, g, zoomLevel);
      } else {
        generateTileLarge(node, g, zoomLevel);
      }
    } else if (node == null) {
      String msg = "Out of bounds";
      g.setColor(Color.DARK_GRAY);
      int width = g.getFontMetrics().stringWidth(msg);
      g.drawString(msg, 127 - (width / 2), 127);
    }
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

    if (debugTileCoords) {
      g.setColor(Color.RED);
      g.drawRect(0, 0, 255, 255);
      g.drawString(String.format("%d (%d, %d)", zoomLevel, x, y), 5, 250);
    }

    return img;
  }

  private void generateTileTiny(Universe.Node node, BufferedImage img) {
    for (Universe.Star star : node.allStars()) {
      float sx = (float) (star.x - node.bounds.left) / node.bounds.width() * 255.0f;
      float sy = (float) (star.y - node.bounds.top) / node.bounds.height() * 255.0f;

      int color = getStarColor(star);
      if (star.empireName == null) {
        color = Color.TRANSLUCENT;// color & 0x33ffffff;
      }

      img.setRGB((int) sx, (int) sy, color);
    }
  }

  private void generateTileMedium(Universe.Node node, Graphics2D g, int zoomLevel) {
    for (Universe.Star star : node.allStars()) {
      float sx = (float) (star.x - node.bounds.left) / node.bounds.width() * 255.0f;
      float sy = (float) (star.y - node.bounds.top) / node.bounds.height() * 255.0f;

      int color = getStarColor(star);
      if (star.empireName == null) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.6f));
      } else {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
      }

      g.setColor(new Color(color));
      g.drawOval((int) sx, (int) sy, zoomLevel - 3, zoomLevel - 3);
    }
  }

  private void generateTileLarge(Universe.Node node, Graphics2D g, int zoomLevel) {
    for (Universe.Star star : node.allStars()) {
      float sx = (float) (star.x - node.bounds.left) / node.bounds.width() * 255.0f;
      float sy = (float) (star.y - node.bounds.top) / node.bounds.height() * 255.0f;

      int color = getStarColor(star);
      if (star.empireName == null) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.6f));
      } else {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
      }

      g.setColor(new Color(color));
      g.drawOval((int) sx, (int) sy, zoomLevel - 3, zoomLevel - 3);
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
}
