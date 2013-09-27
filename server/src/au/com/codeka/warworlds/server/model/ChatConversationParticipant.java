package au.com.codeka.warworlds.server.model;

import au.com.codeka.common.model.BaseChatConversationParticipant;

public class ChatConversationParticipant extends BaseChatConversationParticipant {
    public ChatConversationParticipant() {
    }

    public ChatConversationParticipant(int empireID, boolean isMuted) {
        mEmpireID = empireID;
        mIsMuted = isMuted;
    }
}
