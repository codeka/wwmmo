package au.com.codeka.warworlds.intel;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

/** Handler which actually generates the tile images. */
public class TileHandler extends RequestHandler {
  @Override
  public void get() {
    int zoom = Integer.parseInt(getUrlParameter("zoom"));
    long x = Long.parseLong(getUrlParameter("x"));
    long y = Long.parseLong(getUrlParameter("y"));

    TileGenerator generator = new TileGenerator();
    if (getRequest().getParameter("debug") != null) {
      generator.setDebugTileCoords(true);
    }
    BufferedImage img = generator.generateTile(zoom, x, y);

    getResponse().setHeader("Content-Type", "image/png");
    setCacheTime(24, img.toString());
    try {
      ImageIO.write(img, "png", getResponse().getOutputStream());
    } catch (IOException e) {
      // Ignore errors?
    }
  }
}
