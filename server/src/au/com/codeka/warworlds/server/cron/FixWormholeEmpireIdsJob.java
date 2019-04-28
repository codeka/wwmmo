package au.com.codeka.warworlds.server.cron;

import java.util.HashMap;
import java.util.Map;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Star;

/**
 * Goes through the stars and updates the wormhole_empire_id columns.
 */
public class FixWormholeEmpireIdsJob extends CronJob {
  @Override
  public void run(String s) throws Exception {
    Map<Integer, Integer> starEmpireIds = new HashMap<>();
    String sql = "SELECT id, extra FROM stars WHERE star_type = " + Star.Type.Wormhole.ordinal();
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        int id = res.getInt(1);
        byte[] extra = res.getBytes(2);
        if (extra != null) {
          Messages.Star.StarExtra star_extra_pb = Messages.Star.StarExtra.parseFrom(extra);
          if (star_extra_pb.hasWormholeEmpireId()) {
            starEmpireIds.put(id, star_extra_pb.getWormholeEmpireId());
          }
        }
      }
    }

    try (SqlStmt stmt = DB.prepare("UPDATE stars SET wormhole_empire_id = ? WHERE id = ?")) {
      for (Integer id : starEmpireIds.keySet()) {
        stmt.setInt(1, starEmpireIds.get(id));
        stmt.setInt(2, id);
        stmt.update();
      }
    }
  }
}
