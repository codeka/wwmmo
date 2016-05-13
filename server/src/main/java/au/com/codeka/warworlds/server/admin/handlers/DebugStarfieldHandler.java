package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.world.SectorManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * This handler is for /admin/debug/starfield, and allows us to explore the starfield itself.
 */
public class DebugStarfieldHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    render("debug/starfield.html", null);
  }

  @Override
  public void post() throws RequestException {
    switch (getRequest().getParameter("action")) {
      case "xy":
        long x = Long.parseLong(getRequest().getParameter("x"));
        long y = Long.parseLong(getRequest().getParameter("y"));
        handleXyRequest(x, y);
        break;
      default:
        throw new RequestException(400, "Unknown action: " + getRequest().getParameter("action"));
    }
  }

  private void handleXyRequest(long x, long y) {
    WatchableObject<Sector> sector =
        SectorManager.i.getSector(new SectorCoord.Builder().x(x).y(y).build());
    setResponseJson(sector.get());
  }
}
