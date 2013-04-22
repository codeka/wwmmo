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
            // todo: fetch by rank
/*
                if self.request.get("minRank", "") != "":
                  minRank = int(self.request.get("minRank"))
                  maxRank = minRank + 5
                  if minRank <= 3:
                    minRank = 1 # we'll always return the first 3 anyway
                  if self.request.get("maxRank", "") != "":
                    maxRank = int(self.request.get("maxRank"))
                  empire_pbs = empire.getEmpiresByRank(minRank, maxRank)
                  empires_pb.empires.extend(empire_pbs)
                  if minRank > 3:
                    empires_pb.empires.extend(empire.getEmpiresByRank(1, 3))
 */
        }

        str = getRequest().getParameter("self");
        if (str != null && str.equals("1")) {
            empires.add(ctrl.getEmpire(getSession().getEmpireID()));
        }

        Messages.Empires.Builder pb = Messages.Empires.newBuilder();
        for (Empire empire : empires) {
            Messages.Empire.Builder empire_pb = Messages.Empire.newBuilder();
            empire.toProtocolBuffer(empire_pb);
            pb.addEmpires(empire_pb);
        }
        setResponseBody(pb.build());
        /*
        empires_pb = pb.Empires()

                if len(empires_pb.empires) == 0:
                  self.response.set_status(404)
                  return

                if len(empires_pb.empires) > 25:
                  del empires_pb.empires[25:]

                empire_keys = []
                for empire_pb in empires_pb.empires:
                  if empire_pb.email != self.user.email():
                    # it's not OUR empire, so block out a few details...
                    empire_pb.cash = 0;
                  empire_keys.append(empire_pb.key)

                self_empire_pb = empire.getEmpireForUser(self.user)
                for empire_rank in empire.getEmpireRanks(empire_keys):
                  for empire_pb in empires_pb.empires:
                    if empire_pb.key == empire_rank.empire_key:
                      # is there a better way??
                      empire_pb.rank.CopyFrom(empire_rank)
                      if not self._isAdmin() and empire_pb.rank.total_stars < 10 and empire_pb.key != self_empire_pb.key:
                        # if an enemy has < 10 stars under their control, we'll hide the number
                        # of ships they have, since it's probably giving away a little too much info
                        empire_pb.rank.total_ships = 0
                        empire_pb.rank.total_buildings = 0
                      break

                return empires_pb
*/
    }

}
