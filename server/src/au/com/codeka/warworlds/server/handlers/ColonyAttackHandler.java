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

public class ColonyAttackHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        int colonyID = Integer.parseInt(getUrlParameter("colony_id"));
        int starID = Integer.parseInt(getUrlParameter("star_id"));

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
                throw new RequestException(404, Messages.GenericError.ErrorCode.CannotAttackColonyGone,
                        "No colony found on this planet.");
            }

            if (colony.getEmpireID() == getSession().getEmpireID()) {
                throw new RequestException(400, Messages.GenericError.ErrorCode.CannotAttackOwnColony,
                        "Cannot attack your own colony.");
            }

            Simulation sim = new Simulation();
            sim.simulate(star);

            new ColonyController(t).attack(getSession().getEmpireID(), star, colony);
            new StarController(t).update(star);
            t.commit();

            Messages.Star.Builder star_pb = Messages.Star.newBuilder();
            star.toProtocolBuffer(star_pb);
            setResponseBody(star_pb.build());
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    @Override
    protected boolean supportsRetryOnDeadlock() {
        return true;
    }
}
