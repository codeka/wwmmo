package au.com.codeka.warworlds.server.handlers;

import java.sql.ResultSet;

import org.expressme.openid.Base64;
import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.ctrl.NotificationController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.ChatMessage;

/**
 * Handles the /realms/.../chat URL.
 */
public class ChatHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        DateTime since = DateTime.now().minusDays(7);
        if (getRequest().getParameter("since") != null) {
            int epoch = Integer.parseInt(getRequest().getParameter("since"));
            since = new DateTime(epoch * 1000);
        }

        int max = 100;
        if (getRequest().getParameter("max") != null) {
            max = Integer.parseInt(getRequest().getParameter("max"));
        }
        if (max > 1000) {
            max = 1000;
        }

        String sql = "SELECT * FROM chat_messages" +
                    " WHERE posted_date > ?" +
                    " ORDER BY posted_date DESC" +
                    " LIMIT "+max;
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, since);
            ResultSet rs = stmt.select();

            Messages.ChatMessages.Builder chat_msgs_pb = Messages.ChatMessages.newBuilder();
            while (rs.next()) {
                ChatMessage msg = new ChatMessage(rs);
                Messages.ChatMessage.Builder chat_msg_pb = Messages.ChatMessage.newBuilder();
                msg.toProtocolBuffer(chat_msg_pb);
                chat_msgs_pb.addMessages(chat_msg_pb);
            }

            setResponseBody(chat_msgs_pb.build());
        } catch(Exception e) {
            throw new RequestException(500, e);
        }
    }

    @Override
    protected void post() throws RequestException {
        Messages.ChatMessage inp_chat_msg_pb = getRequestBody(Messages.ChatMessage.class);
        Messages.ChatMessage.Builder chat_msg_pb = Messages.ChatMessage.newBuilder();

        Session session = getSession();
        String sql = "INSERT INTO chat_messages (empire_id, alliance_id, message, posted_date)" +
                    " VALUES (?, ?, ?, ?)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            if (!session.isAdmin()) {
                chat_msg_pb.setEmpireKey(Integer.toString(session.getEmpireID()));
                // todo: alliance

                stmt.setInt(1, Integer.parseInt(chat_msg_pb.getEmpireKey()));
                stmt.setNull(2); // TODO: alliance
            } else {
                stmt.setNull(1);
                stmt.setNull(2);
            }
            chat_msg_pb.setMessage(inp_chat_msg_pb.getMessage()); // TODO: sanitize this
            stmt.setString(3, chat_msg_pb.getMessage());

            DateTime now = DateTime.now();
            chat_msg_pb.setDatePosted(now.getMillis() / 1000);
            stmt.setDateTime(4, now);
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(500, e);
        }

        new NotificationController().sendNotification(
                "chat", Base64.encodeBytes(chat_msg_pb.build().toByteArray()));
    }
}
