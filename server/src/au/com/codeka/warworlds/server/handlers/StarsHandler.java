package au.com.codeka.warworlds.server.handlers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.NewEmpireStarFinder;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.ctrl.StarExportController;
import au.com.codeka.warworlds.server.model.Star;

/**
 * Handles /realm/.../stars URL
 */
public class StarsHandler extends RequestHandler {
    private static final Logger log = LoggerFactory.getLogger(StarsHandler.class);

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

        String query = getRequest().getParameter("q");
        if (query != null) {
            int starID;
            try {
                starID = Integer.parseInt(query);
            } catch (Exception e) {
                return;
            }

            Star star = new StarController().getStar(starID);

            Messages.Stars.Builder stars_pb = Messages.Stars.newBuilder();
            Messages.Star.Builder star_pb = Messages.Star.newBuilder();
            star.toProtocolBuffer(star_pb);
            stars_pb.addStars(star_pb);
            setResponseBody(stars_pb.build());
        }

        String export = getRequest().getParameter("export");
        if (export != null) {
            if (export.equals("csv")) {
                getResponse().setContentType("text/csv");
            } else {
                getResponse().setContentType("text/plain");
            }
            getResponse().setCharacterEncoding("utf-8");
            try {
                new StarExportController().export(getResponse().getOutputStream());
            } catch (IOException e) {
                log.error("Error occured exporting stars.", e);
            }
        }
    }
}
