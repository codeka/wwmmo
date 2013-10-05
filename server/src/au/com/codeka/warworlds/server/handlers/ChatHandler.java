package au.com.codeka.warworlds.server.handlers;

import java.sql.ResultSet;

import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.ChatController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.ChatMessage;

/**
 * Handles the /realms/.../chat URL.
 */
public class ChatHandler extends RequestHandler {

    @Override
    protected void get() throws RequestException {
        DateTime after = DateTime.now().minusDays(14);
        if (getRequest().getParameter("since") != null) { // note: synonym for 'after'
            long epoch = Long.parseLong(getRequest().getParameter("since")) + 1;
            after = new DateTime(epoch * 1000);
        }
        if (getRequest().getParameter("after") != null) {
            long epoch = Long.parseLong(getRequest().getParameter("after")) + 1;
            after = new DateTime(epoch * 1000);
        }

        DateTime before = DateTime.now().plusHours(1);
        if (getRequest().getParameter("before") != null) {
            long epoch = Long.parseLong(getRequest().getParameter("before")) - 1;
            before = new DateTime(epoch * 1000);
        }

        DateTime minDate = DateTime.now().minusDays(14);
        if (after.isBefore(minDate)) {
            after = minDate;
        }
        if (before.isBefore(minDate)) {
            before = minDate;
        }

        Integer conversationID = null;
        if (getRequest().getParameter("conversation") != null) {
            conversationID = Integer.parseInt(getRequest().getParameter("conversation"));
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
                      " AND posted_date <= ?" +
                      " AND (conversation_id IN (SELECT conversation_id FROM chat_conversation_participants WHERE empire_id = ?)" +
                       " OR (conversation_id IS NULL" +
                      (getSession().isAdmin()
                          ? "" // admin can see all alliance chat
                          : " AND (alliance_id IS NULL OR alliance_id = ?)") +
                       "))" +
                      (conversationID != null && conversationID > 0 ? " AND conversation_id = ?" : "") +
                      (conversationID != null && conversationID == 0 ? " AND alliance_id IS NULL" : "") +
                      (conversationID != null && conversationID < 0 ? " AND alliance_id IS NOT NULL" : "") +
                    " ORDER BY posted_date DESC" +
                    " LIMIT "+max;
        try (SqlStmt stmt = DB.prepare(sql)) {
            int i = 1;
            stmt.setDateTime(i++, after);
            stmt.setDateTime(i++, before);
            if (!getSession().isAdmin()) {
                stmt.setInt(i++, getSession().getEmpireID());
                stmt.setInt(i++, getSession().getAllianceID());
            } else {
                stmt.setInt(i++, 0); // TODO: admin won't see any private conversations...
            }
            if (conversationID != null && conversationID > 0) {
                stmt.setInt(i++, conversationID);
            }
            ResultSet rs = stmt.select();

            Messages.ChatMessages.Builder chat_msgs_pb = Messages.ChatMessages.newBuilder();
            while (rs.next()) {
                ChatMessage msg = new ChatMessage(rs);
                Messages.ChatMessage.Builder chat_msg_pb = Messages.ChatMessage.newBuilder();
                msg.toProtocolBuffer(chat_msg_pb, true);
                chat_msgs_pb.addMessages(chat_msg_pb);
            }

            setResponseBody(chat_msgs_pb.build());
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    @Override
    protected void post() throws RequestException {
        Messages.ChatMessage chat_msg_pb = getRequestBody(Messages.ChatMessage.class);

        if (chat_msg_pb.hasAllianceKey() && chat_msg_pb.getAllianceKey().length() > 0) {
            // confirm that if they've specified an alliance, that it's actually their
            // own alliance...
            int allianceID = Integer.parseInt(chat_msg_pb.getAllianceKey());
            if (allianceID != getSession().getAllianceID()) {
                throw new RequestException(400);
            }
        }

        // if it's not admin, add the right empire ID
        if (!getSession().isAdmin()) {
            chat_msg_pb = Messages.ChatMessage.newBuilder(chat_msg_pb)
                    .setEmpireKey(Integer.toString(getSession().getEmpireID()))
                    .build();
        }

        ChatMessage msg = new ChatMessage();
        msg.fromProtocolBuffer(chat_msg_pb);
        new ChatController().postMessage(msg);

        setResponseBody(chat_msg_pb);
    }
}
