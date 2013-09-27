package au.com.codeka.warworlds.server.model;

import java.util.ArrayList;

import au.com.codeka.common.model.BaseChatConversation;
import au.com.codeka.common.model.BaseChatConversationParticipant;
import au.com.codeka.warworlds.server.model.ChatConversationParticipant;
import au.com.codeka.common.protobuf.Messages;

public class ChatConversation extends BaseChatConversation {
    public ChatConversation(int conversationID) {
        mID = conversationID;
        mParticipants = new ArrayList<BaseChatConversationParticipant>();
    }

    public void addParticipant(int empireID, boolean isMuted) {
        mParticipants.add(new ChatConversationParticipant(empireID, isMuted));
    }

    @Override
    protected BaseChatConversationParticipant createChatConversationParticipant(
            Messages.ChatConversationParticipant pb) {
        ChatConversationParticipant participant = new ChatConversationParticipant();
        if (pb != null) {
            participant.fromProtocolBuffer(pb);
        }
        return participant;
    }
}
