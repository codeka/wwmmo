package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.ctrl.WormholeController;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Star;

/** Handler class that destroys a wormhole. */
public class WormholeDestroyHandler extends RequestHandler {
  @Override
  protected void post() throws RequestException {
    int starID = Integer.parseInt(getUrlParameter("starid"));
    Star wormhole = new StarController().getStar(starID);
    int myEmpireID = getSession().getEmpireID();
    if (!new WormholeController().isInRangeOfWormholeDistruptor(myEmpireID, wormhole)) {
      throw new RequestException(400, Messages.GenericError.ErrorCode.NoWormholeDisruptorInRange,
          "You don't have any wormhole disruptors in range of this wormhole.");
    }

    Empire empire = new EmpireController().getEmpire(getSession().getEmpireID());
    new WormholeController().destroyWormhole(empire, wormhole);
  }
}
