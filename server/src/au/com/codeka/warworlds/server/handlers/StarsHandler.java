package au.com.codeka.warworlds.server.handlers;

import java.io.IOException;
import java.util.ArrayList;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Stars;
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
  private static final Log log = new Log("StarsHandler");

  @Override
  protected void get() throws RequestException {
    String findForEmpire = getRequest().getParameter("find_for_empire");
    if (findForEmpire != null && findForEmpire.equals("1")) {
      NewEmpireStarFinder starFinder = new NewEmpireStarFinder();
      if (!starFinder.findStarForNewEmpire()) {
        throw new RequestException(404);
      }

      Star star = new StarController().getStar(starFinder.getStarID());

      au.com.codeka.common.protobuf.Star star_pb = new au.com.codeka.common.protobuf.Star();
      star.toProtocolBuffer(star_pb);
      setResponseBody(star_pb);
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

      Stars stars_pb = new Stars();
      stars_pb.stars = new ArrayList<>();
      au.com.codeka.common.protobuf.Star star_pb = new au.com.codeka.common.protobuf.Star();
      star.toProtocolBuffer(star_pb);
      stars_pb.stars.add(star_pb);
      setResponseBody(stars_pb);
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
        log.error("Error occurred exporting stars.", e);
      }
    }
  }
}
