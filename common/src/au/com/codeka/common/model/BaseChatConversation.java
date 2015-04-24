package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.protobuf.ChatConversation;
import au.com.codeka.common.protobuf.ChatConversationParticipant;

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

    public void fromProtocolBuffer(ChatConversation pb) {
        mID = pb.id;
        mParticipants = new ArrayList<>();
        for (ChatConversationParticipant participant_pb : pb.participants) {
            mParticipants.add(createChatConversationParticipant(participant_pb));
        }
    }

    public void toProtocolBuffer(ChatConversation pb) {
        pb.id = mID;
        pb.participants = new ArrayList<>();
        for (BaseChatConversationParticipant participant : mParticipants) {
            ChatConversationParticipant participant_pb = new ChatConversationParticipant();
            participant.toProtocolBuffer(participant_pb);
            pb.participants.add(participant_pb);
        }
    }

    protected abstract BaseChatConversationParticipant createChatConversationParticipant(ChatConversationParticipant pb);
}
