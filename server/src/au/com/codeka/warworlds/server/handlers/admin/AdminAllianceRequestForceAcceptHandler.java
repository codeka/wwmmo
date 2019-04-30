package au.com.codeka.warworlds.server.handlers.admin;

import au.com.codeka.common.model.BaseAllianceMember;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.AllianceController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.Alliance;
import au.com.codeka.warworlds.server.model.AllianceRequestVote;

public class AdminAllianceRequestForceAcceptHandler extends AdminHandler {
  @Override
  protected void post() throws RequestException {
    int allianceId = Integer.parseInt(getUrlParameter("allianceid"));
    int requestId = Integer.parseInt(getUrlParameter("requestid"));

    int empireId = 0;
    Alliance alliance = new AllianceController().getAlliance(allianceId);
    for (BaseAllianceMember member : alliance.getMembers()) {
      if (member.getRank() == BaseAllianceMember.Rank.CAPTAIN) {
        empireId = member.getEmpireID();
      }
    }

    AllianceRequestVote requestVote = new AllianceRequestVote(
        allianceId, requestId, empireId);

    try (Transaction t = DB.beginTransaction()) {
      new AllianceController().vote(requestVote);
      t.commit();
    } catch(Exception e) {
      throw new RequestException(e);
    }
  }
}
