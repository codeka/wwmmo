package au.com.codeka.warworlds.server.handlers.admin;

import java.sql.ResultSet;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;

public class AdminChatProfanityHandler extends AdminGenericHandler {
    private final Logger log = LoggerFactory.getLogger(AdminChatHandler.class);
    @Override
    protected void get() throws RequestException {
        if (!isAdmin()) {
            return;
        }
        TreeMap<String, Object> data = new TreeMap<String, Object>();

        String sql = "SELECT * FROM chat_profane_words";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                if (rs.getInt("profanity_level") == 1) {
                    data.put("mild", rs.getString("words"));
                } else {
                    data.put("strong", rs.getString("words"));
                }
            }
        } catch(Exception e) {
            log.error("Error fetching sinbin.", e);
            // TODO: handle errors
        }

        render("admin/chat/profanity.html", data);
    }

    @Override
    protected void post() throws RequestException {
        if (!isAdmin()) {
            return;
        }

        try (Transaction t = DB.beginTransaction()) {
            String sql = "DELETE FROM chat_profane_words";
            try (SqlStmt stmt = t.prepare(sql)) {
                stmt.update();
            }

            sql = "INSERT INTO chat_profane_words (profanity_level, words) VALUES (?, ?)";
            try (SqlStmt stmt = t.prepare(sql)) {
                stmt.setInt(1, 1);
                stmt.setString(2, getRequest().getParameter("mild-words"));
                stmt.update();

                stmt.setInt(1, 2);
                stmt.setString(2, getRequest().getParameter("strong-words"));
                stmt.update();
            }
        } catch (Exception e) {
            throw new RequestException(e);
        }

        redirect("/realms/" + getRealm() + "/admin/chat/profanity");
    }
}
