package au.com.codeka.warworlds.server.admin.handlers;

import java.util.ArrayList;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.sim.Simulation;
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.world.SectorManager;
import au.com.codeka.warworlds.server.world.StarManager;
import au.com.codeka.warworlds.server.world.SuspiciousEventManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/** Handler for /admin/ajax/starfield requests. */
public class AjaxStarfieldHandler extends AjaxHandler {
  private static final Log log = new Log("AjaxStarfieldHandler");

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
    long starId;

    switch (getRequest().getParameter("action")) {
      case "simulate":
        starId = Long.parseLong(getRequest().getParameter("id"));
        handleSimulateRequest(starId);
        break;
      case "modify":
        starId = Long.parseLong(getRequest().getParameter("id"));
        String modifyJson = getRequest().getParameter("modify");
        handleModifyRequest(starId, modifyJson);
        break;
      case "delete":
        starId = Long.parseLong(getRequest().getParameter("id"));
        handleDeleteRequest(starId);
        break;
      case "clearNatives":
        starId = Long.parseLong(getRequest().getParameter("id"));
        handleClearNativesRequest(starId);
        break;
      default:
        throw new RequestException(400, "Unknown action: " + getRequest().getParameter("action"));
    }
  }

  private void handleXyRequest(long x, long y) {
    WatchableObject<Sector> sector =
        SectorManager.i.getSector(new SectorCoord.Builder().x(x).y(y).build());
    SectorManager.i.verifyNativeColonies(sector);
    setResponseJson(sector.get());
  }

  private void handleSimulateRequest(long starId) throws RequestException {
    setResponseGson(modifyAndSimulate(starId, null));
  }

  private void handleModifyRequest(long starId, String modifyJson) throws RequestException {
    log.debug("modify: " + modifyJson);
    StarModification modification = fromJson(modifyJson, StarModification.class);
    setResponseGson(modifyAndSimulate(starId, modification));
  }

  private void handleDeleteRequest(long starId) {
    log.debug("delete star: %d", starId);
    StarManager.i.deleteStar(starId);
  }

  private void handleClearNativesRequest(long starId) throws RequestException {
    log.debug("delete star: %d", starId);
    modifyAndSimulate(starId, new StarModification.Builder()
        .type(StarModification.MODIFICATION_TYPE.EMPTY_NATIVE)
        .build());
  }

  private SimulateResponse modifyAndSimulate(long starId, @Nullable StarModification modification)
      throws RequestException {
    SimulateResponse resp = new SimulateResponse();
    new SimulateResponse();
    long startTime = System.nanoTime();
    WatchableObject<Star> star = StarManager.i.getStar(starId);
    resp.loadTime = (System.nanoTime() - startTime) / 1000000L;

    final StringBuilder logMessages = new StringBuilder();

    ArrayList<StarModification> modifications = new ArrayList<>();
    if (modification != null) {
      modifications.add(modification);
    }
    try {
      StarManager.i.modifyStar(star, modifications, new LogHandler(logMessages));
    } catch (SuspiciousModificationException e) {
      log.warning("Suspicious modification.", e);
      // We'll log it as well, even though technically it wasn't the empire who made it.
      SuspiciousEventManager.i.addSuspiciousEvent(e);
      throw new RequestException(e);
    }
    long simulateTime = System.nanoTime();
    resp.simulateTime = (simulateTime - startTime) / 1000000L;
    resp.logMessages = logMessages.toString();
    return resp;
  }

  private static class LogHandler implements Simulation.LogHandler {
    private final StringBuilder logMessages;

    LogHandler(StringBuilder logMessages) {
      this.logMessages = logMessages;
    }

    @Override
    public void setStarName(String starName) {
      // ignore.
    }

    @Override
    public void log(String message) {
      logMessages.append(message);
      logMessages.append("\n");
      log.debug(message);
    }
  }

  private static class SimulateResponse {
    long loadTime;
    long simulateTime;
    String logMessages;
  }
}
