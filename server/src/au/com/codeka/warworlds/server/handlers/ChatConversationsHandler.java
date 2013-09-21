package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
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

        Messages.ChatConversations.Builder conversations_pb = Messages.ChatConversations.newBuilder();
        for (ChatConversation conversation : new ChatController().getConversationsForEmpire(empireID)) {
            Messages.ChatConversation.Builder conversation_pb = Messages.ChatConversation.newBuilder();
            conversation.toProtocolBuffer(conversation_pb);
            conversations_pb.addConversations(conversation_pb);
        }
        setResponseBody(conversations_pb.build());
    }

    @Override
    protected void post() throws RequestException {
        Messages.ChatConversation pb = getRequestBody(Messages.ChatConversation.class);

        int empireID1 = getSession().getEmpireID();
        if (pb.getEmpireIdsCount() > 2 || pb.getEmpireIdsCount() < 1) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.InvalidConversation, "Cannot start new conversation.");
        }
        int empireID2 = 0;
        if (pb.getEmpireIdsCount() > 1 && pb.getEmpireIds(0) == empireID1) {
            empireID2 = pb.getEmpireIds(1);
        } else if (pb.getEmpireIdsCount() > 1 && pb.getEmpireIds(1) != empireID1) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.InvalidConversation, "Cannot start new conversation.");
        } else {
            empireID2 = pb.getEmpireIds(0);
        }

        try (Transaction t = DB.beginTransaction()) {

            ChatConversation conversation = new ChatController(t).createConversation(empireID1, empireID2);

            Messages.ChatConversation.Builder conversation_pb = Messages.ChatConversation.newBuilder();
            conversation.toProtocolBuffer(conversation_pb);
            setResponseBody(conversation_pb.build());

            t.commit();
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

}
