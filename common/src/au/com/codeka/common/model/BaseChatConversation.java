package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.protobuf.Messages;

public class BaseChatConversation {
    protected int mID;
    protected List<Integer> mEmpireIDs;

    public int getID() {
        return mID;
    }
    public List<Integer> getEmpireIDs() {
        return mEmpireIDs;
    }

    public void fromProtocolBuffer(Messages.ChatConversation pb) {
        mID = pb.getId();
        mEmpireIDs = new ArrayList<Integer>();
        for (int i = 0; i < pb.getEmpireIdsCount(); i++) {
            mEmpireIDs.add(pb.getEmpireIds(i));
        }
    }

    public void toProtocolBuffer(Messages.ChatConversation.Builder pb) {
        pb.setId(mID);
        pb.addAllEmpireIds(mEmpireIDs);
    }
}
