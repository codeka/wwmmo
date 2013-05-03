package au.com.codeka.warworlds.server.handlers;

import java.util.List;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Star;

public class EmpiresTaxesHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        try (Transaction t = DB.beginTransaction()) {
            Empire empire = new EmpireController().getEmpire(Integer.parseInt(this.getUrlParameter("empire_id")));
            if (!getSession().isAdmin() && empire.getID() != getSession().getEmpireID()) {
                throw new RequestException(403);
            }

            int[] starIds = new EmpireController(t).getStarsForEmpire(empire.getID());
            List<Star> stars = new StarController(t).getStars(starIds);

            // simulate all of the stars
            Simulation sim = new Simulation();
            for (Star star : stars) {
                sim.simulate(star);
            }

            // gather up all of the cash
            float taxes = 0.0f;
            for (Star star : stars) {
                for (BaseColony baseColony : star.getColonies()) {
                    Colony colony = (Colony) baseColony;
                    if (colony.getEmpireID() == empire.getID()) {
                        taxes += colony.collectTaxes();
                    }
                }
            }

            Messages.CashAuditRecord.Builder audit_record_pb = Messages.CashAuditRecord.newBuilder();
            audit_record_pb.setEmpireId(empire.getID());
            audit_record_pb.setReason(Messages.CashAuditRecord.Reason.CollectFromColonies);
            new EmpireController(t).depositCash(empire.getID(), taxes, audit_record_pb);

            for (Star star : stars) {
                new StarController(t).update(star);
            }

            t.commit();
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

}
