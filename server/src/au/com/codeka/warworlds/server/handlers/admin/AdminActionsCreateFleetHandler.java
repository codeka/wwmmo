package au.com.codeka.warworlds.server.handlers.admin;

import java.util.TreeMap;

import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.FleetController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.events.FleetMoveCompleteEvent;
import au.com.codeka.warworlds.server.model.DesignManager;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

public class AdminActionsCreateFleetHandler extends AdminGenericHandler {
  @Override
  protected void post() throws RequestException {
    Star star = new StarController().getStar(Integer.parseInt(getRequest().getParameter("star")));
    Empire empire =
        new EmpireController().getEmpire(Integer.parseInt(getRequest().getParameter("empire")));
    String designName = getRequest().getParameter("designName");
    int numShips = Integer.parseInt(getRequest().getParameter("numShips"));

    DesignManager.i.getDesign(DesignKind.SHIP, designName);
    Fleet fleet = new FleetController().createFleet(empire, star, designName, numShips);
    FleetMoveCompleteEvent.fireFleetArrivedEvents(star, fleet);

    new Simulation().simulate(star);
    new StarController().update(star);

    render("admin/actions/create-fleet.html", new TreeMap<>());
  }
}
