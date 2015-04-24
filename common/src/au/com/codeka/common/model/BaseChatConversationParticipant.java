package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.ChatConversationParticipant;

public class BaseChatConversationParticipant {
    protected int mEmpireID;
    protected boolean mIsMuted;

    public int getEmpireID() {
        return mEmpireID;
    }

    public boolean isMuted() {
        return mIsMuted;
    }

    public void fromProtocolBuffer(ChatConversationParticipant pb) {
        mEmpireID = pb.empire_id;
        mIsMuted = pb.is_muted;
    }

    public void toProtocolBuffer(ChatConversationParticipant pb) {
        pb.empire_id = mEmpireID;
        pb.is_muted = mIsMuted;
    }
}
