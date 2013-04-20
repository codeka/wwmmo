package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.NewEmpireStarFinder;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.Star;

/**
 * Handles /realm/.../stars URL
 */
public class StarsHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        String findForEmpire = getRequest().getParameter("find_for_empire");
        if (findForEmpire != null && findForEmpire.equals("1")) {
            NewEmpireStarFinder starFinder = new NewEmpireStarFinder();
            if (!starFinder.findStarForNewEmpire()) {
                throw new RequestException(404);
            }

            Star star = new StarController().getStar(starFinder.getStarID());

            Messages.Star.Builder star_pb = Messages.Star.newBuilder();
            star.toProtocolBuffer(star_pb);
            setResponseBody(star_pb.build());
        }
    }
}
