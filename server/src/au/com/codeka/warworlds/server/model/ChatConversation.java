package au.com.codeka.warworlds.server.model;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.model.BaseChatConversation;

public class ChatConversation extends BaseChatConversation {
    public ChatConversation(int conversationID) {
        mID = conversationID;
        mEmpireIDs = new ArrayList<Integer>();
    }

    public void addEmpire(int empireID) {
        mEmpireIDs.add(empireID);
    }
}
