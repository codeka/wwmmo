package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.ChatConversation;

public class ChatController {
    private final Logger log = LoggerFactory.getLogger(ChatController.class);
    private DataBase db;

    public ChatController() {
        db = new DataBase();
    }
    public ChatController(Transaction trans) {
        db = new DataBase(trans);
    }

    /**
     * Search for an existing conversation between the two given empires. An existing conversation is one
     * where the last message was sent within the last week and only the given two empires are participants.
     */
    public ChatConversation findExistingConversation(int empireID1, int empireID2) throws RequestException {
        // make sure empireID1 is the larger, since that's what our SQL query assumes
        if (empireID1 < empireID2) {
            int tmp = empireID1;
            empireID1 = empireID2;
            empireID2 = tmp;
        }

        try {
            return db.findExistingConversation(empireID1, empireID2);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public ChatConversation createConversation(int empireID1, int empireID2) throws RequestException {
        ChatConversation conversation = findExistingConversation(empireID1, empireID2);
        if (conversation == null) {
            try {
                log.info(String.format("Creating new conversation between %1d and %2d", empireID1, empireID2));
                conversation = db.createConversation(empireID1, empireID2);
            } catch (Exception e) {
                throw new RequestException(e);
            }
        }
        return conversation;
    }

    private static class DataBase extends BaseDataBase {
        public DataBase() {
            super();
        }
        public DataBase(Transaction trans) {
            super(trans);
        }

        public ChatConversation findExistingConversation(int empireID1, int empireID2) throws Exception {
            Integer chatID = null;
            String sql = "SELECT chat_conversations.id, MAX(empire_id) AS empire_id_1, MIN(empire_id) AS empire_id_2, COUNT(*) AS num_empires" +
                    " FROM chat_conversations" +
                    " INNER JOIN chat_conversation_participants ON conversation_id = chat_conversations.id" +
                    " GROUP BY chat_conversations.id" +
                    " HAVING COUNT(*) = 2" +
                       " AND MAX(empire_id) = ?" +
                       " AND MIN(empire_id) = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, empireID1);
                stmt.setInt(2, empireID2);
                ResultSet rs = stmt.select();
                if (rs.next()) {
                    chatID = rs.getInt(1);
                }
            }

            if (chatID == null) {
                return null;
            }

            ArrayList<ChatConversation> conversations = getConversations("chat_conversations.id = "+chatID);
            return conversations.get(0);
        }

        public ArrayList<ChatConversation> getConversations(String whereClause) throws Exception {
            Map<Integer, ChatConversation> conversations = new HashMap<Integer, ChatConversation>();
            String sql = "SELECT chat_conversations.id, chat_conversation_participants.empire_id" +
                        " FROM chat_conversations" +
                        " INNER JOIN chat_conversation_participants ON conversation_id = chat_conversations.id" +
                        "WHERE " + whereClause;
            try (SqlStmt stmt = prepare(sql)) {
                ResultSet rs = stmt.select();
                while (rs.next()) {
                    int conversationID = rs.getInt(1);
                    ChatConversation conversation = conversations.get(conversationID);
                    if (conversation == null) {
                        conversation = new ChatConversation(conversationID);
                        conversations.put(conversationID, conversation);
                    }
                    conversation.addEmpire(rs.getInt(2));
                }
            }
            return new ArrayList<ChatConversation>(conversations.values());
        }

        public ChatConversation createConversation(int empireID1, int empireID2) throws Exception {
            ChatConversation conversation;

            String sql = "INSERT INTO chat_conversations () VALUES ()";
            try (SqlStmt stmt = prepare(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.update();
                conversation = new ChatConversation(stmt.getAutoGeneratedID());
            }

            sql = "INSERT INTO chat_conversation_participants (conversation_id, empire_id, is_muted) VALUES" +
                 " (?, ?, 0), (?, ?, 0)";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, conversation.getID());
                stmt.setInt(2, empireID1);
                stmt.setInt(3, conversation.getID());
                stmt.setInt(4, empireID2);
                stmt.update();
            }

            conversation.addEmpire(empireID1);
            conversation.addEmpire(empireID2);
            return conversation;
        }
    }
}
