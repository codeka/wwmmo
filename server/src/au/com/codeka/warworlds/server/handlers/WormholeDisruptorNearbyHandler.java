package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.ctrl.WormholeController;
import au.com.codeka.warworlds.server.model.Star;

/** Handler that determines whether there is a wormhole disruptor near the given wormhole. */
public class WormholeDisruptorNearbyHandler extends RequestHandler {
  @Override
  protected void get() throws RequestException {
    int starID = Integer.parseInt(getUrlParameter("starid"));
    Star wormhole = new StarController().getStar(starID);
    int myEmpireID = getSession().getEmpireID();
    if (!new WormholeController().isInRangeOfWormholeDistruptor(myEmpireID, wormhole)) {
      throw new RequestException(404);
    }

    // Just response with a blank 200, that's all we need.
  }
}
