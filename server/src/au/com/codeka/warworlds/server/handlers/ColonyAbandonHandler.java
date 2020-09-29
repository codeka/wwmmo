package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.ColonyController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Star;

// When you abandon a colony, it turns back to native. You get nothing from this, but it's just
// a clean way to leave a planet behind. You could attack them after if you wanted to really empty
// the planet, but you don't get anything for that.
public class ColonyAbandonHandler extends RequestHandler {
  @Override
  protected void post() throws RequestException {
    int colonyID = Integer.parseInt(getUrlParameter("colonyid"));
    int starID = Integer.parseInt(getUrlParameter("starid"));

    try (Transaction t = DB.beginTransaction()) {
      Star star = new StarController(t).getStar(starID);

      Colony colony = null;
      for (BaseColony baseColony : star.getColonies()) {
        Colony thisColony = (Colony) baseColony;
        if (thisColony.getID() == colonyID) {
          colony = thisColony;
          break;
        }
      }
      if (colony == null) {
        throw new RequestException(404, "Cannot abandon, no colony found on this planet.")
            .withLogMessageOnly();
      }

      if (colony.getEmpireID() == null || colony.getEmpireID() != getSession().getEmpireID()) {
        throw new RequestException(400, "Cannot abandon a colony that does not belong to you.")
            .withLogMessageOnly();
      }

      Simulation sim = new Simulation();
      sim.simulate(star);

      new ColonyController(t).abandon(getSession().getEmpireID(), star, colony);
      new StarController(t).update(star);
      t.commit();

      Messages.Star.Builder star_pb = Messages.Star.newBuilder();
      star.toProtocolBuffer(star_pb);
      setResponseBody(star_pb.build());
    } catch (Throwable e) {
      throw new RequestException(e);
    }
  }

  @Override
  protected boolean supportsRetryOnDeadlock() {
    return true;
  }
}
