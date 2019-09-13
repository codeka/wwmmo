package au.com.codeka.warworlds.server.handlers.admin;

import java.util.ArrayList;
import java.util.TreeMap;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.events.FleetMoveCompleteEvent;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

public class AdminMovingFleetsHandler extends AdminHandler {
  @Override
  protected void get() throws RequestException {
    if (!isAdmin()) {
      return;
    }
    TreeMap<String, Object> data = new TreeMap<>();

    String sql = "SELECT fleets.*, empires.alliance_id" +
        " FROM fleets" +
        " LEFT OUTER JOIN empires ON empires.id = fleets.empire_id" +
        " WHERE target_star_id IS NOT NULL";

    ArrayList<Fleet> fleets = new ArrayList<>();
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();

      while (res.next()) {
        Fleet fleet = new Fleet(res);
        fleets.add(fleet);
      }
    } catch (Exception e) {
      throw new RequestException(e);
    }
    data.put("fleets", fleets);

    render("admin/debug/moving-fleets.html", data);
  }

  @Override
  protected void post() throws RequestException {
    if (!isAdmin()) {
      return;
    }

    String action = getRequest().getParameter("action");
    if (action.equals("complete-movement")) {
      int fleetID = Integer.parseInt(getRequest().getParameter("fleet_id"));

      Fleet fleet = fetchFleet(fleetID);
      if (fleet == null) {
        write("Unknown fleet: " + fleetID);
        return;
      }

      Star srcStar = new StarController().getStar(fleet.getStarID());
      if (fleet.getDestinationStarID() == null) {
        write("Fleet is not moving.");
        return;
      }

      Star destStar = new StarController().getStar(fleet.getDestinationStarID());

      FleetMoveCompleteEvent.processFleet(fleetID, srcStar, destStar, true);
      write("OK");
    } else {
      write("Invalid action: " + action);
    }
  }

  @Nullable
  private Fleet fetchFleet(int fleetID) throws RequestException {
    String sql = "SELECT fleets.*, empires.alliance_id" +
        " FROM fleets" +
        " LEFT OUTER JOIN empires ON empires.id = fleets.empire_id" +
        " WHERE fleets.id = " + fleetID;
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      if (res.next()) {
        return new Fleet(res);
      }
      return null;
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }
}
