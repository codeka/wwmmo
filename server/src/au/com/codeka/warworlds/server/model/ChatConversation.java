package au.com.codeka.warworlds.server.model;

import java.util.ArrayList;

import au.com.codeka.common.model.BaseChatConversation;
import au.com.codeka.common.model.BaseChatConversationParticipant;

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
            au.com.codeka.common.protobuf.ChatConversationParticipant pb) {
        ChatConversationParticipant participant = new ChatConversationParticipant();
        if (pb != null) {
            participant.fromProtocolBuffer(pb);
        }
        return participant;
    }
}
