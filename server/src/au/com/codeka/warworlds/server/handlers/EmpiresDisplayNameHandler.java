package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.EmpireRenameRequest;
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
        EmpireRenameRequest rename_request_pb = getRequestBody(EmpireRenameRequest.class);
        int empireID = getSession().getEmpireID();
        if (!Integer.toString(empireID).equals(rename_request_pb.key)) {
            throw new RequestException(403, "Cannot rename someone else's empire.");
        }

        Empire empire = new EmpireController().getEmpire(empireID);
        rename_request_pb = new EmpireRenameRequest.Builder(rename_request_pb)
                .old_name(empire.getDisplayName())
                .build();

        new PurchaseController().addPurchase(empireID, rename_request_pb.purchase_info, rename_request_pb);
        empire.setName(rename_request_pb.new_name.trim());
        new EmpireController().update(empire);

        au.com.codeka.common.protobuf.Empire empire_pb = new au.com.codeka.common.protobuf.Empire();
        empire.toProtocolBuffer(empire_pb, true);
        setResponseBody(empire_pb);
    }
}
