package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseAllianceJoinRequest;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.Alliance;
import au.com.codeka.warworlds.server.model.AllianceJoinRequest;
import au.com.codeka.warworlds.server.model.AllianceMember;
import au.com.codeka.warworlds.server.model.Empire;

public class AllianceController {
    private DataBase db;

    public AllianceController() {
        db = new DataBase();
    }
    public AllianceController(Transaction trans) {
        db = new DataBase(trans);
    }

    public List<Alliance> getAlliances() throws RequestException {
        try {
            return db.getAlliances();
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public Alliance getAlliance(int allianceID, boolean includeMembers) throws RequestException {
        try {
            return db.getAlliance(allianceID, includeMembers);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public int requestJoin(int allianceID, int empireID, String message) throws RequestException {
        // TODO: if you're in an alliance, you can't request to join another one

        try {
            return db.requestJoin(allianceID, empireID, message);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public void updateJoinRequest(AllianceJoinRequest joinRequest) throws RequestException {
        try {
            db.updateJoinRequest(joinRequest);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public List<AllianceJoinRequest> getJoinRequests(int allianceID) throws RequestException {
        try {
            return db.getJoinRequests(allianceID);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public void leaveAlliance(int empireID, int allianceID) throws RequestException {
        try {
            db.leaveAlliance(empireID, allianceID);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public void createAlliance(Alliance alliance, Empire ownerEmpire) throws RequestException {
        Messages.CashAuditRecord.Builder audit_record_pb = Messages.CashAuditRecord.newBuilder()
                .setEmpireId(ownerEmpire.getID())
                .setAllianceName(alliance.getName());
        if (!new EmpireController().withdrawCash(ownerEmpire.getID(), 250000, audit_record_pb)) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.InsufficientCash,
                                       "Insufficient cash to create a new alliance.");
        }

        try {
            db.createAlliance(alliance, ownerEmpire.getID());
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

        public List<Alliance> getAlliances() throws Exception {
            String sql = "SELECT alliances.*," +
                               " (SELECT COUNT(*) FROM empires WHERE empires.alliance_id = alliances.id) AS num_empires" +
                        " FROM alliances" +
                        " ORDER BY name DESC";
            try (SqlStmt stmt = prepare(sql)) {
                ResultSet rs = stmt.select();

                ArrayList<Alliance> alliances = new ArrayList<Alliance>();
                while (rs.next()) {
                    alliances.add(new Alliance(null, rs));
                }
                return alliances;
            }
        }

        public Alliance getAlliance(int allianceID, boolean includeMembers) throws Exception {
            Alliance alliance = null;
            String sql = "SELECT *, 0 AS num_empires FROM alliances WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, allianceID);
                ResultSet rs = stmt.select();

                if (rs.next()) {
                    alliance = new Alliance(null, rs);
                }
            }
            if (alliance == null) {
                throw new RequestException(404);
            }

            sql = "SELECT id, alliance_id FROM empires WHERE alliance_id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, allianceID);
                ResultSet rs = stmt.select();
                while (rs.next()) {
                    AllianceMember member = new AllianceMember(rs);
                    alliance.getMembers().add(member);
                }
            }

            return alliance;
        }

        public void createAlliance(Alliance alliance, int creatorEmpireID) throws Exception {
            String sql = "INSERT INTO alliances (name, creator_empire_id, created_date) VALUES (?, ?, ?)";
            try (SqlStmt stmt = prepare(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, alliance.getName());
                stmt.setInt(2, creatorEmpireID);
                stmt.setDateTime(3, DateTime.now());
                stmt.update();
                alliance.setID(stmt.getAutoGeneratedID());
            }

            sql = "UPDATE empires SET alliance_id = ? WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, alliance.getID());
                stmt.setInt(2, creatorEmpireID);
                stmt.update();
            }
        }

        public int requestJoin(int allianceID, int empireID, String message) throws Exception {
            // see if there's already a request to join this alliance
            Integer joinRequestID = null;
            String sql = "SELECT id FROM alliance_join_requests" +
                        " WHERE empire_id = ? AND alliance_id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, empireID);
                stmt.setInt(2, allianceID);
                joinRequestID = stmt.selectFirstValue(Integer.class);
            }

            if (joinRequestID == null) {
                sql = "INSERT INTO alliance_join_requests (empire_id, alliance_id, message, request_date, state) VALUES (?, ?, ?, ?, ?)";
            } else {
                sql = "UPDATE alliance_join_requests SET empire_id = ?, alliance_id = ?," +
                            " message = ?, request_date = ?, state = ?" +
                     " WHERE id = ?";
            }
            try (SqlStmt stmt = prepare(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, empireID);
                stmt.setInt(2, allianceID);
                stmt.setString(3, message);
                stmt.setDateTime(4, DateTime.now());
                stmt.setInt(5, BaseAllianceJoinRequest.RequestState.PENDING.getValue());
                if (joinRequestID != null) {
                    stmt.setInt(6, joinRequestID);
                }
                stmt.update();
                if (joinRequestID == null) {
                    return stmt.getAutoGeneratedID();
                } else {
                    return joinRequestID;
                }
            }
        }

        public void updateJoinRequest(AllianceJoinRequest joinRequest) throws Exception {
            String sql = "UPDATE alliance_join_requests SET state = ?" +
                        " WHERE empire_id = ? AND alliance_id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, joinRequest.getState().getValue());
                stmt.setInt(2, joinRequest.getEmpireID());
                stmt.setInt(3, joinRequest.getAllianceID());
                stmt.update();
            }

            // if we've set it to "ACCEPTED" then we'll want to make them an actual member
            if (joinRequest.getState() == BaseAllianceJoinRequest.RequestState.ACCEPTED) {
                sql = "UPDATE empires SET alliance_id = ? WHERE id = ?";
                try (SqlStmt stmt = prepare(sql)) {
                    stmt.setInt(1, joinRequest.getAllianceID());
                    stmt.setInt(2, joinRequest.getEmpireID());
                    stmt.update();
                }
            }
        }

        public void leaveAlliance(int empireID, int allianceID) throws Exception {
            String sql = "UPDATE empires SET alliance_id = NULL WHERE id = ? AND alliance_id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, empireID);
                stmt.setInt(2, allianceID);
                stmt.update();
            }
        }

        public List<AllianceJoinRequest> getJoinRequests(int allianceID) throws Exception {
            String sql = "SELECT * FROM alliance_join_requests WHERE alliance_id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, allianceID);
                ResultSet rs = stmt.select();

                ArrayList<AllianceJoinRequest> joinRequests = new ArrayList<AllianceJoinRequest>();
                while (rs.next()) {
                    joinRequests.add(new AllianceJoinRequest(rs));
                }
                return joinRequests;
            }
        }
    }
}
