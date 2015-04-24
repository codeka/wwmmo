package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;

import au.com.codeka.common.protobuf.ChatConversations;
import au.com.codeka.common.protobuf.GenericError;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.ChatController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.ChatConversation;

public class ChatConversationsHandler extends RequestHandler {

    /** Gets the list of conversations the current empire is part of. */
    @Override
    protected void get() throws RequestException {
        int empireID = getSession().getEmpireID();

        ChatConversations.Builder conversations_pb = new ChatConversations.Builder();
        conversations_pb.conversations = new ArrayList<>();
        for (ChatConversation conversation : new ChatController().getConversationsForEmpire(empireID)) {
            au.com.codeka.common.protobuf.ChatConversation conversation_pb =
                    new au.com.codeka.common.protobuf.ChatConversation();
            conversation.toProtocolBuffer(conversation_pb);
            conversations_pb.conversations.add(conversation_pb);
        }
        setResponseBody(conversations_pb.build());
    }

    @Override
    protected void post() throws RequestException {
        au.com.codeka.common.protobuf.ChatConversation pb =
                getRequestBody(au.com.codeka.common.protobuf.ChatConversation.class);

        int empireID1 = getSession().getEmpireID();
        if (pb.participants.size() > 2 || pb.participants.size() < 1) {
            throw new RequestException(400, GenericError.ErrorCode.InvalidConversation, "Cannot start new conversation.");
        }
        int empireID2 = 0;
        if (pb.participants.size() > 1 && pb.participants.get(0).empire_id == empireID1) {
            empireID2 = pb.participants.get(1).empire_id;
        } else if (pb.participants.size() > 1 && pb.participants.get(1).empire_id != empireID1) {
            throw new RequestException(400, GenericError.ErrorCode.InvalidConversation, "Cannot start new conversation.");
        } else {
            empireID2 = pb.participants.get(0).empire_id;
        }

        try (Transaction t = DB.beginTransaction()) {

            ChatConversation conversation = new ChatController(t).createConversation(empireID1, empireID2);

            au.com.codeka.common.protobuf.ChatConversation conversation_pb =
                    new au.com.codeka.common.protobuf.ChatConversation();
            conversation.toProtocolBuffer(conversation_pb);
            setResponseBody(conversation_pb);

            t.commit();
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

}
