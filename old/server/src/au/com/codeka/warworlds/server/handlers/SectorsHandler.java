package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.Pair;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.BuildingController;
import au.com.codeka.warworlds.server.ctrl.SectorController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.model.BuildingPosition;
import au.com.codeka.warworlds.server.model.Sector;
import au.com.codeka.warworlds.server.model.Star;

/**
 * Handles /realm/.../sectors URL
 */
public class SectorsHandler extends RequestHandler {

    private List<Pair<Long, Long>> getCoords() throws RequestException {
        String str = getRequest().getParameter("coords");
        if (str == null) {
            throw new RequestException(404);
        }

        List<Pair<Long, Long>> coords = new ArrayList<Pair<Long, Long>>();
        for (String xystr : str.split("\\|")) {
            String[] xy = xystr.split(",");

            Pair<Long, Long> coord = new Pair<Long, Long>();
            try {
                if (xy.length == 2) {
                    coord.one = Long.parseLong(xy[0]);
                    coord.two = Long.parseLong(xy[1]);
                    coords.add(coord);
                }
            } catch (NumberFormatException e) {
                // just ignore
            }
        }

        return coords;
    }

    @Override
    protected void get() throws RequestException {
        int myEmpireID = getSession().getEmpireID();

        List<Pair<Long, Long>> coords = getCoords();
        if (coords == null || coords.size() == 0) {
            throw new RequestException(404);
        }
        boolean generate = true;
        if (getRequest().getParameter("gen") != null && getRequest().getParameter("gen").equals("0")) {
            generate = false;
        }

        long minSectorX, minSectorY, maxSectorX, maxSectorY;
        minSectorX = maxSectorX = coords.get(0).one;
        minSectorY = maxSectorY = coords.get(0).two;
        for (Pair<Long, Long> xy : coords) {
            if (xy.one < minSectorX) {
                minSectorX = xy.one;
            }
            if (xy.one > maxSectorX) {
                maxSectorX = xy.one;
            }
            if (xy.two < minSectorY) {
                minSectorY = xy.two;
            }
            if (xy.two > maxSectorY) {
                maxSectorY = xy.two;
            }
        }

        ArrayList<BuildingPosition> buildings = new BuildingController().getBuildings(
                myEmpireID, minSectorX, minSectorY, maxSectorX, maxSectorY);

        SectorController ctrl = new SectorController();
        List<Sector> sectors = ctrl.getSectors(coords, generate);
        ArrayList<Star> allStars = new ArrayList<Star>();
        for (Sector sector : sectors) {
            for (BaseStar baseStar : sector.getStars()) {
                allStars.add((Star) baseStar);
            }
        }

        Messages.Sectors.Builder sectors_pb = Messages.Sectors.newBuilder();
        for (Sector sector : sectors) {
            if (!isAdmin()) {
                for (BaseStar baseStar : sector.getStars()) {
                    Star star = (Star) baseStar;
                    new StarController().sanitizeStar(star, myEmpireID, buildings, allStars);
                }
            }

            sector.toProtocolBuffer(sectors_pb.addSectorsBuilder());
        }
        setResponseBody(sectors_pb.build());
    }
}
