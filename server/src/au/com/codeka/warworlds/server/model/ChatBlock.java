package au.com.codeka.warworlds.server.model;

import org.joda.time.DateTime;

import java.sql.SQLException;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.SqlResult;

public class ChatBlock {
  private int empireID;
  private int blockedEmpireID;
  private DateTime blockTime;

  public ChatBlock() {
  }

  public ChatBlock(SqlResult result) throws SQLException {
    empireID = result.getInt("empire_id");
    blockedEmpireID = result.getInt("blocked_empire_id");
    blockTime = result.getDateTime("created_date");
  }

  public int getEmpireID() {
    return empireID;
  }

  public int getBlockedEmpireID() {
    return blockedEmpireID;
  }

  public DateTime getBlockTime() {
    return blockTime;
  }

  public void fromProtocolBuffer(Messages.ChatBlockRequest pb) {
    empireID = pb.getEmpireId();
    blockedEmpireID = pb.getBlockedEmpireId();
    blockTime = new DateTime(pb.getBlockTime());
  }

  public void toProtocolBuffer(Messages.ChatBlockRequest.Builder pb) {
    pb.setEmpireId(empireID);
    pb.setBlockedEmpireId(blockedEmpireID);
    pb.setBlockTime(blockTime.getMillis());
  }
}
