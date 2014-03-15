package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.ChatMessage;
import au.com.codeka.warworlds.server.model.Empire;

public class ChatAbuseController {
    private final Logger log = LoggerFactory.getLogger(ChatAbuseController.class);
    private DataBase db;

    // number of unique empires who need to vote an empire in the sinbin before they get added
    private static int sSinBinUniqueEmpireVotes = 3;

    // number of seconds that sSinBinUniqueEmpireVotes needs to come in for us to count them
    private static int sSinBinVoteTimeSeconds = 4 * 60 * 60; // 4 hours

    static {
        String str = System.getProperty("au.com.codeka.warworlds.server.sinbinUniqueEmpireVotes");
        if (str != null) {
            sSinBinUniqueEmpireVotes = Integer.parseInt(str);
        }

        str = System.getProperty("au.com.codeka.warworlds.server.sinbinVoteTimeSeconds");
        if (str != null) {
            sSinBinVoteTimeSeconds = Integer.parseInt(str);
        }
    }

    public ChatAbuseController() {
        db = new DataBase();
    }
    public ChatAbuseController(Transaction trans) {
        db = new DataBase(trans);
    }

    public boolean isInPenaltyBox(int empireID) throws RequestException {
        try {
            return db.isInPenaltyBox(empireID);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public void reportAbuse(ChatMessage msg, Empire reportingEmpire) throws RequestException {
        // you can't report yourself for abuse
        if (msg.getEmpireID() == reportingEmpire.getID()) {
            return;
        }

        if (isInPenaltyBox(msg.getEmpireID())) {
            throw new RequestException(400, "Cannot report an empire that's already in penalty.");
        }
        if (isInPenaltyBox(reportingEmpire.getID())) {
            throw new RequestException(400, "Cannot report an empire while you are in penalty.");
        }

        try {
            db.reportAbuse(msg.getID(), msg.getEmpireID(), reportingEmpire.getID());

            int numVotes = db.getUniqueReports(msg.getEmpireID(), DateTime.now().minusSeconds(sSinBinVoteTimeSeconds));
            if (numVotes >= sSinBinUniqueEmpireVotes) {
                log.info(String.format("Empire #%d received %d abuse reports in the last %.2f hours, moving them to the sin bin.",
                        msg.getEmpireID(), numVotes, sSinBinVoteTimeSeconds / 3600.0f));

                int numPenalties = db.getNumPenalties(msg.getEmpireID());
                numPenalties ++;
                int numHours = (int) Math.pow(2, numPenalties);
                log.info(String.format("Empire has received %d past penalties, sinbinning for %d hours.", numPenalties,
                        numHours));

                db.addToPenaltyBox(msg.getEmpireID(), DateTime.now().plusHours(numHours));

                // send a broadcast message from the server letting people know what's happened.
                Empire empire = new EmpireController().getEmpire(msg.getEmpireID());
                ChatMessage notification = ChatMessage.createServerMessage(String.format(Locale.ENGLISH,
                        "%s has been put into the penalty box for %d hours. Review the in-game "+
                        "chat policy here: http://www.war-worlds.com/doc/chat/abuse", empire.getDisplayName(), numHours));
                new ChatController().postMessage(notification);
            } else {
                log.info(String.format("Empire #%d received %d abuse reports in the last %.2f hours, not yet moving to the sin bin.",
                        msg.getEmpireID(), numVotes, sSinBinVoteTimeSeconds / 3600.0f));
            }
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

        public void reportAbuse(int chatMsgID, int empireID, int reportingEmpireID) throws Exception {
            String sql = "INSERT INTO chat_abuse_reports (chat_msg_id, empire_id, reporting_empire_id, " +
                        " reported_date) VALUES (?, ?, ?, NOW())";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, chatMsgID);
                stmt.setInt(2, empireID);
                stmt.setInt(3, reportingEmpireID);
                stmt.update();
            }
        }

        /**
         * Gets the number of unique reports for the given empire since the given cutoff date.
         */
        public int getUniqueReports(int empireID, DateTime cutoff) throws Exception {
            String sql = "SELECT COUNT(DISTINCT reporting_empire_id) FROM chat_abuse_reports" +
                        " WHERE empire_id = ?" +
                          " AND reported_date > ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, empireID);
                stmt.setDateTime(2, cutoff);
                return (int) (long) stmt.selectFirstValue(Long.class);
            }
        }

        /**
         * Gets the number of penalties the given empire has received. If there's a gap of > 7 days, we
         * stop counting at that point.
         */
        public int getNumPenalties(int empireID) throws Exception {
            int numPenalties = 0;
            DateTime lastPenalty = DateTime.now();
            String sql = "SELECT expiry FROM chat_sinbin WHERE empire_id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, empireID);
                ResultSet rs = stmt.select();
                while (rs.next()) {
                    DateTime expiry = new DateTime(rs.getTimestamp(1).getTime());
                    if (Days.daysBetween(expiry, lastPenalty).getDays() > 7) {
                        break;
                    }

                    numPenalties ++;
                    lastPenalty = expiry;
                }
            }

            return numPenalties;
        }

        public boolean isInPenaltyBox(int empireID) throws Exception {
            String sql = "SELECT COUNT(*) FROM chat_sinbin WHERE empire_id = ? AND expiry > NOW()";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, empireID);
                return (stmt.selectFirstValue(Long.class) > 0);
            }
        }

        public void addToPenaltyBox(int empireID, DateTime expiry) throws Exception {
            String sql = "INSERT INTO chat_sinbin (empire_id, expiry) VALUES (?, ?)";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, empireID);
                stmt.setDateTime(2, expiry);
                stmt.update();
            }

        }
    }
}
