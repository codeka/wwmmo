package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.PurchaseController;
import au.com.codeka.warworlds.server.model.Empire;

/**
 * This handler handles the empires/[key]/display-name request when users change their empire name.
 */
public class EmpiresDisplayNameHandler extends RequestHandler {
    @Override
    protected void put() throws RequestException {
        Messages.EmpireRenameRequest rename_request_pb = getRequestBody(Messages.EmpireRenameRequest.class);
        int empireID = getSession().getEmpireID();
        if (!Integer.toString(empireID).equals(rename_request_pb.getKey())) {
            throw new RequestException(403, "Cannot rename someone else's empire.");
        }

        Empire empire = new EmpireController().getEmpire(empireID);
        rename_request_pb = Messages.EmpireRenameRequest.newBuilder(rename_request_pb)
                .setOldName(empire.getDisplayName())
                .build();

        new PurchaseController().addPurchase(empireID, rename_request_pb.getPurchaseInfo(), rename_request_pb);
        empire.setName(rename_request_pb.getNewName().trim());
        new EmpireController().update(empire);

        Messages.Empire.Builder empire_pb = Messages.Empire.newBuilder();
        empire.toProtocolBuffer(empire_pb);
        setResponseBody(empire_pb.build());
    }

}
