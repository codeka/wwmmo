package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.model.BaseChatMessage.MessageAction;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.ChatController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.ChatConversation;
import au.com.codeka.warworlds.server.model.ChatMessage;

/**
 * Handles the /chat/conversations/<id>/participants/<empire_id> URL that lets you remove participants (i.e. yourself).
 */
public class ChatConversationParticipantHandler extends RequestHandler {
    @Override
    protected void delete() throws RequestException {
        int conversationID = Integer.parseInt(getUrlParameter("conversation_id"));
        int empireID = Integer.parseInt(getUrlParameter("empire_id"));

        if (empireID != getSession().getEmpireID()) {
            throw new RequestException(403); // you can only remove yourself...
        }

        try (Transaction t = DB.beginTransaction()) {
            ChatConversation conversation = new ChatController(t).getConversation(conversationID);
            if (conversation == null) {
                throw new RequestException(404);
            }

            new ChatController(t).removeParticipant(conversation, empireID);

            t.commit();
        } catch(Exception e) {
            throw new RequestException(e);
        }

        // send a notification to the participants (including the new one!) that a new empire has been added.
        ChatMessage msg = new ChatMessage(getSession().getEmpireID(),
                                          Integer.toString(empireID),
                                          MessageAction.ParticipantLeft,
                                          conversationID);
        new ChatController().postMessage(msg);
    }
}
