package au.com.codeka.warworlds.planetrender.ui;

import au.com.codeka.warworlds.common.Colour;
import au.com.codeka.warworlds.common.Image;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

class ImagePanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private java.awt.Image image;
  private int imageWidth;
  private int imageHeight;
  private Colour backgroundColour;

  public void setImage(Image img) {
    MemoryImageSource mis =
        new MemoryImageSource(img.getWidth(), img.getHeight(), img.getArgb(), 0, img.getWidth());

    image = getTopLevelAncestor().createImage(mis);
    imageWidth = img.getWidth();
    imageHeight = img.getHeight();
    repaint();
  }

  public void saveImageAs(File f) throws IOException {
    BufferedImage img = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics g = img.getGraphics();
    g.drawImage(image, 0, 0, this);
    g.dispose();

    ImageIO.write(img, "png", f);
  }

  public void setBackgroundColour(Colour c) {
    backgroundColour = c;
    repaint();
  }

  @Override
  public void paintComponent(Graphics g) {
    int width = getWidth();
    int height = getHeight();

    // fill the entire background gray
    Color bg = Color.LIGHT_GRAY;
    if (backgroundColour != null) {
      bg = new Color((float) backgroundColour.r, (float) backgroundColour.g,
          (float) backgroundColour.b, (float) backgroundColour.a);
    }
    g.setColor(bg);
    g.fillRect(0, 0, width, height);

    if (image != null) {
      int sx = (width - imageWidth) / 2;
      int sy = (height - imageHeight) / 2;
      width = imageWidth;
      height = imageHeight;

      // If the background is set to transparent, we'll draw a checkerboard background to represent
      // the transparent parts of the image.
      if (backgroundColour == null) {
        g.setColor(Color.GRAY);
        boolean odd = false;
        for (int y = sy; y < sy + height; y += 16) {
          int xOffset = (odd ? 16 : 0);
          odd = !odd;
          for (int x = sx + xOffset; x < sx + width; x += 32) {
            g.fillRect(x, y, 16, 16);
          }
        }
      }

      g.drawImage(image, sx, sy, null);
    }
  }
}
