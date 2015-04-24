package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.EmpireStar;
import au.com.codeka.common.protobuf.EmpireStars;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Star;

public class EmpiresStarsHandler extends RequestHandler {
  private final Log log = new Log("EmpiresStarsHandler");

  @Override
  protected void get() throws RequestException {
    Empire empire =
        new EmpireController().getEmpire(Integer.parseInt(this.getUrlParameter("empireid")));
    if (!getSession().isAdmin() && empire.getID() != getSession().getEmpireID()) {
      throw new RequestException(403);
    }

    EmpireController.EmpireStarsFilter filter = EmpireController.EmpireStarsFilter.Everything;
    if (getRequest().getParameter("filter") != null) {
      for (EmpireController.EmpireStarsFilter val : EmpireController.EmpireStarsFilter.values()) {
        if (getRequest().getParameter("filter").toLowerCase()
            .equals(val.toString().toLowerCase())) {
          filter = val;
          break;
        }
      }
    }

    String search = null;
    if (getRequest().getParameter("search") != null) {
      search = getRequest().getParameter("search");
    }

    ArrayList<Integer> starIds =
        new EmpireController().getStarsForEmpire(empire.getID(), filter, search);

    if (getRequest().getParameter("indices") != null) {
      processSublist(empire, starIds, getRequest().getParameter("indices"));
      return;
    }

    // if they're doing a query to see what index into the array their star appears, then just
    // return that as a single integer.
    if (getRequest().getParameter("indexof") != null) {
      int starID = Integer.parseInt(getRequest().getParameter("indexof"));
      for (int i = 0; i < starIds.size(); i++) {
        if (starIds.get(i) == starID) {
          setResponseText(Integer.toString(i));
          return;
        }
      }
      // we couldn't find it... standard return value is -1...
      setResponseText("-1");
      return;
    }

    List<Star> stars = new StarController().getStars(starIds);

    au.com.codeka.common.protobuf.Stars pb = new au.com.codeka.common.protobuf.Stars();
    pb.stars = new ArrayList<>();
    for (Star star : stars) {
      if (!isAdmin()) {
        // no need to filter by buildings, these are -- by definition -- our stars anyway
        new StarController().sanitizeStar(star, empire.getID(), null, null);
      }

      au.com.codeka.common.protobuf.Star star_pb = new au.com.codeka.common.protobuf.Star();
      star.toProtocolBuffer(star_pb);
      pb.stars.add(star_pb);
    }
    setResponseBody(pb);
  }

  /**
   * This will return a sublist of the stars based on the indices string. The format of the
   * indices string is basically "a-b,c-d,e-f" and we'll return indices a-b, b-c and so on.
   */
  private void processSublist(Empire empire, ArrayList<Integer> starIds, String indices)
      throws RequestException {
    TreeMap<Integer, Integer> starIdMap = mapStarIdsToIndices(starIds, indices);
    List<Star> stars = new StarController().getStars(starIdMap.keySet());

    EmpireStars pb = new EmpireStars();
    pb.total_stars = starIds.size();
    pb.stars = new ArrayList<>();
    for (Star star : stars) {
      if (!isAdmin()) {
        // no need to filter by buildings, these are -- by definition -- our stars anyway
        new StarController().sanitizeStar(star, empire.getID(), null, null);
      }

      au.com.codeka.common.protobuf.Star star_pb = new au.com.codeka.common.protobuf.Star();
      star.toProtocolBuffer(star_pb);

      EmpireStar empire_star_pb = new EmpireStar();
      empire_star_pb.star = star_pb;
      empire_star_pb.index = starIdMap.get(star.getID());
      pb.stars.add(empire_star_pb);
    }

    setResponseBody(pb);
  }

  /**
   * Gets just the starIds for the given indices from the given list of "all" starIds.
   */
  private TreeMap<Integer, Integer> mapStarIdsToIndices(ArrayList<Integer> starIds,
      String indices) {
    TreeMap<Integer, Integer> ids = new TreeMap<>();
    for (String s : indices.split(",")) {
      String[] parts = s.split("-");
      if (parts.length != 2) {
        log.error("Invalid indices string: %s", indices);
        continue;
      }
      int startIndex = Integer.parseInt(parts[0]);
      int endIndex = Integer.parseInt(parts[1]);

      for (int i = startIndex; i <= endIndex && i < starIds.size(); i++) {
        ids.put(starIds.get(i), i);
      }
    }
    return ids;
  }
}
