package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.PurchaseController;
import au.com.codeka.warworlds.server.model.Empire;

/**
 * This handler handles the empires/[key]/shield request when users update their shield image.
 */
public class EmpiresShieldHandler extends RequestHandler {
    @Override
    protected void put() throws RequestException {
        Messages.EmpireChangeShieldRequest shield_request_pb = getRequestBody(Messages.EmpireChangeShieldRequest.class);
        int empireID = getSession().getEmpireID();
        if (!Integer.toString(empireID).equals(shield_request_pb.getKey())) {
            throw new RequestException(403, "Cannot change someone else's shield image.");
        }

        new PurchaseController().addPurchase(empireID, shield_request_pb.getPurchaseInfo(), shield_request_pb);
        Empire empire = new EmpireController().getEmpire(empireID);
        //empire.setShieldImage(shield_request_pb.getNewName().trim());
        new EmpireController().update(empire);

        Messages.Empire.Builder empire_pb = Messages.Empire.newBuilder();
        empire.toProtocolBuffer(empire_pb);
        setResponseBody(empire_pb.build());
    }

}
