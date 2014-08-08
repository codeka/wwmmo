package au.com.codeka.warworlds.server.handlers.admin;

import java.util.ArrayList;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
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
                    " WHERE expiry > NOW()";
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

        render("admin/chat/messages.html", data);
    }

}
