package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.EmpireRank;

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

    /**
     * Updates the rank statistics for all empires
     */
    public void updateRanks() throws Exception {
        TreeMap<Integer, EmpireRank> ranks = new TreeMap<Integer, EmpireRank>();

        String sql = "SELECT id AS empire_id FROM empires";
        try (SqlStmt stmt = db.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                EmpireRank rank = new EmpireRank(rs);
                ranks.put(rank.getEmpireID(), rank);
            }
        }

        sql = "SELECT empire_id, SUM(num_ships) FROM fleets WHERE empire_id IS NOT NULL GROUP BY empire_id";
        try (SqlStmt stmt = db.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int empireID = rs.getInt(1);
                int totalShips = rs.getInt(2);
                ranks.get(empireID).setTotalShips(totalShips);
            }
        }

        sql = "SELECT empire_id, SUM(num_ships) FROM fleets WHERE empire_id IS NOT NULL GROUP BY empire_id";
        try (SqlStmt stmt = db.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int empireID = rs.getInt(1);
                int totalShips = rs.getInt(2);
                ranks.get(empireID).setTotalShips(totalShips);
            }
        }

        sql = "SELECT empire_id, COUNT(*) FROM buildings GROUP BY empire_id";
        try (SqlStmt stmt = db.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int empireID = rs.getInt(1);
                int totalBuildings = rs.getInt(2);
                ranks.get(empireID).setTotalBuildings(totalBuildings);
            }
        }

        sql = "SELECT empire_id, COUNT(*) FROM colonies WHERE empire_id IS NOT NULL GROUP BY empire_id";
        try (SqlStmt stmt = db.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int empireID = rs.getInt(1);
                int totalColonies = rs.getInt(2);
                ranks.get(empireID).setTotalColonies(totalColonies);
            }
        }

        sql = "SELECT empire_id, COUNT(*) FROM (" +
               " SELECT empire_id, star_id" +
               " FROM stars" +
               " INNER JOIN colonies ON colonies.star_id = stars.id" +
               " WHERE colonies.empire_id IS NOT NULL" +
               " GROUP BY empire_id, star_id" +
              ") AS stars" +
             " GROUP BY empire_id";
        try (SqlStmt stmt = db.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int empireID = rs.getInt(1);
                int totalStars = rs.getInt(2);
                ranks.get(empireID).setTotalStars(totalStars);
            }
        }

        ArrayList<EmpireRank> sortedRanks = new ArrayList<EmpireRank>(ranks.values());
        Collections.sort(sortedRanks, new Comparator<EmpireRank>() {
            @Override
            public int compare(EmpireRank left, EmpireRank right) {
                int diff = right.getTotalColonies() - left.getTotalColonies();
                if (diff != 0)
                    return diff;

                diff = right.getTotalStars() - left.getTotalStars();
                if (diff != 0)
                    return diff;

                diff = right.getTotalShips() - left.getTotalShips();
                return diff;
            }
        });

        sql = "INSERT INTO empire_ranks (empire_id, rank, total_stars, total_colonies," +
                                       " total_buildings, total_ships)" +
             " VALUES (?, ?, ?, ?, ?, ?)" +
             " ON DUPLICATE KEY UPDATE" +
                 " rank = ?, total_stars = ?, total_colonies = ?, total_buildings = ?," +
                 " total_ships = ?";
        try (SqlStmt stmt = db.prepare(sql)) {
            int rankValue = 1;
            for (EmpireRank rank : sortedRanks) {
                stmt.setInt(1, rank.getEmpireID());
                stmt.setInt(2, rankValue);
                stmt.setInt(3, rank.getTotalStars());
                stmt.setInt(4, rank.getTotalColonies());
                stmt.setInt(5, rank.getTotalBuildings());
                stmt.setInt(6, rank.getTotalShips());
                stmt.setInt(7, rankValue);
                stmt.setInt(8, rank.getTotalStars());
                stmt.setInt(9, rank.getTotalColonies());
                stmt.setInt(10, rank.getTotalBuildings());
                stmt.setInt(11, rank.getTotalShips());
                stmt.update();

                rankValue ++;
            }
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
