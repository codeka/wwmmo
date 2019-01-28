package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.model.EmpireBattleRank;

public class EmpireBattleRankListHandler extends RequestHandler {
  private final Log log = new Log("EmpireBattleRankListHandler");

  @Override
  protected void get() throws RequestException {
    int offset = Integer.parseInt(getRequest().getParameter("offset"));
    int count = Integer.parseInt(getRequest().getParameter("count"));
    int numDays = Integer.parseInt(getRequest().getParameter("numDays"));

    if (numDays >= 28) {
      numDays = 28;
    } else if (numDays >= 14) {
      numDays = 14;
    } else {
      numDays = 7;
    }

    EmpireController ctrl = new EmpireController();
    ArrayList<EmpireBattleRank> ranks = ctrl.getEmpireBattleRanks(numDays, offset, count);

    Messages.EmpireBattleRanks.Builder pb = Messages.EmpireBattleRanks.newBuilder()
        .setNumDays(numDays);
    for (EmpireBattleRank battleRank : ranks) {
      Messages.EmpireBattleRank.Builder battleRankPb = Messages.EmpireBattleRank.newBuilder();
      battleRank.toProtocolBuffer(battleRankPb);
      pb.addRanks(battleRankPb);
    }
    setResponseBody(pb.build());
  }
}
