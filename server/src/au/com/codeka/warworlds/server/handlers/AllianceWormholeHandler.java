package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.Star;

import static com.google.common.base.Preconditions.checkNotNull;

public class AllianceWormholeHandler extends RequestHandler {
  @Override
  protected void get() throws RequestException {
    int allianceID = Integer.parseInt(getUrlParameter("allianceid"));

    // only admins and people in this alliance can view the list of wormholes
    if (!getSession().isAdmin() && getSession().getAllianceID() != allianceID) {
      throw new RequestException(403);
    }

    List<Star> allStars = new StarController().getWormholesForAlliance(allianceID);
    allStars.sort((lhs, rhs) -> lhs.getName().compareTo(rhs.getName()));

    List<Star> stars;
    String s = getRequest().getParameter("startIndex");
    if (s != null) {
      int startIndex = Integer.parseInt(s);
      int count = Integer.parseInt(getRequest().getParameter("count"));

      // empireId means only wormholes owned by the given empire
      s = getRequest().getParameter("empireId");
      if (s != null) {
        int empireId = Integer.parseInt(s);
        allStars.removeIf(star -> {
          BaseStar.WormholeExtra wormhole = checkNotNull(star.getWormholeExtra());
          return (wormhole.getEmpireID() != empireId);
        });
      }

      // name is a substring to search in the list for the name of the star
      s = getRequest().getParameter("name");
      if (s != null) {
        String name = s;
        allStars.removeIf(star -> !star.getName().contains(name));
      }

      stars = new ArrayList<>();
      for (int i = startIndex; i < (startIndex + count); i++) {
        if (i >= allStars.size()) {
          break;
        }
        stars.add(allStars.get(i));
      }
    } else {
      // Old behavior, just return everything
      stars = allStars;
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
