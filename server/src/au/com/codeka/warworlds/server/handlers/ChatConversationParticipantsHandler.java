package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.model.BaseChatMessage.MessageAction;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.ChatController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.ChatConversation;
import au.com.codeka.warworlds.server.model.ChatMessage;

/**
 * Handles the /chat/conversations/<id>/participants URL that lets you invite new participants to a chat.
 */
public class ChatConversationParticipantsHandler extends RequestHandler {

    @Override
    protected void post() throws RequestException {
        Messages.ChatConversationParticipant pb = getRequestBody(Messages.ChatConversationParticipant.class);
        int conversationID = Integer.parseInt(getUrlParameter("conversation_id"));

        try (Transaction t = DB.beginTransaction()) {
            ChatConversation conversation = new ChatController(t).getConversation(conversationID);
            if (conversation == null) {
                throw new RequestException(404);
            }

            boolean isThisEmpireInConversation = false;
            boolean isNewEmpireInConversation = false;
            for (int empireID : conversation.getEmpireIDs()) {
                if (empireID == getSession().getEmpireID()) {
                    isThisEmpireInConversation = true;
                }
                if (empireID == pb.getEmpireId()) {
                    isNewEmpireInConversation = true;
                }
            }

            // you must already be a participant of this conversation to add somebody
            if (!isThisEmpireInConversation) {
                throw new RequestException(404);
            }

            // the empire you want to add can't already be there either...
            if (isNewEmpireInConversation) {
                throw new RequestException(400, Messages.GenericError.ErrorCode.EmpireAlreadyInConversation,
                        "They're already part of this conversation.");
            }

            new ChatController(t).addParticipant(conversation, pb.getEmpireId());

            t.commit();
        } catch(Exception e) {
            throw new RequestException(e);
        }

        // send a notification to the participants (including the new one!) that a new empire has been added.
        ChatMessage msg = new ChatMessage(getSession().getEmpireID(),
                                          Integer.toString(pb.getEmpireId()),
                                          MessageAction.ParticipantAdded,
                                          conversationID);
        new ChatController().postMessage(msg);
    }

}
