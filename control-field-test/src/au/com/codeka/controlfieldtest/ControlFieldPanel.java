package au.com.codeka.controlfieldtest;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import au.com.codeka.common.Colour;
import au.com.codeka.common.Image;
import au.com.codeka.common.PointCloud;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.Voronoi;
import au.com.codeka.controlfield.ControlField;

/**
 * A special panel that renders the control field and underlying PointCloud.
 */
public class ControlFieldPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private java.awt.Image mImage;
    private int mImageWidth;
    private int mImageHeight;
    private Colour mBackgroundColour;
    private PointCloud mPointCloud;
    private Voronoi mVoronoi;
    private ControlField mControlField;

    public ControlFieldPanel() {
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evnt) {
                if (mVoronoi == null || mImage == null) {
                    return;
                }

                int mx = evnt.getPoint().x - ControlFieldPanel.this.getWidth() / 2;
                int my = evnt.getPoint().y - ControlFieldPanel.this.getHeight() / 2;

                double u = (double) mx / (mImageWidth / 2.0);
                double v = (double) my / (mImageHeight / 2.0);

                u = 0.5 + (u / 2.0);
                if (u < 0.0 || u > 1.0) {
                    return;
                }

                v = 0.5 + (v / 2.0);
                if (v < 0.0 || v > 1.0) {
                    return;
                }

                Vector2 pt = mVoronoi.findClosestPoint(Vector2.pool.borrow().reset(u, v));
                mControlField.addPointToControlField(pt);
                render();
            }
        });

        mBackgroundColour = Colour.BLACK;
    }

    public void setPointCloud(PointCloud pointCloud) {
        mPointCloud = pointCloud;
    }

    public void setVoronoi(Voronoi voronoi) {
        mVoronoi = voronoi;
    }

    public void setControlField(ControlField controlField) {
        mControlField = controlField;
    }

    public void render() {
        Image img = new Image(512, 512, Colour.TRANSPARENT);
        if (mControlField != null) {
            mControlField.render(img, Colour.GREEN);
        }
        if (mPointCloud != null) {
            mPointCloud.render(img);
        }
        if (mVoronoi != null) {
            //mVoronoi.renderVoronoi(img, Colour.BLUE);
            //mVoronoi.renderDelaunay(img, Colour.BLUE);
        }

        setImage(img);
    }

    private void setImage(Image img) {
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
