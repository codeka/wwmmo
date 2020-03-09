package au.com.codeka.warworlds.server.ctrl;

import org.joda.time.DateTime;

import javax.annotation.Nullable;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;

public class StatisticsController {
  private DataBase db;

  public StatisticsController() {
    db = new DataBase();
  }

  public StatisticsController(Transaction trans) {
    db = new DataBase(trans);
  }

  public void registerLogin(
      Session session,
      String userAgent,
      Messages.HelloRequest hello_request_pb) throws RequestException {
    String[] parts = userAgent.split("/");
    String version = parts.length == 2 ? parts[1] : userAgent;

    try {
      db.registerLogin(session.getEmpireID(),
          session.getClientId(),
          hello_request_pb.getDeviceModel(),
          hello_request_pb.getDeviceManufacturer(),
          hello_request_pb.getDeviceBuild(),
          hello_request_pb.getDeviceVersion(),
          hello_request_pb.getAccessibilitySettingsInfo(),
          version);
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  private static class DataBase extends BaseDataBase {
    public DataBase() {
      super();
    }

    public DataBase(Transaction trans) {
      super(trans);
    }

    public void registerLogin(
        int empireID, String clientId, String deviceModel, String deviceManufacturer,
        String deviceBuild, String deviceVersion,
        @Nullable Messages.AccessibilitySettingsInfo accessibilitySettingsInfo,
        String version) throws Exception {
      String sql = "INSERT INTO empire_logins (empire_id, date, device_model, " +
          "device_manufacturer, device_build, device_version, num_accessibility_services, " +
          "accessibility_service_infos, version, client_id)" +
          " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try (SqlStmt stmt = prepare(sql)) {
        stmt.setInt(1, empireID);
        stmt.setDateTime(2, DateTime.now());
        stmt.setString(3, deviceModel);
        stmt.setString(4, deviceManufacturer);
        stmt.setString(5, deviceBuild);
        stmt.setString(6, deviceVersion);
        if (accessibilitySettingsInfo == null) {
          stmt.setInt(7, 0);
          stmt.setNull(8);
        } else {
          stmt.setInt(7, accessibilitySettingsInfo.getServiceCount());
          stmt.setBytes(8, accessibilitySettingsInfo.toByteArray());
        }
        stmt.setString(9, version);
        stmt.setString(10, clientId);
        stmt.update();
      }
    }
  }
}
