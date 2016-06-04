package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.Simulation;
import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.world.StarManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * Handler for simulating a single star via /admin/ajax/simulate.
 */
public class AjaxSimulateHandler extends AjaxHandler {
  @Override
  public void post() throws RequestException {
    SimulateResponse resp = new SimulateResponse();
    Long id = Long.parseLong (getRequest().getParameter("id"));
    long startTime = System.nanoTime();
    WatchableObject<Star> star = StarManager.i.getStar(id);
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
    resp.simulateTime = (System.nanoTime() - startTime) / 1000000L;
    star.set(starBuilder.build());
    resp.saveTime = (System.nanoTime() - startTime) / 1000000L;
    resp.logMessages = logMessages.toString();

    setResponseGson(resp);
  }

  static class SimulateResponse {
    public long loadTime;
    public long simulateTime;
    public long saveTime;
    public String logMessages;
  }
}
