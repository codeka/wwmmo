package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.Simulation;
import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.world.SectorManager;
import au.com.codeka.warworlds.server.world.StarManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/** Handler for /admin/ajax/starfield requests. */
public class AjaxStarfieldHandler extends AjaxHandler {
  @Override
  public void get() throws RequestException {
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

  @Override
  public void post() throws RequestException {
    switch (getRequest().getParameter("action")) {
      case "simulate":
        long starId = Long.parseLong(getRequest().getParameter("id"));
        handleSimulateRequest(starId);
    }
  }

  private void handleXyRequest(long x, long y) {
    WatchableObject<Sector> sector =
        SectorManager.i.getSector(new SectorCoord.Builder().x(x).y(y).build());
    SectorManager.i.verifyNativeColonies(sector);
    setResponseJson(sector.get());
  }

  private void handleSimulateRequest(long starId) {
    SimulateResponse resp = new SimulateResponse();
    long startTime = System.nanoTime();
    WatchableObject<Star> star = StarManager.i.getStar(starId);
    resp.loadTime = (System.nanoTime() - startTime) / 1000000L;
    Star.Builder starBuilder = star.get().newBuilder();
    final StringBuilder logMessages = new StringBuilder();
    new Simulation(new Simulation.LogHandler() {
      @Override
      public void setStarName(String starName) {
        // ignore.
      }

      @Override
      public void log(String message) {
        logMessages.append(message);
        logMessages.append("\n");
      }
    }).simulate(starBuilder);
    long simulateTime = System.nanoTime();
    resp.simulateTime = (simulateTime - startTime) / 1000000L;
    StarManager.i.completeActions(star, starBuilder);
    resp.saveTime = (System.nanoTime() - simulateTime) / 1000000L;
    resp.logMessages = logMessages.toString();

    setResponseGson(resp);
  }

  private static class SimulateResponse {
    long loadTime;
    long simulateTime;
    long saveTime;
    String logMessages;
  }
}
