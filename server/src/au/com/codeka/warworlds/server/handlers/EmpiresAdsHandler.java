package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.EmpireAdsRemoveRequest;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.PurchaseController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

/**
 * This handler handles the empires/[key]/ads request to remove ads.
 */
public class EmpiresAdsHandler extends RequestHandler {
    @Override
    protected void put() throws RequestException {
        EmpireAdsRemoveRequest remove_ads_request_pb = getRequestBody(EmpireAdsRemoveRequest.class);
        int empireID = getSession().getEmpireID();

        new PurchaseController().addPurchase(empireID, remove_ads_request_pb.purchase_info, remove_ads_request_pb);

        String sql = "UPDATE empires SET remove_ads=1 WHERE id=?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, empireID);
            stmt.update();
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }
}
