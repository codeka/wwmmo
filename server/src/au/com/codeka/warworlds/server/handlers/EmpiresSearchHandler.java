package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.model.Empire;

public class EmpiresSearchHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        ArrayList<Empire> empires = new ArrayList<Empire>();
        EmpireController ctrl = new EmpireController();

        String str = getRequest().getParameter("email");
        if (str != null) {
            Empire empire = ctrl.getEmpireByEmail(str);
            if (empire != null) {
                empires.add(empire);
            }
        }

        str = getRequest().getParameter("name");
        if (str != null) {
            for (Empire empire : ctrl.getEmpiresByName(str, 25)) {
                empires.add(empire);
            }
        }

        str = getRequest().getParameter("ids");
        if (str != null) {
            String[] parts = str.split(",");
            int[] ids = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                ids[i] = Integer.parseInt(parts[i]);
            }

            for (Empire empire : ctrl.getEmpires(ids)) {
                empires.add(empire);
            }
        }

        str = getRequest().getParameter("minRank");
        if (str != null) {
            int minRank = Integer.parseInt(str);
            int maxRank = minRank + 5;
            if (minRank <= 3) {
                minRank = 1;
            }
            str = getRequest().getParameter("maxRank");
            if (str != null) {
                maxRank = Integer.parseInt(str);
            }
            if (maxRank <= minRank) {
                maxRank = minRank + 5;
            }

            for (Empire empire : ctrl.getEmpiresByRank(minRank, maxRank)) {
                empires.add(empire);
            }
            if (minRank > 3) {
                for (Empire empire : ctrl.getEmpiresByRank(1, 3)) {
                    empires.add(empire);
                }
            }
        }

        str = getRequest().getParameter("self");
        if (str != null && str.equals("1")) {
            empires.add(ctrl.getEmpire(getSession().getEmpireID()));
        }

        Messages.Empires.Builder pb = Messages.Empires.newBuilder();
        for (Empire empire : empires) {
            Messages.Empire.Builder empire_pb = Messages.Empire.newBuilder();
            empire.toProtocolBuffer(empire_pb);
            if (getSessionNoError() == null || empire.getID() != getSession().getEmpireID() && !getSession().isAdmin()) {
                // if it's not our empire....
                empire_pb.setCash(0);
            }
            pb.addEmpires(empire_pb);
        }
        setResponseBody(pb.build());
    }

}
