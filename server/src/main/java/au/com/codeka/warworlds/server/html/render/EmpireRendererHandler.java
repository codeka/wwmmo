package au.com.codeka.warworlds.server.html.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;

import javax.imageio.ImageIO;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.handlers.RequestException;

/**
 * {@link RendererHandler} for rendering empire shields.
 */
public class EmpireRendererHandler extends RendererHandler {
  private static final Log log = new Log("EmpireRendererHandler");

  @Override
  protected void get() throws RequestException {

    long empireId = Long.parseLong(getUrlParameter("empire"));
    int width = Integer.parseInt(getUrlParameter("width"));
    int height = Integer.parseInt(getUrlParameter("height"));
    String bucket = getUrlParameter("bucket");
    Float factor = BUCKET_FACTORS.get(bucket);
    if (factor == null) {
      log.warning("Invalid bucket: %s", getRequest().getPathInfo());
      getResponse().setStatus(404);
      return;
    }

    File cacheFile = new File(String.format(Locale.ENGLISH,
        "data/cache/empire/%d/%dx%d/%s.png", empireId, width, height, bucket));
    if (cacheFile.exists()) {
      serveCachedFile(cacheFile);
      return;
    } else {
      cacheFile.getParentFile().mkdirs();
    }
    width = (int) (width * factor);
    height = (int) (height * factor);

    // TODO: if they have a custom one, use that
    //WatchableObject<Empire> empire = EmpireManager.i.getEmpire(empireId);

    BufferedImage shieldImage = new BufferedImage(128, 128, ColorSpace.TYPE_RGB);
    Graphics2D g = shieldImage.createGraphics();
    g.setPaint(getShieldColour(empireId));
    g.fillRect(0, 0, 128, 128);

    // Merge the shield image with the outline image.
    try {
      shieldImage = mergeShieldImage(shieldImage);
    } catch (IOException e) {
      throw new RequestException(e);
    }

    // Resize the image if required.
    if (width != 128 || height != 128) {
      int w = shieldImage.getWidth();
      int h = shieldImage.getHeight();
      BufferedImage after = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      AffineTransform at = new AffineTransform();
      at.scale((float) width / w, (float) height / h);
      AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
      shieldImage = scaleOp.filter(shieldImage, after);
    }

    try {
      ImageIO.write(shieldImage, "png", cacheFile);
    } catch (IOException e) {
      throw new RequestException(e);
    }

    serveCachedFile(cacheFile);
  }

  private Color getShieldColour(long empireID) {
    if (empireID == 0) {
      return new Color(Color.TRANSLUCENT);
    }

    Random rand = new Random(empireID ^ 7438274364563846L);
    return new Color(rand.nextInt(100) + 100,
        rand.nextInt(100) + 100,
        rand.nextInt(100) + 100);
  }

  private BufferedImage mergeShieldImage(BufferedImage shieldImage) throws IOException {
    BufferedImage finalImage = ImageIO.read(new File("data/renderer/empire/shield.png"));
    int width = finalImage.getWidth();
    int height = finalImage.getHeight();

    float fx = (float) shieldImage.getWidth() / (float) width;
    float fy = (float) shieldImage.getHeight() / (float) height;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int pixel = finalImage.getRGB(x, y);
        if ((pixel & 0xffffff) == 0xff00ff) {
          pixel = shieldImage.getRGB((int) (x * fx), (int) (y * fy));
          finalImage.setRGB(x, y, pixel);
        }
      }
    }

    return finalImage;
  }

}
