package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;

import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.ctrl.AllianceController;
import au.com.codeka.warworlds.server.ctrl.BuildingController;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.PurchaseController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.BuildingPosition;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Star;
import au.com.codeka.warworlds.server.monitor.EmpireIpAddressMonitor;
import au.com.codeka.warworlds.server.utils.NameValidator;

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

    ArrayList<BuildingPosition> buildings = null;
    // Note: anybody can call this, even if not logged in (for example, the notification handle
    // might call this from the background when the app isn't running).
    Session session = getSessionNoError();
    if (session != null) {
      int myEmpireID = session.getEmpireID();
      buildings = new BuildingController()
          .getBuildings(myEmpireID, star.getSectorX() - 1, star.getSectorY() - 1,
              star.getSectorX() + 1, star.getSectorY() + 1);
    }

    if (session == null || !session.isAdmin()) {
      new StarController().sanitizeStar(
          star, session == null ? 0 : session.getEmpireID(), buildings, null);
    }

    Messages.Star.Builder star_pb = Messages.Star.newBuilder();
    star.toProtocolBuffer(star_pb);
    setResponseBody(star_pb.build());
  }

  @Override
  protected void put() throws RequestException {
    Messages.StarRenameRequest star_rename_request_pb =
        getRequestBody(Messages.StarRenameRequest.class);
    if (!star_rename_request_pb.getStarKey().equals(getUrlParameter("starid"))) {
      throw new RequestException(404);
    }
    int starID = Integer.parseInt(getUrlParameter("starid"));

    if (star_rename_request_pb.getNewName().trim().equals("")) {
      throw new RequestException(400);
    }

    String newName = NameValidator.validate(
        star_rename_request_pb.getNewName(),
        Configuration.i.getLimits().maxStarNameLength());

    Empire empire = new EmpireController().getEmpire(getSession().getEmpireID());
    if (!star_rename_request_pb.hasPurchaseInfo()) {
      // if there's no purchase info then you must be either be an empire-level patron, or you're
      // renaming a wormhole that belongs to your alliance.
      Star star = new StarController().getStar(starID);
      if (star.getWormholeExtra() == null) {
        // it's not a wormhole, so you must be an empire-level patron to rename for free.
        if (empire.getPatreonLevel() != BaseEmpire.PatreonLevel.EMPIRE) {
          throw new RequestException(400, "You are not allowed to rename this star.");
        }
      } else {
        Star.WormholeExtra wormhole = star.getWormholeExtra();
        if (!new AllianceController()
            .isSameAlliance(wormhole.getEmpireID(), getSession().getEmpireID())) {
          throw new RequestException(400, "You cannot rename wormholes that do not belong to you.");
        }
      }
    }

    String sql = "UPDATE stars SET name = ? WHERE id = ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setString(1, newName);
      stmt.setInt(2, starID);
      stmt.update();
    } catch (Exception e) {
      throw new RequestException(e);
    }

    if (empire.getPatreonLevel() != BaseEmpire.PatreonLevel.EMPIRE
        && star_rename_request_pb.hasPurchaseInfo()) {
      new PurchaseController()
          .addPurchase(getSession().getEmpireID(), star_rename_request_pb.getPurchaseInfo(),
              star_rename_request_pb);
    }

    Star star = new StarController().getStar(starID);
    Messages.Star.Builder star_pb = Messages.Star.newBuilder();
    star.toProtocolBuffer(star_pb);
    setResponseBody(star_pb.build());
  }
}
