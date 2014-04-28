package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseAllianceMember;
import au.com.codeka.common.model.BaseAllianceRequest;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.Alliance;
import au.com.codeka.warworlds.server.model.AllianceMember;
import au.com.codeka.warworlds.server.model.AllianceRequest;
import au.com.codeka.warworlds.server.model.AllianceRequestVote;
import au.com.codeka.warworlds.server.model.Empire;

public class AllianceController {
    private DataBase db;

    public AllianceController() {
        db = new DataBase();
    }
    public AllianceController(Transaction trans) {
        db = new DataBase(trans);
    }

    public BaseDataBase getDB() {
        return db;
    }

    public List<Alliance> getAlliances() throws RequestException {
        try {
            return db.getAlliances();
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public Alliance getAlliance(int allianceID) throws RequestException {
        try {
            return db.getAlliance(allianceID);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public boolean isSameAlliance(int empireID1, int empireID2) throws RequestException {
        try {
            return db.isSameAlliance(empireID1, empireID2);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public List<AllianceRequest> getRequests(int allianceID, boolean includeWithdrawn, Integer cursor)
            throws RequestException {
        try {
            return db.getRequests(allianceID, includeWithdrawn, cursor);
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

    public int addRequest(AllianceRequest request) throws RequestException {
        try {
            int requestID = db.addRequest(request);
            request.setID(requestID);

            // there's an implicit vote when you create a request (some requests require zero
            // votes, which means it passes straight away)
            Alliance alliance = db.getAlliance(request.getAllianceID());
            AllianceRequestProcessor processor = AllianceRequestProcessor.get(alliance, request);
            processor.onVote(this);

            return requestID;
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public void vote(AllianceRequestVote vote) throws RequestException {
        try {
            Alliance alliance = db.getAlliance(vote.getAllianceID());
            // normalize the number of votes they get by their rank in the alliance
            for (BaseAllianceMember member : alliance.getMembers()) {
                if (Integer.parseInt(member.getEmpireKey()) == vote.getEmpireID()) {
                    int numVotes = member.getRank().getNumVotes();
                    if (vote.getVotes() < 0) {
                        numVotes *= -1;
                    }
                    vote.setVotes(numVotes);
                    break;
                }
            }

            db.vote(vote);

            // depending on the kind of request this is, check whether this is enough votes to
            // complete the voting or not
            AllianceRequest request = db.getRequest(vote.getAllianceRequestID());
            AllianceRequestProcessor processor = AllianceRequestProcessor.get(alliance, request);
            processor.onVote(this);
        } catch(Exception e) {
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

    public byte[] getAllianceShield(int allianceID, Integer shieldID) throws RequestException {
        String sql = "SELECT image FROM alliance_shields " +
                    " WHERE alliance_id = ? ";
        if (shieldID != null) {
            sql += " AND id = ?";
        }
        sql += " ORDER BY create_date DESC LIMIT 1";
        try (SqlStmt stmt = db.prepare(sql)) {
            stmt.setInt(1, allianceID);
            if (shieldID != null) {
                stmt.setInt(2, shieldID);
            }
            ResultSet rs = stmt.select();
            if (rs.next()) {
                return rs.getBytes(1);
            }
        } catch (Exception e) {
            throw new RequestException(e);
        }

        return null;
    }

    public void changeAllianceShield(int allianceID, byte[] pngImage) throws RequestException {
        String sql = "INSERT INTO alliance_shields (alliance_id, create_date, image) VALUES (?, NOW(), ?)";
        try (SqlStmt stmt = db.prepare(sql)) {
            stmt.setInt(1, allianceID);
            stmt.setBlob(2, pngImage);
            stmt.update();
        } catch (Exception e) {
            throw new RequestException(e);
        }

        sql = "UPDATE alliances SET image_updated_date = NOW() WHERE id = ?";
        try (SqlStmt stmt = db.prepare(sql)) {
            stmt.setInt(1, allianceID);
            stmt.update();
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

        public boolean isSameAlliance(int empireID1, int empireID2) throws Exception {
            String sql = "SELECT alliance_id FROM empires WHERE id IN (?, ?)";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, empireID1);
                stmt.setInt(2, empireID2);
                ResultSet rs = stmt.select();

                int allianceID = -1;
                while (rs.next()) {
                    if (allianceID < 0) {
                        allianceID = rs.getInt(1);
                    } else if (allianceID > 0) {
                        return rs.getInt(1) == allianceID;
                    }
                }
            }

            return false;
        }

        public List<Alliance> getAlliances() throws Exception {
            String sql = "SELECT alliances.*," +
                               " (SELECT COUNT(*) FROM empires WHERE empires.alliance_id = alliances.id) AS num_empires," +
                               " (SELECT COUNT(*) FROM alliance_requests WHERE alliance_id = alliances.id AND state = " + AllianceRequest.RequestState.PENDING.getNumber() + ") AS num_pending_requests" +
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

        public Alliance getAlliance(int allianceID) throws Exception {
            Alliance alliance = null;
            String sql = "SELECT *, 0 AS num_empires," +
                               " (SELECT COUNT(*) FROM alliance_requests WHERE alliance_id = alliances.id AND state = " + AllianceRequest.RequestState.PENDING.getNumber() + ") AS num_pending_requests" +
                        " FROM alliances WHERE id = ?";
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

            sql = "SELECT id, alliance_id, alliance_rank FROM empires WHERE alliance_id = ?";
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
            String sql = "INSERT INTO alliances (name, creator_empire_id, created_date," +
                              " bank_balance, image_updated_date) VALUES (?, ?, ?, 0, NOW())";
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

        public int addRequest(AllianceRequest request) throws Exception {
            // if you make another request while you've still got one pending, the new request
            // will overwrite the old one.
            String sql = "DELETE FROM alliance_requests" +
                        " WHERE request_empire_id = ?" +
                          " AND alliance_id = ?" +
                          " AND request_type = ?" +
                          " AND state = " + BaseAllianceRequest.RequestState.PENDING.getNumber();
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, request.getRequestEmpireID());
                stmt.setInt(2, request.getAllianceID());
                stmt.setInt(3, request.getRequestType().getNumber());
                stmt.update();
            }

            sql = "INSERT INTO alliance_requests (" +
                    "alliance_id, request_empire_id, request_date, request_type, message, state," +
                   " votes, target_empire_id, amount, png_image, new_name)" +
                 " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (SqlStmt stmt = prepare(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, request.getAllianceID());
                stmt.setInt(2, request.getRequestEmpireID());
                stmt.setDateTime(3, DateTime.now());
                stmt.setInt(4, request.getRequestType().getNumber());
                stmt.setString(5, request.getMessage());
                stmt.setInt(6, BaseAllianceRequest.RequestState.PENDING.getNumber());
                stmt.setInt(7, 0);
                if (request.getTargetEmpireID() != null) {
                    stmt.setInt(8, request.getTargetEmpireID());
                } else {
                    stmt.setNull(8);
                }
                if (request.getAmount() != null) {
                    stmt.setDouble(9, request.getAmount());
                } else {
                    stmt.setNull(9);
                }
                stmt.setBlob(10, request.getPngImage());
                stmt.setString(11, request.getNewName());
                stmt.update();

                return stmt.getAutoGeneratedID();
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

        public List<AllianceRequest> getRequests(int allianceID, boolean includeWithdrawn,
                Integer cursor) throws Exception {
            ArrayList<AllianceRequest> requests = new ArrayList<AllianceRequest>();
            HashSet<Integer> requestIDs = new HashSet<Integer>();

            String sql = "SELECT * FROM alliance_requests" +
                        " WHERE alliance_id = ?";
            if (!includeWithdrawn) {
                sql += " AND state != " + AllianceRequest.RequestState.WITHDRAWN.getNumber();
            }
            if (cursor != null) {
                sql += " AND id < ?";
            }
            sql += " ORDER BY request_date DESC LIMIT 50";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, allianceID); 
                if (cursor != null) {
                    stmt.setInt(2, cursor);
                }
                ResultSet rs = stmt.select();

                while (rs.next()) {
                    AllianceRequest request = new AllianceRequest(rs);
                    requests.add(request);

                    if (!requestIDs.contains(request.getID())) {
                        requestIDs.add(request.getID());
                    }
                }
            }

            if (!requestIDs.isEmpty()) {
                sql = "SELECT * FROM alliance_request_votes WHERE alliance_request_id IN ";
                sql += buildInClause(requestIDs);
                try (SqlStmt stmt = prepare(sql)) {
                    ResultSet rs = stmt.select();
                    while (rs.next()) {
                        AllianceRequestVote vote = new AllianceRequestVote(rs);
                        for (BaseAllianceRequest request : requests) {
                            if (request.getID() == vote.getAllianceRequestID()) {
                                request.getVotes().add(vote);
                            }
                        }
                    }
                }
            }

            return requests;
        }

        public AllianceRequest getRequest(int allianceRequestID) throws Exception {
            String sql = "SELECT * FROM alliance_requests WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, allianceRequestID); 
                ResultSet rs = stmt.select();

                if (rs.next()) {
                    return new AllianceRequest(rs);
                }
            }

            throw new RequestException(404, "No such alliance request found!");
        }

        public void vote(AllianceRequestVote vote) throws Exception {
            // if they're already voted for this request, then update the existing vote
            String sql = "SELECT id FROM alliance_request_votes " +
                         "WHERE alliance_request_id = ? AND empire_id = ?";
            Integer id = null;
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, vote.getAllianceRequestID());
                stmt.setInt(2, vote.getEmpireID());
                id = stmt.selectFirstValue(Integer.class);
            }

            if (id != null) {
                sql = "UPDATE alliance_request_votes SET votes = ? WHERE id = ?";
                try (SqlStmt stmt = prepare(sql)) {
                    stmt.setInt(1, vote.getVotes());
                    stmt.setInt(2, (int) id);
                    stmt.update();
                }
            } else {
                sql = "INSERT INTO alliance_request_votes (alliance_id, alliance_request_id," +
                         " empire_id, votes, date) VALUES (?, ?, ?, ?, NOW())";
                try (SqlStmt stmt = prepare(sql)) {
                    stmt.setInt(1, vote.getAllianceID());
                    stmt.setInt(2, vote.getAllianceRequestID());
                    stmt.setInt(3, vote.getEmpireID());
                    stmt.setInt(4, vote.getVotes());
                    stmt.update();
                }
            }

            // update the alliance_requests table so it has an accurate vote count for this request
            sql = "UPDATE alliance_requests SET votes = (" +
                        "SELECT SUM(votes) FROM alliance_request_votes WHERE alliance_request_id = alliance_requests.id" +
                    ") WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, vote.getAllianceRequestID());
                stmt.update();
            }
        }
    }
}
