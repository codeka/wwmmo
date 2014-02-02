package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.BuildingController;
import au.com.codeka.warworlds.server.ctrl.PurchaseController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.BuildingPosition;
import au.com.codeka.warworlds.server.model.Star;

/**
 * Handles /realm/.../stars/{id} URL
 */
public class StarHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int id = Integer.parseInt(getUrlParameter("star_id"));
        Star star = new StarController().getStar(id);
        if (star == null) {
            throw new RequestException(404);
        }

        int myEmpireID = getSession().getEmpireID();
        ArrayList<BuildingPosition> buildings = new BuildingController().getBuildings(
                myEmpireID, star.getSectorX() - 1, star.getSectorY() - 1,
                star.getSectorX() + 1, star.getSectorY() + 1);

        if (!isAdmin()) {
            new StarController().sanitizeStar(star, myEmpireID, buildings, null);
        }

        Messages.Star.Builder star_pb = Messages.Star.newBuilder();
        star.toProtocolBuffer(star_pb);
        setResponseBody(star_pb.build());
    }

    @Override
    protected void put() throws RequestException {
        Messages.StarRenameRequest star_rename_request_pb = getRequestBody(Messages.StarRenameRequest.class);
        if (!star_rename_request_pb.getStarKey().equals(getUrlParameter("star_id"))) {
            throw new RequestException(404);
        }
        int starID = Integer.parseInt(getUrlParameter("star_id"));

        if (star_rename_request_pb.getNewName().trim().equals("")) {
            throw new RequestException(400);
        }

        String sql = "UPDATE stars SET name = ? WHERE id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setString(1, star_rename_request_pb.getNewName().trim());
            stmt.setInt(2, starID);
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }

        new PurchaseController().addPurchase(getSession().getEmpireID(), star_rename_request_pb.getPurchaseInfo(),
                star_rename_request_pb);

        Star star = new StarController().getStar(starID);
        Messages.Star.Builder star_pb = Messages.Star.newBuilder();
        star.toProtocolBuffer(star_pb);
        setResponseBody(star_pb.build());
    }
}
