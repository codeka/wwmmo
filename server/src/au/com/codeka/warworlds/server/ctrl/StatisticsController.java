package au.com.codeka.warworlds.server.ctrl;

import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
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

    public void registerLogin(int empireID, Messages.HelloRequest hello_request_pb) throws RequestException {
        try {
            db.registerLogin(empireID, hello_request_pb.getDeviceModel(),
                             hello_request_pb.getDeviceManufacturer(),
                             hello_request_pb.getDeviceBuild(),
                             hello_request_pb.getDeviceVersion());
        } catch(Exception e) {
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

        public void registerLogin(int empireID, String deviceModel, String deviceManufacturer,
                                  String deviceBuild, String deviceVersion) throws Exception {
            String sql = "INSERT INTO empire_logins (empire_id, date, device_model, device_manufacturer," +
                                                   " device_build, device_version)" +
                        " VALUES (?, ?, ?, ?, ?, ?)";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, empireID);
                stmt.setDateTime(2, DateTime.now());
                stmt.setString(3, deviceModel);
                stmt.setString(4, deviceManufacturer);
                stmt.setString(5, deviceBuild);
                stmt.setString(6, deviceVersion);
                stmt.update();
            }
        }
    }
}
