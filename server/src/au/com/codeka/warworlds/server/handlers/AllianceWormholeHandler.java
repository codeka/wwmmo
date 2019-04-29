package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.Star;

import static com.google.common.base.Preconditions.checkNotNull;

public class AllianceWormholeHandler extends RequestHandler {
  private static final Log log = new Log("AllianceWormholeHandler");

  @Override
  protected void get() throws RequestException {
    int allianceID = Integer.parseInt(getUrlParameter("allianceid"));

    // only admins and people in this alliance can view the list of wormholes
    if (!getSession().isAdmin() && getSession().getAllianceID() != allianceID) {
      log.warning(
          "Current session's alliance (%d) not the same as requested alliance (%d)",
          getSession().getAllianceID(),
          allianceID);
      throw new RequestException(403);
    }


    List<Star> stars;
    String s = getRequest().getParameter("startIndex");
    if (s != null) {
      int startIndex = Integer.parseInt(s);
      int count = Integer.parseInt(getRequest().getParameter("count"));
      int empireId = 0;
      String name = null;

      // empireId means only wormholes owned by the given empire
      s = getRequest().getParameter("empireId");
      if (s != null) {
        empireId = Integer.parseInt(s);
      }

      // name is a substring to search in the list for the name of the star
      s = getRequest().getParameter("name");
      if (s != null) {
        name = s.toLowerCase(Locale.ENGLISH);
      }

      stars = new StarController().getWormholesForAlliance(
          allianceID, empireId, name, startIndex, count);
    } else {
      // Old behavior, just return everything
      stars = new StarController().getWormholesForAlliance(allianceID);
      stars.sort((lhs, rhs) -> lhs.getName().compareTo(rhs.getName()));
    }

    Messages.Stars.Builder stars_pb = Messages.Stars.newBuilder();
    for (Star star : stars) {
      Messages.Star.Builder star_pb = Messages.Star.newBuilder();
      star.toProtocolBuffer(star_pb);
      stars_pb.addStars(star_pb);
    }
    setResponseBody(stars_pb.build());
  }
}
