package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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
        Empire empire = new EmpireController().getEmpire(Integer.parseInt(
                this.getUrlParameter("empire_id")));
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

        ArrayList<Integer> starIds = new EmpireController().getStarsForEmpire(empire.getID(),
                filter, search);

        if (getRequest().getParameter("indices") != null) {
            processSublist(empire, starIds, getRequest().getParameter("indices"));
            return;
        }

        List<Star> stars = new StarController().getStars(starIds);

        Messages.Stars.Builder pb = Messages.Stars.newBuilder();
        for (Star star : stars) {
            if (!isAdmin()) {
                // no need to filter by buildings, these are -- by definition -- our stars anyway
                new StarController().sanitizeStar(star, empire.getID(), null, null);
            }

            Messages.Star.Builder star_pb = Messages.Star.newBuilder();
            star.toProtocolBuffer(star_pb);
            pb.addStars(star_pb);
        }
        setResponseBody(pb.build());
    }

    /**
     * This will return a sublist of the stars based on the indices string. The format of the
     * indices string is basically "a-b,c-d,e-f" and we'll return indices a-b, b-c and so on.
     */
    private void processSublist(Empire empire, ArrayList<Integer> starIds, String indices)
            throws RequestException {
        TreeMap<Integer, Integer> starIdMap = mapStarIdsToIndices(starIds, indices);
        List<Star> stars = new StarController().getStars(starIdMap.keySet());

        Messages.EmpireStars.Builder pb = Messages.EmpireStars.newBuilder();
        pb.setTotalStars(starIds.size());
        for (Star star : stars) {
            if (!isAdmin()) {
                // no need to filter by buildings, these are -- by definition -- our stars anyway
                new StarController().sanitizeStar(star, empire.getID(), null, null);
            }

            Messages.Star.Builder star_pb = Messages.Star.newBuilder();
            star.toProtocolBuffer(star_pb);

            Messages.EmpireStar.Builder empire_star_pb = Messages.EmpireStar.newBuilder();
            empire_star_pb.setStar(star_pb);
            empire_star_pb.setIndex(starIdMap.get(star.getID()));
            pb.addStars(empire_star_pb);
        }

        setResponseBody(pb.build());
    }

    /** Gets just the starIds for the given indices from the given list of "all" starIds. */
    private TreeMap<Integer, Integer> mapStarIdsToIndices(ArrayList<Integer> starIds,
            String indices) {
        TreeMap<Integer, Integer> ids = new TreeMap<Integer, Integer>();
        for (String s : indices.split(",")) {
            String[] parts = s.split("-");
            int startIndex = Integer.parseInt(parts[0]);
            int endIndex = Integer.parseInt(parts[1]);

            for (int i = startIndex; i <= endIndex && i < starIds.size(); i++) {
                ids.put(starIds.get(i), i);
            }
        }
        return ids;
    }
}
