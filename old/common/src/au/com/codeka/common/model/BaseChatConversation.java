package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.protobuf.Messages;

public abstract class BaseChatConversation {
    protected int mID;
    protected List<BaseChatConversationParticipant> mParticipants;

    public int getID() {
        return mID;
    }
    public boolean isPrivateChat() {
        return (mID > 0);
    }
    public List<BaseChatConversationParticipant> getParticipants() {
        return mParticipants;
    }

    public void fromProtocolBuffer(Messages.ChatConversation pb) {
        mID = pb.getId();
        mParticipants = new ArrayList<BaseChatConversationParticipant>();
        for (Messages.ChatConversationParticipant participant_pb : pb.getParticipantsList()) {
            mParticipants.add(createChatConversationParticipant(participant_pb));
        }
    }

    public void toProtocolBuffer(Messages.ChatConversation.Builder pb) {
        pb.setId(mID);
        for (BaseChatConversationParticipant participant : mParticipants) {
            Messages.ChatConversationParticipant.Builder participant_pb = Messages.ChatConversationParticipant.newBuilder();
            participant.toProtocolBuffer(participant_pb);
            pb.addParticipants(participant_pb);
        }
    }

    protected abstract BaseChatConversationParticipant createChatConversationParticipant(Messages.ChatConversationParticipant pb);
}
