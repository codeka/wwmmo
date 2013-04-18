package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.Pair;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.SectorController;
import au.com.codeka.warworlds.server.model.Sector;

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
            coord.one = Long.parseLong(xy[0]);
            coord.two = Long.parseLong(xy[1]);
            coords.add(coord);
        }

        return coords;
    }

    @Override
    protected void get() throws RequestException {
        List<Pair<Long, Long>> coords = getCoords();
        boolean generate = true;
        if (getRequest().getParameter("gen") != null && getRequest().getParameter("gen").equals("0")) {
            generate = false;
        }

        SectorController ctrl = new SectorController();
        Messages.Sectors.Builder sectors_pb = Messages.Sectors.newBuilder();
        for (Sector sector : ctrl.getSectors(coords, generate)) {
            sector.toProtocolBuffer(sectors_pb.addSectorsBuilder());
        }
        setResponseBody(sectors_pb.build());
    }
}
