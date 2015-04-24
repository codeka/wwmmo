package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;

import au.com.codeka.common.protobuf.StarRenameRequest;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.AllianceController;
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
    int id = Integer.parseInt(getUrlParameter("starid"));
    Star star = new StarController().getStar(id);
    if (star == null) {
      throw new RequestException(404);
    }

    int myEmpireID = getSession().getEmpireID();
    ArrayList<BuildingPosition> buildings = new BuildingController()
        .getBuildings(myEmpireID, star.getSectorX() - 1, star.getSectorY() - 1,
            star.getSectorX() + 1, star.getSectorY() + 1);

    if (!isAdmin()) {
      new StarController().sanitizeStar(star, myEmpireID, buildings, null);
    }

    setCacheTime(24, star.getLastSimulation().toString());

    au.com.codeka.common.protobuf.Star star_pb = new au.com.codeka.common.protobuf.Star();
    star.toProtocolBuffer(star_pb);
    setResponseBody(star_pb);
  }

  @Override
  protected void put() throws RequestException {
    StarRenameRequest star_rename_request_pb = getRequestBody(StarRenameRequest.class);
    if (!star_rename_request_pb.star_key.equals(getUrlParameter("starid"))) {
      throw new RequestException(404);
    }
    int starID = Integer.parseInt(getUrlParameter("starid"));

    if (star_rename_request_pb.new_name.trim().equals("")) {
      throw new RequestException(400);
    }

    if (star_rename_request_pb.purchase_info == null) {
      // if there's no purchase info then you must be renaming a wormhole, and it must be
      // one belonging to your alliance.
      Star star = new StarController().getStar(starID);
      if (star.getWormholeExtra() == null) {
        throw new RequestException(400, "You are you allowed to rename this star.");
      }

      Star.WormholeExtra wormhole = star.getWormholeExtra();
      if (!new AllianceController()
          .isSameAlliance(wormhole.getEmpireID(), getSession().getEmpireID())) {
        throw new RequestException(400, "You cannot rename wormholes that do not belong to you.");
      }
    }

    String sql = "UPDATE stars SET name = ? WHERE id = ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setString(1, star_rename_request_pb.new_name.trim());
      stmt.setInt(2, starID);
      stmt.update();
    } catch (Exception e) {
      throw new RequestException(e);
    }

    if (star_rename_request_pb.purchase_info != null) {
      new PurchaseController()
          .addPurchase(getSession().getEmpireID(), star_rename_request_pb.purchase_info,
              star_rename_request_pb);
    }

    Star star = new StarController().getStar(starID);
    au.com.codeka.common.protobuf.Star star_pb = new au.com.codeka.common.protobuf.Star();
    star.toProtocolBuffer(star_pb);
    setResponseBody(star_pb);
  }
}
