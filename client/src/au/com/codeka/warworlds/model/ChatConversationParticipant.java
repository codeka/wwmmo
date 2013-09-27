package au.com.codeka.warworlds.model;

import au.com.codeka.common.model.BaseChatConversationParticipant;

public class ChatConversationParticipant extends BaseChatConversationParticipant {
    public ChatConversationParticipant() {
    }
    public ChatConversationParticipant(int empireID) {
        mEmpireID = empireID;
    }
}
