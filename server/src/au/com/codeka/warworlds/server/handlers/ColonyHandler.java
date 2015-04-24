package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Star;

public class ColonyHandler extends RequestHandler {

    @Override
    protected void put() throws RequestException {
        int starID = Integer.parseInt(getUrlParameter("starid"));
        int colonyID = Integer.parseInt(getUrlParameter("colonyid"));
        Simulation sim = new Simulation();

        Session session = getSession();

        Star star = new StarController().getStar(starID);
        Colony colony = null;
        for (BaseColony baseColony : star.getColonies()) {
            if (((Colony) baseColony).getID() == colonyID) {
                if (((Colony) baseColony).getEmpireID() != session.getEmpireID()) {
                    // if the colony isn't own by this user's empire, then it's an error!
                    throw new RequestException(403);
                }
                colony = (Colony) baseColony;
                break;
            }
        }
        if (colony == null) {
            throw new RequestException(404);
        }

        // simulate the star up to this point...
        sim.simulate(star);

        // adjust the colony's focus values based on what the post has
        au.com.codeka.common.protobuf.Colony colony_pb =
            getRequestBody(au.com.codeka.common.protobuf.Colony.class);

        float focusTotal = colony_pb.focus_construction +
                           colony_pb.focus_population +
                           colony_pb.focus_farming +
                           colony_pb.focus_mining;
        colony.setConstructionFocus(colony_pb.focus_construction / focusTotal);
        colony.setPopulationFocus(colony_pb.focus_population / focusTotal);
        colony.setFarmingFocus(colony_pb.focus_farming / focusTotal);
        colony.setMiningFocus(colony_pb.focus_mining / focusTotal);

        new StarController().update(star);
    }
}
