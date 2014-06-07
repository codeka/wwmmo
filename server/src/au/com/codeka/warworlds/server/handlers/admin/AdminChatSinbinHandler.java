package au.com.codeka.warworlds.server.handlers.admin;

import java.util.ArrayList;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class AdminChatSinbinHandler extends AdminGenericHandler {
    private final Logger log = LoggerFactory.getLogger(AdminChatHandler.class);
    @Override
    protected void get() throws RequestException {
        if (!isAdmin()) {
            return;
        }
        TreeMap<String, Object> data = new TreeMap<String, Object>();

        String sql = "SELECT *" +
                    " FROM chat_sinbin" +
                    " INNER JOIN empires ON chat_sinbin.empire_id = empires.id" +
                    " ORDER BY expiry DESC" +
                    " LIMIT 50";
        try (SqlStmt stmt = DB.prepare(sql)) {
            SqlResult res = stmt.select();
            ArrayList<TreeMap<String, Object>> sinbin = new ArrayList<TreeMap<String, Object>>();
            while (res.next()) {
                TreeMap<String, Object> result = new TreeMap<String, Object>();
                result.put("empire_id", res.getInt("empire_id"));
                result.put("expiry", res.getDateTime("expiry"));
                result.put("empireName", res.getString("name"));
                result.put("userEmail", res.getString("user_email"));

                sinbin.add(result);
            }
            data.put("sinbin", sinbin);
        } catch(Exception e) {
            log.error("Error fetching sinbin.", e);
            // TODO: handle errors
        }

        sql = "SELECT chat_abuse_reports.empire_id, reporting_empire_id, reported_date," +
                    " message, message_en, profanity_level, posted_date, e1.name AS empire_name," +
                    " e1.user_email AS empire_user_email, e2.name AS reporting_empire_name," +
                    " e2.user_email AS reporting_empire_user_email" +
             " FROM chat_abuse_reports" +
             " INNER JOIN empires e1 ON e1.id = chat_abuse_reports.empire_id" +
             " INNER JOIN empires e2 ON e2.id = chat_abuse_reports.reporting_empire_id" +
             " INNER JOIN chat_messages on chat_messages.id = chat_abuse_reports.chat_msg_id" +
             " ORDER BY reported_date DESC" +
             " LIMIT 50";
        try (SqlStmt stmt = DB.prepare(sql)) {
            SqlResult res = stmt.select();
            ArrayList<TreeMap<String, Object>> reports = new ArrayList<TreeMap<String, Object>>();
            while (res.next()) {
                TreeMap<String, Object> report = new TreeMap<String, Object>();
                report.put("empire_id", res.getInt("empire_id"));
                report.put("reporting_empire_id", res.getInt("reporting_empire_id"));
                report.put("reported_date", res.getDateTime("reported_date"));
                report.put("message", res.getString("message"));
                report.put("message_en", res.getString("message_en"));
                report.put("profanity_level", res.getInt("profanity_level"));
                report.put("posted_date", res.getDateTime("posted_date"));
                report.put("empire_name", res.getString("empire_name"));
                report.put("empire_user_email", res.getString("empire_user_email"));
                report.put("reporting_empire_name", res.getString("reporting_empire_name"));
                report.put("reporting_empire_user_email", res.getString("reporting_empire_user_email"));

                reports.add(report);
            }
            data.put("reports", reports);
        } catch(Exception e) {
            log.error("Error fetching sinbin.", e);
            // TODO: handle errors
        }

        render("admin/chat/sinbin.html", data);
    }

}
