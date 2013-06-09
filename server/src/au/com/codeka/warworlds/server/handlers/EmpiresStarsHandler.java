package au.com.codeka.warworlds.server.handlers;

import java.util.List;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Star;

public class EmpiresStarsHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        Empire empire = new EmpireController().getEmpire(Integer.parseInt(this.getUrlParameter("empire_id")));
        if (!getSession().isAdmin() && empire.getID() != getSession().getEmpireID()) {
            throw new RequestException(403);
        }

        int[] starIds = new EmpireController().getStarsForEmpire(empire.getID());
        List<Star> stars = new StarController().getStars(starIds);

        Messages.Stars.Builder pb = Messages.Stars.newBuilder();
        for (Star star : stars) {
            StarHandler.sanitizeStar(star, empire.getID(), null, null); // no need to filter by buildings, these are -- by definition -- our stars anyway

            Messages.Star.Builder star_pb = Messages.Star.newBuilder();
            star.toProtocolBuffer(star_pb);
            pb.addStars(star_pb);
        }
        setResponseBody(pb.build());
    }
}
