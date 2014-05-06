package au.com.codeka.warworlds.server.handlers.admin;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class AdminChatHandler extends AdminGenericHandler {
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
                    " WHERE expiry > ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, DateTime.now().minusHours(24));
            ResultSet rs = stmt.select();
            ArrayList<TreeMap<String, Object>> current = new ArrayList<TreeMap<String, Object>>();
            ArrayList<TreeMap<String, Object>> past = new ArrayList<TreeMap<String, Object>>();
            while (rs.next()) {
                TreeMap<String, Object> result = new TreeMap<String, Object>();
                result.put("empire_id", rs.getInt("empire_id"));
                result.put("expiry", rs.getTimestamp("expiry").getTime());
                result.put("empireName", rs.getString("name"));
                result.put("userEmail", rs.getString("user_email"));
                
                DateTime dt = new DateTime(rs.getTimestamp("expiry").getTime());
                if (dt.isBefore(DateTime.now())) {
                    past.add(result);
                } else {
                    current.add(result);
                }
            }
            data.put("pastPenalty", past);
            data.put("currPenalty", current);
        } catch(Exception e) {
            log.error("Error fetching sinbin.", e);
            // TODO: handle errors
        }

        render("admin/chat/messages.html", data);
    }

}
