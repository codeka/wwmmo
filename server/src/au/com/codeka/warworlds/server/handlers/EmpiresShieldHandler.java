package au.com.codeka.warworlds.server.handlers;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.imaging.ImageFormat;
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

    @Override
    protected void get() throws RequestException {
        int empireID = Integer.parseInt(getUrlParameter("empire_id"));

        byte[] pngImage = new EmpireController().getEmpireShield(empireID);
        if (pngImage == null) {
            throw new RequestException(404);
        }

        getResponse().setContentType("image/png");
        try {
            getResponse().getOutputStream().write(pngImage);
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }
}
