package au.com.codeka.common.model;

import au.com.codeka.common.protobuf.Messages;

public class BaseChatConversationParticipant {
    protected int mEmpireID;
    protected boolean mIsMuted;

    public int getEmpireID() {
        return mEmpireID;
    }

    public boolean isMuted() {
        return mIsMuted;
    }

    public void fromProtocolBuffer(Messages.ChatConversationParticipant pb) {
        mEmpireID = pb.getEmpireId();
        mIsMuted = pb.getIsMuted();
    }

    public void toProtocolBuffer(Messages.ChatConversationParticipant.Builder pb) {
        pb.setEmpireId(mEmpireID);
        pb.setIsMuted(mIsMuted);
    }
}
