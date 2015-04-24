package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.Star;

public class AllianceWormholeHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        int allianceID = Integer.parseInt(getUrlParameter("allianceid"));

        // only admins and people in this alliance can view the list of wormholes
        if (!getSession().isAdmin() && getSession().getAllianceID() != allianceID) {
            throw new RequestException(403);
        }

        List<Star> stars = new StarController().getWormholesForAlliance(allianceID);

        au.com.codeka.common.protobuf.Stars.Builder stars_pb =
                new au.com.codeka.common.protobuf.Stars.Builder();
        stars_pb.stars = new ArrayList<>();
        for (Star star : stars) {
            au.com.codeka.common.protobuf.Star star_pb =
                    new au.com.codeka.common.protobuf.Star();
            star.toProtocolBuffer(star_pb);
            stars_pb.stars.add(star_pb);
        }
        setResponseBody(stars_pb.build());
    }

}
