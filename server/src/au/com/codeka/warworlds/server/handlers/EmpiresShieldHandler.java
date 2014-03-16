package au.com.codeka.warworlds.server.handlers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.common.protobuf.Messages.GenericError;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.PurchaseController;
import au.com.codeka.warworlds.server.model.Empire;

/**
 * This handler handles the empires/[key]/shield request when users update their shield image.
 */
public class EmpiresShieldHandler extends RequestHandler {
    private static final Logger log = LoggerFactory.getLogger(EmpiresShieldHandler.class);

    @Override
    protected void get() throws RequestException {
        int empireID = Integer.parseInt(getUrlParameter("empire_id"));

        Integer shieldID = null;
        if (getRequest().getParameter("id") != null) {
            shieldID = Integer.parseInt(getRequest().getParameter("id"));
        }

        byte[] pngImage = new EmpireController().getEmpireShield(empireID, shieldID);
        if (pngImage == null) {
            if (getRequest().getParameter("final") != null && getRequest().getParameter("final").equals("1")) {
                try {
                    // if we're doing a "final" image for this guy, just create a coloured image based on his key
                    BufferedImage shieldImage = new BufferedImage(128, 128, ColorSpace.TYPE_RGB);
                    Graphics2D g = shieldImage.createGraphics();
                    g.setPaint(getShieldColour(empireID));
                    g.fillRect(0, 0, 128, 128);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(shieldImage, "png", baos);
                    pngImage = baos.toByteArray();
                } catch (Exception e) {
                    throw new RequestException(e);
                }
            } else {
                throw new RequestException(404);
            }
        }

        if (getRequest().getParameter("final") != null && getRequest().getParameter("final").equals("1")) {
            try {
                BufferedImage shieldImage = Imaging.getBufferedImage(pngImage);
                shieldImage = mergeShieldImage(shieldImage);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(shieldImage, "png", baos);
                pngImage = baos.toByteArray();
            } catch (Exception e) {
                throw new RequestException(e);
            }
        }

        if (getRequest().getParameter("size") != null) {
            int size = Integer.parseInt(getRequest().getParameter("size"));
            if (size > 1 && size < 128) {
                try {
                    BufferedImage shieldImage = Imaging.getBufferedImage(pngImage);

                    int w = shieldImage.getWidth();
                    int h = shieldImage.getHeight();
                    BufferedImage after = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                    AffineTransform at = new AffineTransform();
                    at.scale((float) size / w, (float) size / h);
                    AffineTransformOp scaleOp = 
                       new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
                    shieldImage = scaleOp.filter(shieldImage, after);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(shieldImage, "png", baos);
                    pngImage = baos.toByteArray();
                } catch(Exception e) {
                    throw new RequestException(e);
                }
            }
        }

        getResponse().setContentType("image/png");
        try {
            getResponse().getOutputStream().write(pngImage);
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }

    @Override
    protected void put() throws RequestException {
        Messages.EmpireChangeShieldRequest shield_request_pb = getRequestBody(Messages.EmpireChangeShieldRequest.class);
        int empireID = getSession().getEmpireID();
        if (!Integer.toString(empireID).equals(shield_request_pb.getKey())) {
            throw new RequestException(403, "Cannot change someone else's shield image.");
        }

        // load up the image, make sure it's valid and reasonable dimenions
        BufferedImage img;
        try {
            img = Imaging.getBufferedImage(shield_request_pb.getPngImage().toByteArray());
        } catch (Exception e) {
            log.error("Exception caught loading image, assuming invalid!", e);
            throw new RequestException(400, GenericError.ErrorCode.InvalidImage, "Supplied image is not valid.");
        }
        if (img.getWidth() > 128 || img.getHeight() > 128) {
            // if it's bigger than 128x128, we'll resize it here so that we don't ever store images
            // that are too big to actually display.
            BufferedImage after = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            AffineTransform at = new AffineTransform();
            at.scale(128.0 / img.getWidth(), 128.0 / img.getHeight());
            AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            after = scaleOp.filter(img, after);
        }

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            Imaging.writeImage(img, png, ImageFormat.IMAGE_FORMAT_PNG, null);
        } catch(Exception e) {
            throw new RequestException(e);
        }

        new PurchaseController().addPurchase(empireID, shield_request_pb.getPurchaseInfo(), shield_request_pb);
        new EmpireController().changeEmpireShield(empireID, png.toByteArray());

        Messages.Empire.Builder empire_pb = Messages.Empire.newBuilder();
        Empire empire = new EmpireController().getEmpire(empireID);
        empire.toProtocolBuffer(empire_pb);
        setResponseBody(empire_pb.build());
    }

    private BufferedImage mergeShieldImage(BufferedImage shieldImage) throws ImageReadException, IOException {
        BufferedImage finalImage = Imaging.getBufferedImage(new File(getBasePath(), "data/static/img/shield.png"));
        int width = finalImage.getWidth();
        int height = finalImage.getHeight();
        int[] pixels = (int[]) finalImage.getRaster().getDataElements(0, 0, width, height, null);

        float fx = (float) shieldImage.getWidth() / (float) width;
        float fy = (float) shieldImage.getHeight() / (float) height;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = ((y * width) + x);
                if ((pixels[offset] & 0xffffff) == 0xff00ff) {
                    pixels[offset] = shieldImage.getRGB((int) (x * fx), (int) (y * fy));
                }
            }
        }

        int[] bitMasks = new int[]{0xFF0000, 0xFF00, 0xFF, 0xFF000000};
        SinglePixelPackedSampleModel sm = new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_INT, width, height, bitMasks);
        DataBufferInt db = new DataBufferInt(pixels, pixels.length);
        WritableRaster wr = Raster.createWritableRaster(sm, db, new Point());
        return new BufferedImage(ColorModel.getRGBdefault(), wr, false, null);
    }

    public Color getShieldColour(int empireID) {
        Random rand = new Random(Integer.toString(empireID).hashCode());
        return new Color(rand.nextInt(100) + 100,
                         rand.nextInt(100) + 100,
                         rand.nextInt(100) + 100);
    }
}
