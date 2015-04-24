package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.WormholeTuneRequest;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.AllianceController;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.ctrl.WormholeController;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Star;

public class WormholeTuneHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        int starID = Integer.parseInt(getUrlParameter("starid"));

        Star srcWormhole = new StarController().getStar(starID);
        if (srcWormhole.getStarType().getType() != Star.Type.Wormhole) {
            throw new RequestException(404);
        }

        WormholeTuneRequest pb = getRequestBody(WormholeTuneRequest.class);
        if (pb.src_star_id != starID) {
            throw new RequestException(404);
        }

        Star destWormhole = new StarController().getStar(pb.dest_star_id);
        if (destWormhole.getStarType().getType() != Star.Type.Wormhole) {
            throw new RequestException(400);
        }

        Star.WormholeExtra srcWormholeExtra = srcWormhole.getWormholeExtra();
        Star.WormholeExtra destWormholeExtra = destWormhole.getWormholeExtra();

        Empire empire = new EmpireController().getEmpire(getSession().getEmpireID());
        if (srcWormholeExtra.getEmpireID() != empire.getID()) {
            Empire ownerEmpire = new EmpireController().getEmpire(srcWormholeExtra.getEmpireID());
            if (!new AllianceController().isSameAlliance(ownerEmpire, empire)) {
                throw new RequestException(400, "You do not have control of this wormhole and cannot tune it.");
            }
        }

        if (destWormholeExtra.getEmpireID() != empire.getID()) {
            Empire ownerEmpire = new EmpireController().getEmpire(destWormholeExtra.getEmpireID());
            if (!new AllianceController().isSameAlliance(ownerEmpire, empire)) {
                throw new RequestException(400, "You do not have control of the destination wormhole and cannot tune it.");
            }
        }

        new WormholeController().tuneWormhole(srcWormhole, destWormhole);

        srcWormhole = new StarController().getStar(srcWormhole.getID());
        au.com.codeka.common.protobuf.Star star_pb = new au.com.codeka.common.protobuf.Star();
        srcWormhole.toProtocolBuffer(star_pb);
        setResponseBody(star_pb);
    }
}
