package au.com.codeka.warworlds.server.handlers;

import java.util.List;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.Star;

/**
 * This handler handles the empires/[key]/reset request when users request an empire reset.
 */
public class EmpireResetHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        Messages.EmpireResetRequest reset_request_pb = getRequestBody(Messages.EmpireResetRequest.class);
        int empireID = getSession().getEmpireID();

        // make sure they've purchased the right sku for the number of stars they have
        int[] starIDs = new EmpireController().getStarsForEmpire(empireID);
        List<Star> stars = new StarController().getStars(starIDs);
        int numStarsWithColonies = 0;
        for (Star star : stars) {
            for (BaseColony colony : star.getColonies()) {
                if (colony.getEmpireKey() != null && Integer.parseInt(colony.getEmpireKey()) == empireID) {
                    numStarsWithColonies ++;
                    break;
                }
            }
        }

        if (numStarsWithColonies >= 5 && (!reset_request_pb.hasPurchaseInfo() || !reset_request_pb.getPurchaseInfo().hasSku())) {
            throw new RequestException(400, "You did not purchase the right SKU.");
        }
        if (numStarsWithColonies >= 10) {
            if (!reset_request_pb.getPurchaseInfo().getSku().equals("reset_empire_big")) {
                throw new RequestException(400, "You did not purchase the right SKU.");
            }
        } else if (numStarsWithColonies >= 5) {
            if (!reset_request_pb.getPurchaseInfo().getSku().equals("reset_empire_small")) {
                throw new RequestException(400, "You did not purchase the right SKU.");
            }
        }

        new EmpireController().resetEmpire(empireID, "as-requested");
    }
}
