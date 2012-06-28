package au.com.codeka.warworlds.testplanetrender;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import au.com.codeka.planetrender.Colour;
import au.com.codeka.planetrender.Image;

class ImagePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private java.awt.Image mImage;
    private int mImageWidth;
    private int mImageHeight;
    private Colour mBackgroundColour;

    public void setImage(Image img) {
        MemoryImageSource mis = new MemoryImageSource(img.getWidth(), img.getHeight(),
                                                      img.getArgb(), 0, img.getWidth());

        mImage = getTopLevelAncestor().createImage(mis);
        mImageWidth = img.getWidth();
        mImageHeight = img.getHeight();
        repaint();
    }

    public void saveImageAs(File f) throws IOException {
        BufferedImage img = new BufferedImage(mImageWidth, mImageHeight,
                                              BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.drawImage(mImage, 0, 0, this);
        g.dispose();

        ImageIO.write(img, "png", f);
    }

    public void setBackgroundColour(Colour c) {
        mBackgroundColour = c;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();

        // fill the entire background gray
        Color bg = Color.LIGHT_GRAY;
        if (mBackgroundColour != null) {
            bg = new Color((float) mBackgroundColour.r, (float)  mBackgroundColour.g,
                           (float) mBackgroundColour.b, (float) mBackgroundColour.a);
        }
        g.setColor(bg);
        g.fillRect(0, 0, width, height);

        if (mImage != null) {
            int sx = (width - mImageWidth) / 2;
            int sy = (height - mImageHeight) / 2;
            width = mImageWidth;
            height = mImageHeight;

            // if the background is set to transparent, we'll draw a checkboard background
            //to represent the transparent parts of the image
            if (mBackgroundColour == null) {
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

            g.drawImage(mImage, sx, sy, null);
        }
    }
}
