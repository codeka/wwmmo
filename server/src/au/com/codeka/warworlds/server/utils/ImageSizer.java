package au.com.codeka.warworlds.server.utils;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

/**
 * A helper class we can use to ensure images are never greater than some maximum width/height.
 */
public class ImageSizer {
    public static byte[] ensureMaxSize(byte[] pngImage, int maxWidth, int maxHeight) {
        try {
            BufferedImage image = Imaging.getBufferedImage(pngImage);
            image = ensureMaxSize(image, maxWidth, maxHeight);
    
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException | ImageReadException e) {
            return pngImage; // no ideal, but shouldn't happen
        }
    }

    public static BufferedImage ensureMaxSize(BufferedImage img, int maxWidth, int maxHeight) {
        // make sure maxWidth/maxHeight is the same aspect ratio as the input image
        int width = img.getWidth();
        int height = img.getHeight();
        float aspect = (float) width / (float) height;
        if (aspect > 1.0f) {
            maxHeight = (int) (maxWidth / aspect);
        } else {
            maxWidth = (int) (maxHeight * aspect);
        }

        if (maxWidth > width || maxHeight > height) {
            return img;
        }

        BufferedImage after = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = new AffineTransform();
        at.scale((float) maxWidth / width, (float) maxHeight / height);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
        return scaleOp.filter(img, after);
    }
}
