package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.GenericError;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.ctrl.WormholeController;
import au.com.codeka.warworlds.server.model.Star;

/** Handler class that lets you take over a wormhole. */
public class WormholeTakeOverHandler extends RequestHandler {
  @Override
  protected void post() throws RequestException {
    int starID = Integer.parseInt(getUrlParameter("starid"));
    Star wormhole = new StarController().getStar(starID);
    int myEmpireID = getSession().getEmpireID();
    if (!new WormholeController().isInRangeOfWormholeDistruptor(myEmpireID, wormhole)) {
      throw new RequestException(400, GenericError.ErrorCode.NoWormholeDisruptorInRange,
          "You don't have any wormhole disruptors in range of this wormhole.");
    }

    new WormholeController().takeOverWormhole(myEmpireID, wormhole);
  }
}
