package au.com.codeka.warworlds.server.handlers;

import java.util.List;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.Star;

public class AllianceWormholeHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int allianceID = Integer.parseInt(getUrlParameter("alliance_id"));

        // only admins and people in this alliance can view the list of wormholes
        if (!getSession().isAdmin() && getSession().getAllianceID() != allianceID) {
            throw new RequestException(403);
        }

        List<Star> stars = new StarController().getWormholesForAlliance(allianceID);

        Messages.Stars.Builder stars_pb = Messages.Stars.newBuilder();
        for (Star star : stars) {
            Messages.Star.Builder star_pb = Messages.Star.newBuilder();
            star.toProtocolBuffer(star_pb);
            stars_pb.addStars(star_pb);
        }
        setResponseBody(stars_pb.build());
    }

}
